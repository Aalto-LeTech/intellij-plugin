package fi.aalto.cs.apluscourses.intellij.actions;

import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.project.Project;
import fi.aalto.cs.apluscourses.intellij.model.CourseProject;
import fi.aalto.cs.apluscourses.intellij.model.IntelliJModelFactory;
import fi.aalto.cs.apluscourses.intellij.model.SettingsImporter;
import fi.aalto.cs.apluscourses.intellij.notifications.CourseConfigurationError;
import fi.aalto.cs.apluscourses.intellij.notifications.CourseFileError;
import fi.aalto.cs.apluscourses.intellij.notifications.CourseVersionOutdatedError;
import fi.aalto.cs.apluscourses.intellij.notifications.CourseVersionTooNewError;
import fi.aalto.cs.apluscourses.intellij.notifications.DefaultNotifier;
import fi.aalto.cs.apluscourses.intellij.notifications.NetworkErrorNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.Notifier;
import fi.aalto.cs.apluscourses.intellij.services.PluginSettings;
import fi.aalto.cs.apluscourses.model.ComponentInstaller;
import fi.aalto.cs.apluscourses.model.ComponentInstallerImpl;
import fi.aalto.cs.apluscourses.model.Course;
import fi.aalto.cs.apluscourses.model.MalformedCourseConfigurationException;
import fi.aalto.cs.apluscourses.presentation.CourseItemViewModel;
import fi.aalto.cs.apluscourses.presentation.CourseProjectViewModel;
import fi.aalto.cs.apluscourses.presentation.CourseSelectionViewModel;
import fi.aalto.cs.apluscourses.ui.InstallerDialogs;
import fi.aalto.cs.apluscourses.ui.courseproject.CourseProjectActionDialogs;
import fi.aalto.cs.apluscourses.ui.courseproject.CourseProjectActionDialogsImpl;
import fi.aalto.cs.apluscourses.ui.ideactivities.ComponentDatabase;
import fi.aalto.cs.apluscourses.utils.BuildInfo;
import fi.aalto.cs.apluscourses.utils.PostponedRunnable;
import fi.aalto.cs.apluscourses.utils.Version;
import fi.aalto.cs.apluscourses.utils.async.SimpleAsyncTaskManager;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CourseProjectAction extends AnAction {

  private static final Logger logger = LoggerFactory.getLogger(CourseProjectAction.class);

  public static final String ACTION_ID = CourseProjectAction.class.getCanonicalName();

  @NotNull
  private final CourseFactory courseFactory;

  private final boolean useCourseFile;

  @NotNull
  private final SettingsImporter settingsImporter;

  @NotNull
  private final ComponentInstaller.Factory installerFactory;

  @NotNull
  private final PostponedRunnable ideRestarter;

  @NotNull
  private final CourseProjectActionDialogs dialogs;

  @NotNull
  private final InstallerDialogs.Factory installerDialogsFactory;

  @NotNull
  private final Notifier notifier;

  private final ExecutorService executor;

  private static final List<CourseItemViewModel> AVAILABLE_COURSES = List.of(
      new CourseItemViewModel("O1", "Fall 2020",
          "https://grader.cs.aalto.fi/static/O1_2020/projects/o1_course_config.json"),
      new CourseItemViewModel("Ohjelmointistudio 2 / Programming Studio A", "Spring 2021",
          "https://grader.cs.aalto.fi/static/studio2_k2021/projects/s2_course_config.json")
  );

  /**
   * Construct a course project action with the given parameters.
   *
   * @param courseFactory    An instance of {@link CourseFactory} that is used to create a course
   *                         instance from a URL.
   * @param useCourseFile    Determines whether a course file is read/created or not. This is useful
   *                         only for testing purposes.
   * @param settingsImporter An instance of {@link SettingsImporter} that is used to import IDE and
   *                         project settings.
   * @param installerFactory The factory used to create the component installer. The component
   *                         installer is then used to install the automatically installed
   *                         components of the course. This is useful mainly for testing.
   * @param ideRestarter     A {@link PostponedRunnable} that is used to restart the IDE after
   *                         everything related to the course project action is done. In practice,
   *                         this is either immediately after the action is done, or after all
   *                         automatically installed components for the course have been installed.
   *                         Since the installation of automatically installed components may take
   *                         quite a while, it is advisable for this to show the user a confirmation
   *                         dialog regarding the restart.
   * @param notifier         Notification bus.
   */
  public CourseProjectAction(@NotNull CourseFactory courseFactory,
                             boolean useCourseFile,
                             @NotNull SettingsImporter settingsImporter,
                             @NotNull ComponentInstaller.Factory installerFactory,
                             @NotNull PostponedRunnable ideRestarter,
                             @NotNull CourseProjectActionDialogs dialogs,
                             @NotNull InstallerDialogs.Factory installerDialogsFactory,
                             @NotNull Notifier notifier,
                             @NotNull ExecutorService executor) {
    this.courseFactory = courseFactory;
    this.useCourseFile = useCourseFile;
    this.settingsImporter = settingsImporter;
    this.installerFactory = installerFactory;
    this.ideRestarter = ideRestarter;
    this.dialogs = dialogs;
    this.installerDialogsFactory = installerDialogsFactory;
    this.notifier = notifier;
    this.executor = executor;
  }

  /**
   * Construct a course project action with sensible defaults.
   */
  public CourseProjectAction() {
    this.courseFactory = (url, project) -> Course.fromUrl(url, new IntelliJModelFactory(project));
    this.useCourseFile = true;
    this.settingsImporter = new SettingsImporter();
    this.installerFactory = new ComponentInstallerImpl.FactoryImpl<>(new SimpleAsyncTaskManager());
    this.dialogs = new CourseProjectActionDialogsImpl();
    this.ideRestarter = new PostponedRunnable(() -> {
      if (dialogs.showRestartDialog()) {
        ((ApplicationEx) ApplicationManager.getApplication()).restart(true);
      }
    });
    this.installerDialogsFactory = InstallerDialogs::new;
    this.notifier = new DefaultNotifier();
    this.executor = JobScheduler.getScheduler();
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();

    if (project == null) {
      return;
    }

    URL courseUrl = tryGetCourseUrl(project);
    if (courseUrl == null) {
      return;
    }

    Course course = tryGetCourse(project, courseUrl);
    if (course == null) {
      return;
    }

    var versionComparison =
        BuildInfo.INSTANCE.courseVersion.compareTo(course.getVersion());

    if (versionComparison == Version.ComparisonStatus.MAJOR_TOO_OLD
        || versionComparison == Version.ComparisonStatus.MAJOR_TOO_NEW) {
      notifier.notify(
          versionComparison == Version.ComparisonStatus.MAJOR_TOO_OLD
          ? new CourseVersionOutdatedError() : new CourseVersionTooNewError(), project);
      return;
    }

    CourseProjectViewModel courseProjectViewModel
        = new CourseProjectViewModel(course, settingsImporter.currentlyImportedIdeSettings());
    if (!dialogs.showMainDialog(project, courseProjectViewModel)) {
      return;
    }

    String language = Objects.requireNonNull(courseProjectViewModel.languageProperty.get());
    if (!tryCreateCourseFile(project, courseUrl, language)) {
      return;
    }

    boolean importIdeSettings = courseProjectViewModel.shouldApplyNewIdeSettings();

    String basePath = project.getBasePath();
    if (basePath == null) {
      logger.error("Settings could not be imported because (default?) project does not have path.");
      return;
    }

    if (useCourseFile) {
      // The course file not created in testing.
      var courseProject = new CourseProject(course, courseUrl, project);
      PluginSettings.getInstance().registerCourseProject(courseProject);
    }

    Future<?> autoInstallDone = executor.submit(() -> startAutoInstalls(course, project));

    Future<Boolean> projectSettingsImported =
        executor.submit(() -> tryImportProjectSettings(project, Paths.get(basePath), course));

    Future<Boolean> ideSettingsImported =
        executor.submit(() -> importIdeSettings && tryImportIdeSettings(project, course));

    ComponentDatabase.showAPlusToolWindow(project);

    executor.execute(() -> {
      try {
        autoInstallDone.get();
        if (projectSettingsImported.get() && importIdeSettings //  NOSONAR
            && ideSettingsImported.get()) { //  NOSONAR
          ideRestarter.run();
        }
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException ex) {
        logger.error("An exception was thrown in an asynchronous call", ex);
      }
    });
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    // This action is available only if a non-default project is open
    Project project = e.getProject();
    e.getPresentation().setEnabledAndVisible(project != null && !project.isDefault());
  }

  @FunctionalInterface
  public interface CourseFactory {

    @NotNull
    Course fromUrl(@NotNull URL courseUrl, @NotNull Project project)
        throws IOException, MalformedCourseConfigurationException;
  }

  @Nullable
  private URL tryGetCourseUrl(@NotNull Project project) {
    try {
      if (useCourseFile && PluginSettings.getInstance().getCourseFileManager(project).load()) {
        return PluginSettings.getInstance().getCourseFileManager(project).getCourseUrl();
      }

      CourseSelectionViewModel viewModel = new CourseSelectionViewModel(AVAILABLE_COURSES);
      boolean cancelled = !dialogs.showCourseSelectionDialog(project, viewModel);
      return cancelled ? null : new URL(Objects.requireNonNull(viewModel.selectedCourseUrl.get()));
    } catch (MalformedURLException e) {
      // User entered an invalid URL (or the default list has an invalid URL, which would be a bug)
      logger.error("Malformed course configuration file URL", e);
      notifier.notify(new NetworkErrorNotification(e), project);
      return null;
    } catch (IOException e) {
      notifier.notify(new NetworkErrorNotification(e), project);
      return null;
    }
  }

  /**
   * Returns a course created from the course configuration file at the given URL. The user is
   * notified if the course initialization fails.
   *
   * @param project   The currently open project.
   * @param courseUrl The URL from which the course configuration file is downloaded.
   * @return The course created from the course configuration file or null in case of an error.
   */
  @Nullable
  private Course tryGetCourse(@NotNull Project project, @NotNull URL courseUrl) {
    try {
      return courseFactory.fromUrl(courseUrl, project);
    } catch (IOException e) {
      notifier.notify(new NetworkErrorNotification(e), project);
      return null;
    } catch (MalformedCourseConfigurationException e) {
      logger.error("Malformed course configuration file", e);
      notifier.notify(new CourseConfigurationError(e), project);
      return null;
    }
  }

  private void startAutoInstalls(@NotNull Course course, @NotNull Project project) {
    ComponentInstaller.Dialogs installerDialogs = installerDialogsFactory.getDialogs(project);
    ComponentInstaller installer = installerFactory.getInstallerFor(course, installerDialogs);
    installer.install(course.getAutoInstallComponents());
  }

  /**
   * Creates a file in the project settings directory which contains the given course configuration
   * file URL and language.
   *
   * @return True if the file was successfully created, false otherwise.
   */
  private boolean tryCreateCourseFile(@NotNull Project project,
                                      @NotNull URL courseUrl,
                                      @NotNull String language) {
    try {
      if (useCourseFile) {
        PluginSettings
            .getInstance()
            .getCourseFileManager(project)
            .createAndLoad(courseUrl, language);
      }
      return true;
    } catch (IOException e) {
      logger.error("Failed to create course file", e);
      notifier.notify(new CourseFileError(e), project);
      return false;
    }
  }

  /**
   * Tries importing project settings from the given course. Shows a notification to the user if a
   * network error occurs.
   *
   * @return True if project settings were successfully imported, false otherwise.
   */
  private boolean tryImportProjectSettings(@NotNull Project project,
                                           @NotNull Path basePath,
                                           @NotNull Course course) {
    try {
      settingsImporter.importProjectSettings(basePath, course);
      return true;
    } catch (IOException e) {
      logger.error("Failed to import project settings", e);
      notifier.notify(new NetworkErrorNotification(e), project);
      return false;
    }
  }

  /**
   * Tries importing IDE settings from the given course. Shows a notification to the user if a
   * network error occurs.
   *
   * @return True if IDE settings were successfully imported, false otherwise.
   */
  private boolean tryImportIdeSettings(@NotNull Project project, @NotNull Course course) {
    try {
      settingsImporter.importIdeSettings(course);
      return true;
    } catch (IOException e) {
      logger.error("Failed to import IDE settings", e);
      notifier.notify(new NetworkErrorNotification(e), project);
      return false;
    }
  }
}
