package fi.aalto.cs.apluscourses.intellij.services;

import static fi.aalto.cs.apluscourses.intellij.services.PluginSettings.LocalIdeSettingsNames.A_PLUS_DEFAULT_GROUP;
import static fi.aalto.cs.apluscourses.intellij.services.PluginSettings.LocalIdeSettingsNames.A_PLUS_IMPORTED_IDE_SETTINGS;
import static fi.aalto.cs.apluscourses.intellij.services.PluginSettings.LocalIdeSettingsNames.A_PLUS_IS_ASSISTANT_MODE;
import static fi.aalto.cs.apluscourses.intellij.services.PluginSettings.LocalIdeSettingsNames.A_PLUS_SHOW_REPL_CONFIGURATION_DIALOG;
import static fi.aalto.cs.apluscourses.utils.PluginResourceBundle.getText;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import fi.aalto.cs.apluscourses.dal.APlusTokenAuthentication;
import fi.aalto.cs.apluscourses.dal.TokenAuthentication;
import fi.aalto.cs.apluscourses.intellij.dal.IntelliJPasswordStorage;
import fi.aalto.cs.apluscourses.intellij.model.CourseProject;
import fi.aalto.cs.apluscourses.intellij.notifications.DefaultNotifier;
import fi.aalto.cs.apluscourses.intellij.utils.CourseFileManager;
import fi.aalto.cs.apluscourses.intellij.utils.IntelliJFilterOption;
import fi.aalto.cs.apluscourses.intellij.utils.ProjectKey;
import fi.aalto.cs.apluscourses.model.ExercisesTree;
import fi.aalto.cs.apluscourses.presentation.CourseEndedBannerViewModel;
import fi.aalto.cs.apluscourses.presentation.CourseViewModel;
import fi.aalto.cs.apluscourses.presentation.MainViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExerciseFilter;
import fi.aalto.cs.apluscourses.presentation.exercise.ExerciseGroupFilter;
import fi.aalto.cs.apluscourses.presentation.exercise.ExercisesTreeViewModel;
import fi.aalto.cs.apluscourses.presentation.filter.Option;
import fi.aalto.cs.apluscourses.presentation.filter.Options;
import fi.aalto.cs.apluscourses.utils.observable.ObservableProperty;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PluginSettings implements MainViewModelProvider, DefaultGroupIdSetting {

  PluginSettings(@NotNull PropertiesManager propertiesManager) {
    applicationPropertiesManager = propertiesManager;
    exerciseFilterOptions = new Options(
      new IntelliJFilterOption(applicationPropertiesManager,
          LocalIdeSettingsNames.A_PLUS_SHOW_NON_SUBMITTABLE,
          getText("presentation.exerciseFilterOptions.nonSubmittable"),
          null,
          new ExerciseFilter.NonSubmittableFilter()),
      new IntelliJFilterOption(applicationPropertiesManager,
          LocalIdeSettingsNames.A_PLUS_SHOW_COMPLETED,
          getText("presentation.exerciseFilterOptions.Completed"),
          null,
          new ExerciseFilter.CompletedFilter()),
      new IntelliJFilterOption(applicationPropertiesManager,
          LocalIdeSettingsNames.A_PLUS_SHOW_OPTIONAL,
          getText("presentation.exerciseFilterOptions.Optional"),
          null,
          new ExerciseFilter.OptionalFilter()),
      new IntelliJFilterOption(applicationPropertiesManager,
          LocalIdeSettingsNames.A_PLUS_SHOW_CLOSED,
          getText("presentation.exerciseGroupFilterOptions.Closed"),
          null,
          new ExerciseGroupFilter.ClosedFilter()));
  }

  public enum LocalIdeSettingsNames {
    A_PLUS_SHOW_REPL_CONFIGURATION_DIALOG("A+.showReplConfigDialog"),
    A_PLUS_IMPORTED_IDE_SETTINGS("A+.importedIdeSettings"),
    A_PLUS_DEFAULT_GROUP("A+.defaultGroup"),
    A_PLUS_SHOW_NON_SUBMITTABLE("A+.showNonSubmittable"),
    A_PLUS_SHOW_COMPLETED("A+.showCompleted"),
    A_PLUS_SHOW_OPTIONAL("A+.showOptional"),
    A_PLUS_SHOW_CLOSED("A+.showClosed"),
    A_PLUS_IS_ASSISTANT_MODE("A+.assistantMode");

    private final String name;

    LocalIdeSettingsNames(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  public static final String MODULE_REPL_INITIAL_COMMANDS_FILE_NAME
      = ".repl-commands";

  public static final String A_PLUS = "A+";

  //  15 minutes in milliseconds
  public static final long UPDATE_INTERVAL = 15L * 60 * 1000;
  //  15 seconds in milliseconds
  public static final long REASONABLE_DELAY_FOR_MODULE_INSTALLATION = 15L * 1000;

  private final @NotNull PropertiesManager applicationPropertiesManager;

  @NotNull
  private final ConcurrentMap<ProjectKey, MainViewModel> mainViewModels = new ConcurrentHashMap<>();

  @NotNull
  private final ConcurrentMap<ProjectKey, CourseProject> courseProjects = new ConcurrentHashMap<>();

  @NotNull
  private final ConcurrentMap<ProjectKey, CourseFileManager> courseFileManagers
      = new ConcurrentHashMap<>();

  @NotNull
  private final Options exerciseFilterOptions;

  private final ProjectManagerListener projectManagerListener = new ProjectManagerListener() {
    @Override
    public void projectClosed(@NotNull Project project) {
      ProjectKey key = new ProjectKey(project);
      courseFileManagers.remove(key);
      var courseProject = courseProjects.remove(key);
      if (courseProject != null) {
        courseProject.dispose();
      }
      MainViewModel mainViewModel = mainViewModels.remove(key);
      if (mainViewModel != null) {
        mainViewModel.dispose();
      }
      ProjectManager.getInstance().removeProjectManagerListener(project, this);
    }
  };

  /**
   * Methods to get the Singleton instance of {@link PluginSettings}.
   *
   * @return an instance of {@link PluginSettings}.
   */
  @NotNull
  public static PluginSettings getInstance() {
    return InstanceHolder.INSTANCE;
  }

  /**
   * Returns a MainViewModel for a specific project.
   *
   * @param project A project, or null (equivalent for default project).
   * @return A main view model.
   */
  @NotNull
  public MainViewModel getMainViewModel(@Nullable Project project) {
    // ProjectKey takes care or project being null and avoids creating differing keys for null.
    ProjectKey key = new ProjectKey(project);
    return mainViewModels.computeIfAbsent(key, projectKey -> {
      ProjectManager
          .getInstance()
          .addProjectManagerListener(project, projectManagerListener);
      return new MainViewModel(exerciseFilterOptions);
    });
  }

  /**
   * Registers a course project. This creates a main view model. It also starts the updater of the
   * course project. Calling this method again with the same project has no effect.
   */
  public void registerCourseProject(@NotNull CourseProject courseProject) {
    var key = new ProjectKey(courseProject.getProject());
    var mainViewModel = getMainViewModel(courseProject.getProject());
    mainViewModel.bannerViewModel.set(new CourseEndedBannerViewModel(courseProject,
        new DefaultNotifier()));
    var passwordStorage = new IntelliJPasswordStorage(courseProject.getCourse().getApiUrl());
    TokenAuthentication.Factory authenticationFactory =
        APlusTokenAuthentication.getFactoryFor(passwordStorage);
    courseProjects.computeIfAbsent(key, projectKey -> {
      courseProject.getCourse().register();
      courseProject.readAuthenticationFromStorage(passwordStorage, authenticationFactory);
      mainViewModel.courseViewModel.set(new CourseViewModel(courseProject.getCourse()));
      // This is needed here, because by default MainViewModel has an ExercisesTreeViewModel that
      // assumes that the project isn't a course project. This means that the user would be
      // instructed to turn the project into a course project for an example when the token is
      // missing.
      var exercisesViewModel = new ExercisesTreeViewModel(new ExercisesTree(), new Options());
      exercisesViewModel.setAuthenticated(courseProject.getAuthentication() != null);
      mainViewModel.exercisesViewModel.set(exercisesViewModel);
      courseProject.courseUpdated.addListener(
          mainViewModel.courseViewModel, ObservableProperty::valueChanged);
      courseProject.exercisesUpdated.addListener(mainViewModel, viewModel ->
          viewModel.updateExercisesViewModel(
              courseProject.getExerciseTree(), courseProject.getAuthentication()));
      courseProject.getCourseUpdater().restart();
      courseProject.getExercisesUpdater().restart();
      return courseProject;
    });
  }

  @Nullable
  public CourseProject getCourseProject(@Nullable Project project) {
    return courseProjects.get(new ProjectKey(project));
  }

  /**
   * Returns the {@link CourseFileManager} instance corresponding to the given project. A new
   * instance is created if no instance exists yet.
   */
  @NotNull
  public CourseFileManager getCourseFileManager(@NotNull Project project) {
    return courseFileManagers.computeIfAbsent(
        new ProjectKey(project),
        key -> new CourseFileManager(project, new DefaultNotifier())
    );
  }

  public boolean isAssistantMode() {
    return Boolean.parseBoolean(
        applicationPropertiesManager.getValue(A_PLUS_IS_ASSISTANT_MODE.getName()));
  }

  /**
   * Sets the property for showing assistant tools.
   */
  public void setAssistantMode(boolean assistantMode) {
    applicationPropertiesManager
        .setValue(A_PLUS_IS_ASSISTANT_MODE.getName(),
            String.valueOf(assistantMode));
  }

  /**
   * Method (getter) to check the property, responsible for showing REPL configuration window.
   */
  public boolean shouldShowReplConfigurationDialog() {
    return Boolean.parseBoolean(
        applicationPropertiesManager.getValue(A_PLUS_SHOW_REPL_CONFIGURATION_DIALOG.getName()));
  }

  /**
   * Method (setter) to set property, responsible for showing REPL configuration window.
   *
   * @param showReplConfigDialog a boolean value of the flag.
   */
  public void setShowReplConfigurationDialog(boolean showReplConfigDialog) {
    applicationPropertiesManager
        //  a String explicitly
        .setValue(A_PLUS_SHOW_REPL_CONFIGURATION_DIALOG.getName(),
            String.valueOf(showReplConfigDialog));
  }

  public String getImportedIdeSettingsId() {
    return applicationPropertiesManager.getValue(A_PLUS_IMPORTED_IDE_SETTINGS.getName());
  }

  public void setImportedIdeSettingsId(@NotNull String courseId) {
    applicationPropertiesManager.setValue(A_PLUS_IMPORTED_IDE_SETTINGS.getName(), courseId);
  }

  @Override
  public @NotNull Optional<Long> getDefaultGroupId() {
    String id = applicationPropertiesManager.getValue(A_PLUS_DEFAULT_GROUP.getName());
    return Optional.ofNullable(id).map(Long::parseLong);
  }

  @Override
  public void setDefaultGroupId(long groupId) {
    applicationPropertiesManager.setValue(A_PLUS_DEFAULT_GROUP.getName(), String.valueOf(groupId));
  }

  @Override
  public void clearDefaultGroupId() {
    applicationPropertiesManager.unsetValue(A_PLUS_DEFAULT_GROUP.getName());
  }

  /**
   * Sets unset local IDE settings to their default values.
   */
  public void initializeLocalIdeSettings() {
    if (!applicationPropertiesManager.isValueSet(A_PLUS_SHOW_REPL_CONFIGURATION_DIALOG.getName())) {
      setShowReplConfigurationDialog(true);
    }
    if (!applicationPropertiesManager.isValueSet(A_PLUS_IMPORTED_IDE_SETTINGS.getName())) {
      setImportedIdeSettingsId("");
    }
    exerciseFilterOptions.forEach(Option::init);
  }

  /**
   * Resets all local settings to their default values.
   */
  public void resetLocalSettings() {
    unsetLocalIdeSettings();
    initializeLocalIdeSettings();
  }

  /**
   * Unsets all the local IDE settings from {@link LocalIdeSettingsNames}.
   */
  public void unsetLocalIdeSettings() {
    Arrays.stream(LocalIdeSettingsNames.values())
        .map(LocalIdeSettingsNames::getName)
        .forEach(applicationPropertiesManager::unsetValue);
  }

  public interface PropertiesManager {
    @Nullable String getValue(@NotNull String key);

    void unsetValue(@NotNull String key);

    boolean isValueSet(@NotNull String key);

    default boolean getBoolean(@NotNull String key) {
      return getBoolean(key, false);
    }

    default boolean getBoolean(@NotNull String key, boolean defaultValue) {
      return isValueSet(key) ? Boolean.parseBoolean(getValue(key)) : defaultValue;
    }

    void setValue(@NotNull String key, @Nullable String value);

    /**
     * Sets property to the given value, or unsets it if the value equals default value.
     *
     * @param key Name of the property
     * @param value Value
     * @param defaultValue Default value
     * @param <T> Type of the value
     */
    default <T> void setValue(@NotNull String key, @Nullable T value, @Nullable T defaultValue) {
      if (Objects.equals(value, defaultValue)) {
        unsetValue(key);
      } else {
        setValue(key, String.valueOf(value));
      }
    }
  }

  private static class PropertiesComponentAdapter implements PropertiesManager {
    private final PropertiesComponent propertiesComponent;

    public PropertiesComponentAdapter(PropertiesComponent propertiesComponent) {
      this.propertiesComponent = propertiesComponent;
    }

    @Override
    public void setValue(@NotNull String key, @Nullable String value) {
      propertiesComponent.setValue(key, value);
    }

    @Override
    public @Nullable String getValue(@NotNull String key) {
      return propertiesComponent.getValue(key);
    }

    @Override
    public void unsetValue(@NotNull String key) {
      propertiesComponent.unsetValue(key);
    }

    @Override
    public boolean isValueSet(@NotNull String key) {
      return propertiesComponent.isValueSet(key);
    }
  }

  // Initialiaziton-on-demand holder
  private static class InstanceHolder {
    private static final PluginSettings INSTANCE =
        new PluginSettings(new PropertiesComponentAdapter(PropertiesComponent.getInstance()));
  }
}
