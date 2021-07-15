package fi.aalto.cs.apluscourses.intellij.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import fi.aalto.cs.apluscourses.intellij.DialogHelper;
import fi.aalto.cs.apluscourses.intellij.model.ProjectModuleSource;
import fi.aalto.cs.apluscourses.intellij.notifications.ExerciseNotSelectedNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.MissingFileNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.MissingModuleNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.NetworkErrorNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.NotSubmittableNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.Notifier;
import fi.aalto.cs.apluscourses.intellij.notifications.SubmissionSentNotification;
import fi.aalto.cs.apluscourses.intellij.services.DefaultGroupIdSetting;
import fi.aalto.cs.apluscourses.intellij.services.Dialogs;
import fi.aalto.cs.apluscourses.intellij.services.MainViewModelProvider;
import fi.aalto.cs.apluscourses.intellij.utils.Interfaces;
import fi.aalto.cs.apluscourses.model.Authentication;
import fi.aalto.cs.apluscourses.model.Course;
import fi.aalto.cs.apluscourses.model.Exercise;
import fi.aalto.cs.apluscourses.model.ExerciseDataSource;
import fi.aalto.cs.apluscourses.model.ExerciseGroup;
import fi.aalto.cs.apluscourses.model.ExercisesTree;
import fi.aalto.cs.apluscourses.model.FileDoesNotExistException;
import fi.aalto.cs.apluscourses.model.FileFinder;
import fi.aalto.cs.apluscourses.model.Group;
import fi.aalto.cs.apluscourses.model.ModelExtensions;
import fi.aalto.cs.apluscourses.model.Points;
import fi.aalto.cs.apluscourses.model.Submission;
import fi.aalto.cs.apluscourses.model.SubmissionInfo;
import fi.aalto.cs.apluscourses.model.SubmittableFile;
import fi.aalto.cs.apluscourses.presentation.CourseViewModel;
import fi.aalto.cs.apluscourses.presentation.MainViewModel;
import fi.aalto.cs.apluscourses.presentation.ModuleSelectionViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExercisesTreeViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.SubmissionViewModel;
import fi.aalto.cs.apluscourses.presentation.filter.Options;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class SubmitExerciseActionTest {

  Course course;
  long exerciseId;
  Exercise exercise;
  Group group;
  List<Group> groups;
  ExerciseGroup exerciseGroup;
  List<ExerciseGroup> exerciseGroups;
  String fileKey;
  String fileName;
  SubmittableFile file;
  SubmissionInfo submissionInfo;
  MainViewModel mainViewModel;
  CourseViewModel courseViewModel;
  Authentication authentication;
  ExerciseDataSource exerciseDataSource;
  ExercisesTreeViewModel exercises;
  String moduleName;
  Path modulePath;
  String moduleFilePath;
  Module module;
  Project project;
  Path filePath;
  DialogHelper<ModuleSelectionViewModel> moduleSelectionDialog;
  DialogHelper<SubmissionViewModel> submissionDialog;
  Dialogs.Factory<ModuleSelectionViewModel> moduleSelectionDialogFactory;
  Dialogs.Factory<SubmissionViewModel> submissionDialogFactory;
  MainViewModelProvider mainVmProvider;
  FileFinder fileFinder;
  ProjectModuleSource moduleSource;
  Dialogs dialogs;
  Notifier notifier;
  AnActionEvent event;
  SubmitExerciseAction action;
  Points points;
  Interfaces.Tagger tagger;
  Interfaces.DocumentSaver documentSaver;
  Interfaces.LanguageSource languageSource;
  DefaultGroupIdSetting defaultGroupIdSetting;

  /**
   * Called before each test.
   *
   * @throws IOException               Never.
   * @throws FileDoesNotExistException Never.
   */
  @Before
  public void setUp() throws Exception {
    exerciseId = 12;
    fileName = "some_file.scala";
    fileKey = "file1";
    file = new SubmittableFile(fileKey, fileName);
    String language = "fi";
    submissionInfo = new SubmissionInfo(Map.of(language, Collections.singletonList(file)));
    exercise = new Exercise(
        exerciseId, "Test exercise", "http://localhost:10000", submissionInfo, 0, 0, OptionalLong.empty());
    group = new Group(124, Collections.singletonList("Only you"));
    groups = Collections.singletonList(group);
    exerciseGroup = new ExerciseGroup(0, "Test EG", "", true);
    exerciseGroup.addExercise(exercise);
    exerciseGroups = Collections.singletonList(exerciseGroup);

    mainViewModel = new MainViewModel(new Options());

    authentication = mock(Authentication.class);
    points = new Points(
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap());

    exerciseDataSource = mock(ExerciseDataSource.class);
    course = spy(new ModelExtensions.TestCourse("91", "NineOne Course", exerciseDataSource));
    doReturn(groups).when(exerciseDataSource).getGroups(course, authentication);
    doReturn(points).when(exerciseDataSource).getPoints(course, authentication);
    doReturn(exerciseGroups).when(exerciseDataSource).getExerciseGroups(course, authentication);
    doReturn("http://localhost:1000")
        .when(exerciseDataSource)
        .submit(any(Submission.class), any(Authentication.class));

    courseViewModel = new CourseViewModel(course);
    mainViewModel.courseViewModel.set(courseViewModel);

    exercises = new ExercisesTreeViewModel(new ExercisesTree(exerciseGroups), new Options());
    exercises.getChildren().get(0).getChildren().get(0).setSelected(true);
    mainViewModel.exercisesViewModel.set(exercises);

    moduleName = "MyModule";
    modulePath = Paths.get(moduleName);
    moduleFilePath = modulePath.resolve("MyModule.iml").toString();
    module = mock(Module.class);
    // Needed because SubmitExerciseAction uses ModuleUtilCore#getModuleDirPath
    doReturn(moduleFilePath).when(module).getModuleFilePath();
    doReturn(moduleName).when(module).getName();

    project = mock(Project.class);
    doReturn(FileUtilRt.getTempDirectory()).when(project).getBasePath();

    filePath = modulePath.resolve(fileName);

    mainVmProvider = mock(MainViewModelProvider.class);
    doReturn(mainViewModel).when(mainVmProvider).getMainViewModel(project);
    var authProvider = mock(Interfaces.AuthenticationProvider.class);
    doReturn(authentication).when(authProvider).getAuthentication(project);


    fileFinder = mock(FileFinder.class);
    doReturn(filePath).when(fileFinder).tryFindFile(modulePath, fileName);
    doCallRealMethod().when(fileFinder).findFile(any(), any());
    doCallRealMethod().when(fileFinder).findFiles(any(), any());

    moduleSource = mock(ProjectModuleSource.class);
    doReturn(new Module[]{module}).when(moduleSource).getModules(project);
    doReturn(module).when(moduleSource).getModule(project, moduleName);

    dialogs = new Dialogs();

    notifier = mock(Notifier.class);

    event = mock(AnActionEvent.class);
    doReturn(project).when(event).getProject();

    moduleSelectionDialog = spy(new DialogHelper<>(viewModel -> {
      viewModel.selectedModule.set(viewModel.getModules()[0]);
      return true;
    }));
    moduleSelectionDialogFactory = new DialogHelper.Factory<>(moduleSelectionDialog, project);
    dialogs.register(ModuleSelectionViewModel.class, moduleSelectionDialogFactory);

    submissionDialog = spy(new DialogHelper<>(viewModel -> {
      // we select the second group of the list (the first is "submit alone")
      viewModel.selectedGroup.set(viewModel.getAvailableGroups().get(1));
      return true;
    }));
    submissionDialogFactory = new DialogHelper.Factory<>(submissionDialog, project);
    dialogs.register(SubmissionViewModel.class, submissionDialogFactory);

    tagger = mock(Interfaces.Tagger.class);

    documentSaver = mock(Interfaces.DocumentSaver.class);
    
    languageSource = mock(Interfaces.LanguageSource.class);
    doReturn(language).when(languageSource).getLanguage(project);
    
    defaultGroupIdSetting = new TestDefaultGroupIdSetting();

    action = new SubmitExerciseAction(mainVmProvider, authProvider, fileFinder, moduleSource,
        dialogs, notifier, tagger, documentSaver, languageSource, defaultGroupIdSetting);
  }

  @Test
  public void testSubmitExerciseAction() throws IOException {
    action.actionPerformed(event);

    ArgumentCaptor<Submission> submissionArg = ArgumentCaptor.forClass(Submission.class);
    verify(exerciseDataSource).submit(submissionArg.capture(), same(authentication));

    Submission submission = submissionArg.getValue();
    assertEquals(group, submission.getGroup());
    assertEquals(exercise, submission.getExercise());

    Map<String, Path> files = new HashMap<>();
    files.put(fileKey, filePath);
    assertThat(submission.getFiles(), is(files));

    ArgumentCaptor<SubmissionSentNotification> notificationArg =
        ArgumentCaptor.forClass(SubmissionSentNotification.class);

    verify(notifier).notifyAndHide(notificationArg.capture(), eq(project));
    verify(tagger).putSystemLabel(any(), anyString(), anyInt());
    verify(documentSaver).saveAllDocuments();
  }

  @Test
  public void testNotifiesNoExerciseSelected() {
    exercises.getChildren().get(0).getChildren().get(0).setSelected(false);

    action.actionPerformed(event);

    ArgumentCaptor<ExerciseNotSelectedNotification> notification
        = ArgumentCaptor.forClass(ExerciseNotSelectedNotification.class);

    verify(notifier).notifyAndHide(notification.capture(), eq(project));
  }

  @Test
  public void testNotifiesOfMissingFile() throws IOException {
    doReturn(null).when(fileFinder).tryFindFile(modulePath, fileName);

    action.actionPerformed(event);

    verifyNoInteractions(submissionDialog);
    verify(exerciseDataSource, never()).submit(any(), any());

    ArgumentCaptor<MissingFileNotification> notificationArg =
        ArgumentCaptor.forClass(MissingFileNotification.class);

    verify(notifier).notify(notificationArg.capture(), eq(project));

    MissingFileNotification notification = notificationArg.getValue();
    assertEquals(fileName, notification.getFilename());
    assertEquals(modulePath, notification.getPath());

    verifyNoMoreInteractions(notifier);
  }

  @Test
  public void testNotifiesOfMissingModule() throws IOException {
    String nonexistentModuleName = "nonexistent module";

    Map<Long, Map<String, String>> map = Collections.singletonMap(exerciseId,
        Collections.singletonMap("fi", nonexistentModuleName));
    doReturn(map).when(course).getExerciseModules();

    action.actionPerformed(event);

    verifyNoInteractions(moduleSelectionDialog);
    verifyNoInteractions(submissionDialog);
    verify(exerciseDataSource, never()).submit(any(), any());

    ArgumentCaptor<MissingModuleNotification> notificationArg =
        ArgumentCaptor.forClass(MissingModuleNotification.class);

    verify(notifier).notify(notificationArg.capture(), eq(project));

    MissingModuleNotification notification = notificationArg.getValue();
    assertEquals(nonexistentModuleName, notification.getModuleName());

    verifyNoMoreInteractions(notifier);
  }

  @Test
  public void testNotifiesExerciseNotSubmittable() throws IOException {
    var exercise = new Exercise(0, "", "", new SubmissionInfo(Map.of()), 0, 0,
        OptionalLong.empty());
    var exerciseGroup = new ExerciseGroup(0, "", "", true);
    exerciseGroup.addExercise(exercise);
    mainViewModel.exercisesViewModel.set(
        new ExercisesTreeViewModel(new ExercisesTree(List.of(exerciseGroup)), new Options()));
    mainViewModel.exercisesViewModel
        .get().getChildren().get(0).getChildren().get(0).setSelected(true);

    action.actionPerformed(event);

    verifyNoInteractions(moduleSelectionDialog);
    verifyNoInteractions(submissionDialog);
    verify(exerciseDataSource, never()).submit(any(), any());

    verify(notifier).notify(any(NotSubmittableNotification.class), eq(project));

    verifyNoMoreInteractions(notifier);
  }

  @Test
  public void testNotifiesOfNetworkErrorWhenGettingGroups() throws IOException {
    IOException exception = new IOException();
    doThrow(exception).when(exerciseDataSource).getGroups(course, authentication);

    action.actionPerformed(event);

    verifyNoInteractions(submissionDialog);
    verify(exerciseDataSource, never()).submit(any(), any());

    ArgumentCaptor<NetworkErrorNotification> notificationArg =
        ArgumentCaptor.forClass(NetworkErrorNotification.class);

    verify(notifier).notify(notificationArg.capture(), eq(project));

    NetworkErrorNotification notification = notificationArg.getValue();
    assertSame(exception, notification.getException());

    verifyNoMoreInteractions(notifier);
  }

  @Test
  public void testNotifiesOfNetworkErrorWhenSubmitting() throws IOException {
    IOException exception = new IOException();
    doThrow(exception).when(exerciseDataSource).submit(any(), any());

    action.actionPerformed(event);

    ArgumentCaptor<NetworkErrorNotification> notificationArg =
        ArgumentCaptor.forClass(NetworkErrorNotification.class);

    verify(notifier).notify(notificationArg.capture(), eq(project));

    NetworkErrorNotification notification = notificationArg.getValue();
    assertSame(exception, notification.getException());

    verifyNoMoreInteractions(notifier);
  }

  @Test
  public void testModuleSelectionDialogCancel() throws IOException {
    dialogs.register(SubmissionViewModel.class, (submission, project) -> () -> false);

    action.actionPerformed(event);

    verify(exerciseDataSource, never()).submit(any(), any());

    verifyNoInteractions(notifier);
  }

  public static class TestDefaultGroupIdSetting implements DefaultGroupIdSetting {
    private volatile @Nullable Long defaultGroupId = null;
    
    @Override
    public @NotNull Optional<Long> getDefaultGroupId() {
      return Optional.ofNullable(defaultGroupId);
    }
  
    @Override
    public void setDefaultGroupId(long groupId) {
      defaultGroupId = groupId;
    }
  
    @Override
    public void clearDefaultGroupId() {
      defaultGroupId = null;
    }
  }
}
