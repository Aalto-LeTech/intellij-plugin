package fi.aalto.cs.apluscourses.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import fi.aalto.cs.apluscourses.intellij.model.CourseProject;
import fi.aalto.cs.apluscourses.intellij.notifications.DefaultNotifier;
import fi.aalto.cs.apluscourses.intellij.notifications.ExerciseNotSelectedNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.MissingFileNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.NetworkErrorNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.Notifier;
import fi.aalto.cs.apluscourses.intellij.services.Dialogs;
import fi.aalto.cs.apluscourses.intellij.services.MainViewModelProvider;
import fi.aalto.cs.apluscourses.intellij.services.PluginSettings;
import fi.aalto.cs.apluscourses.intellij.utils.Interfaces;
import fi.aalto.cs.apluscourses.intellij.utils.VfsUtil;
import fi.aalto.cs.apluscourses.model.ComponentInstaller;
import fi.aalto.cs.apluscourses.model.ComponentInstallerImpl;
import fi.aalto.cs.apluscourses.model.Course;
import fi.aalto.cs.apluscourses.model.Exercise;
import fi.aalto.cs.apluscourses.model.FileDoesNotExistException;
import fi.aalto.cs.apluscourses.model.FileFinder;
import fi.aalto.cs.apluscourses.model.Module;
import fi.aalto.cs.apluscourses.model.SubmissionFileInfo;
import fi.aalto.cs.apluscourses.model.SubmissionResult;
import fi.aalto.cs.apluscourses.presentation.exercise.DownloadSubmissionViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExerciseViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExercisesTreeViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.SubmissionResultViewModel;
import fi.aalto.cs.apluscourses.ui.InstallerDialogs;
import fi.aalto.cs.apluscourses.utils.CoursesClient;
import fi.aalto.cs.apluscourses.utils.async.SimpleAsyncTaskManager;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DownloadSubmissionAction extends AnAction {
  @NotNull
  private final MainViewModelProvider mainViewModelProvider;

  @NotNull
  private final Interfaces.AuthenticationProvider authenticationProvider;

  @NotNull
  private final FileFinder fileFinder;

  @NotNull
  private final Dialogs dialogs;

  @NotNull
  private final Notifier notifier;

  // TODO: store language and default group ID in the object model and read them from there
  private final Interfaces.LanguageSource languageSource;

  @NotNull
  private final Interfaces.AssistantModeProvider assistantModeProvider;

  @NotNull
  private final ComponentInstaller.Factory componentInstallerFactory;

  @NotNull
  private final InstallerDialogs.Factory dialogsFactory;
  private final Interfaces.FileRefresher fileRefresher;
  private final Interfaces.FileBrowser fileBrowser;


  /**
   * Constructor with reasonable defaults.
   */
  public DownloadSubmissionAction() {
    this(
        PluginSettings.getInstance(),
        project -> Optional.ofNullable(PluginSettings.getInstance().getCourseProject(project))
            .map(CourseProject::getAuthentication).orElse(null),
        VfsUtil::findFileInDirectory,
        Dialogs.DEFAULT,
        new DefaultNotifier(),
        project -> PluginSettings.getInstance().getCourseFileManager(project).getLanguage(),
        () -> PluginSettings.getInstance().isAssistantMode(),
        new ComponentInstallerImpl.FactoryImpl<>(new SimpleAsyncTaskManager()),
        InstallerDialogs::new,
        Interfaces.FileRefresherImpl::refreshPath,
        Interfaces.FileBrowserImpl::navigateTo
    );
  }


  /**
   * Construct an exercise submission action with the given parameters. This constructor is useful
   * for testing purposes.
   */
  public DownloadSubmissionAction(@NotNull MainViewModelProvider mainViewModelProvider,
                                  @NotNull Interfaces.AuthenticationProvider authenticationProvider,
                                  @NotNull FileFinder fileFinder,
                                  @NotNull Dialogs dialogs,
                                  @NotNull Notifier notifier,
                                  @NotNull Interfaces.LanguageSource languageSource,
                                  @NotNull Interfaces.AssistantModeProvider assistantModeProvider,
                                  @NotNull ComponentInstaller.Factory componentInstallerFactory,
                                  @NotNull InstallerDialogs.Factory dialogsFactory,
                                  @NotNull Interfaces.FileRefresher fileRefresher,
                                  @NotNull Interfaces.FileBrowser fileBrowser) {
    this.mainViewModelProvider = mainViewModelProvider;
    this.authenticationProvider = authenticationProvider;
    this.fileFinder = fileFinder;
    this.dialogs = dialogs;
    this.notifier = notifier;
    this.languageSource = languageSource;
    this.assistantModeProvider = assistantModeProvider;
    this.componentInstallerFactory = componentInstallerFactory;
    this.dialogsFactory = dialogsFactory;
    this.fileRefresher = fileRefresher;
    this.fileBrowser = fileBrowser;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    var project = e.getProject();
    var mainViewModel = mainViewModelProvider.getMainViewModel(project);
    var courseViewModel = mainViewModel.courseViewModel.get();
    var exercisesTreeViewModel = mainViewModel.exercisesViewModel.get();
    if (project == null || courseViewModel == null || exercisesTreeViewModel == null) {
      return;
    }
    var selectedItem = exercisesTreeViewModel.getSelectedItem();
    if (!(selectedItem instanceof SubmissionResultViewModel)) {
      return;
    }
    var submissionId = selectedItem.getId();

    var selection = (ExercisesTreeViewModel.ExerciseTreeSelection) exercisesTreeViewModel.findSelected();
    var selectedExercise = selection.getExercise();
    var selectedExerciseGroup = selection.getExerciseGroup();
    if (selectedExercise == null || selectedExerciseGroup == null) {
      notifier.notifyAndHide(new ExerciseNotSelectedNotification(), project);
      return;
    }

    var exercise = selectedExercise.getModel();
    var course = courseViewModel.getModel();

    var selectedModule = getSelectedModule(project, course, exercise);

    var installedModules = Arrays
        .stream(ModuleManager.getInstance(project).getModules())
        .map(com.intellij.openapi.module.Module::getName)
        .collect(Collectors.toList());

    var downloadSubmissionViewModel = new DownloadSubmissionViewModel(course, selectedModule, submissionId,
        installedModules);
    if (!dialogs.create(downloadSubmissionViewModel, project).showAndGet()) {
      return;
    }

    var module = downloadSubmissionViewModel.selectedModule.get();
    var newName = downloadSubmissionViewModel.moduleName.get();

    if (module == null || newName == null) {
      return;
    }

    var moduleCopy = module.copy(newName);

    var moduleVf = LocalFileSystem.getInstance().findFileByIoFile(moduleCopy.getFullPath().toFile());

    componentInstallerFactory.getInstallerFor(course, dialogsFactory.getDialogs(e.getProject()))
        .installAsync(List.of(moduleCopy),
            () -> fileRefresher.refreshPath(moduleVf,
                () -> downloadFiles(project,
                    ((SubmissionResult) selectedItem.getModel()).getFilesInfo(),
                    moduleCopy)));
  }

  @Nullable
  private Module getSelectedModule(@NotNull Project project,
                                   @NotNull Course course,
                                   @NotNull Exercise exercise) {
    var language = languageSource.getLanguage(project);

    var exerciseModules = course.getExerciseModules().get(exercise.getId());

    var moduleName = Optional
        .ofNullable(exerciseModules)
        .map(self -> self.get(language));

    var selectedComponent = moduleName.map(course::getComponentIfExists).orElse(null);
    Module selectedModule = null;
    if (selectedComponent instanceof Module) {
      selectedModule = (Module) selectedComponent;
    }
    return selectedModule;
  }

  private void downloadFiles(@Nullable Project project,
                             SubmissionFileInfo @NotNull [] submissionFilesInfo,
                             @NotNull Module module) {
    var courseViewModel = mainViewModelProvider.getMainViewModel(project).courseViewModel.get();
    if (courseViewModel == null || project == null) {
      return;
    }
    var course = courseViewModel.getModel();
    course.validate();

    var auth = authenticationProvider.getAuthentication(project);

    for (var info : submissionFilesInfo) {
      try {
        var file = fileFinder.findFile(module.getFullPath(), info.getFileName()).toFile();
        CoursesClient.fetch(new URL(info.getUrl()), file, auth);
        fileBrowser.navigateTo(file, project);
      } catch (FileDoesNotExistException ex) {
        notifier.notifyAndHide(new MissingFileNotification(module.getPath(), info.getFileName(), true), project);
      } catch (IOException ex) {
        notifier.notifyAndHide(new NetworkErrorNotification(ex), project);
      }
    }

  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setVisible(assistantModeProvider.isAssistantMode());
    e.getPresentation().setEnabled(false);
    var mainViewModel = mainViewModelProvider.getMainViewModel(e.getProject());
    var courseViewModel = mainViewModel.courseViewModel.get();
    var exercisesTreeViewModel = mainViewModel.exercisesViewModel.get();
    if (courseViewModel != null && exercisesTreeViewModel != null) {
      var selection = (ExercisesTreeViewModel.ExerciseTreeSelection) exercisesTreeViewModel.findSelected();
      ExerciseViewModel selectedExercise = selection.getExercise();
      var selectedItem = exercisesTreeViewModel.getSelectedItem();
      e.getPresentation().setEnabled(selectedItem instanceof SubmissionResultViewModel
          && exercisesTreeViewModel.isAuthenticated()
          && selectedExercise != null
          && selectedExercise.isSubmittable());
    }
  }
}
