package fi.aalto.cs.apluscourses.intellij.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import fi.aalto.cs.apluscourses.intellij.DialogHelper;
import fi.aalto.cs.apluscourses.intellij.notifications.MissingFileNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.NetworkErrorNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.NotSubmittableNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.Notifier;
import fi.aalto.cs.apluscourses.intellij.services.Dialogs;
import fi.aalto.cs.apluscourses.intellij.services.MainViewModelProvider;
import fi.aalto.cs.apluscourses.model.APlusAuthentication;
import fi.aalto.cs.apluscourses.model.Authentication;
import fi.aalto.cs.apluscourses.model.Course;
import fi.aalto.cs.apluscourses.model.Exercise;
import fi.aalto.cs.apluscourses.model.ExerciseDataSource;
import fi.aalto.cs.apluscourses.model.ExerciseGroup;
import fi.aalto.cs.apluscourses.model.FileDoesNotExistException;
import fi.aalto.cs.apluscourses.model.FileFinder;
import fi.aalto.cs.apluscourses.model.Group;
import fi.aalto.cs.apluscourses.model.ModelExtensions;
import fi.aalto.cs.apluscourses.model.Submission;
import fi.aalto.cs.apluscourses.model.SubmissionInfo;
import fi.aalto.cs.apluscourses.model.SubmittableFile;
import fi.aalto.cs.apluscourses.presentation.CourseViewModel;
import fi.aalto.cs.apluscourses.presentation.MainViewModel;
import fi.aalto.cs.apluscourses.presentation.ModuleSelectionViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExercisesTreeViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.SubmissionViewModel;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class SubmitExerciseActionTest {

  Course course;
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
  String token;
  Authentication authentication;
  ExerciseDataSource.AuthProvider authProvider;
  ExerciseDataSource.Provider exerciseDataSourceProvider;
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
  SubmitExerciseAction.ModuleSource moduleSource;
  Dialogs dialogs;
  Notifier notifier;
  AnActionEvent event;
  SubmitExerciseAction action;

  /**
   * Called before each test.
   *
   * @throws IOException Never.
   * @throws FileDoesNotExistException Never.
   */
  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws IOException, FileDoesNotExistException {
    course = new ModelExtensions.TestCourse("91");
    exercise = new Exercise(12, "Test exercise");
    group = new Group(124, Collections.singletonList("Only you"));
    groups = Collections.singletonList(group);
    exerciseGroup = new ExerciseGroup("Test EG", Collections.singletonList(exercise));
    exerciseGroups = Collections.singletonList(exerciseGroup);
    fileName = "some_file.scala";
    fileKey = "file1";
    file = new SubmittableFile(fileKey, fileName);
    submissionInfo = new SubmissionInfo(1, new SubmittableFile[]{file});

    mainViewModel = new MainViewModel();

    courseViewModel = new CourseViewModel(course);
    mainViewModel.courseViewModel.set(courseViewModel);

    exerciseDataSource = spy(new ModelExtensions.TestExerciseDataSource());
    doReturn(groups).when(exerciseDataSource).getGroups(course);
    doReturn(submissionInfo).when(exerciseDataSource).getSubmissionInfo(exercise);
    doReturn(exerciseGroups).when(exerciseDataSource).getExerciseGroups(course);

    token = "testtoken";
    authentication = new APlusAuthentication(token.toCharArray());
    authProvider = mock(ExerciseDataSource.AuthProvider.class);
    doReturn(authentication).when(authProvider).create();

    exerciseDataSourceProvider = mock(ExerciseDataSource.Provider.class);
    doReturn(exerciseDataSource).when(exerciseDataSourceProvider).create(authProvider);

    mainViewModel.setExerciseDataSource(exerciseDataSourceProvider, authProvider);

    exercises = Objects.requireNonNull(mainViewModel.exercisesViewModel.get());
    exercises.getGroupViewModels().get(0).getExerciseViewModels().get(0).setSelected(true);

    moduleName = "MyModule";
    modulePath = Paths.get(moduleName);
    moduleFilePath = modulePath.resolve("MyModule.iml").toString();
    module = mock(Module.class);
    doReturn(moduleFilePath).when(module).getModuleFilePath();
    doReturn(moduleName).when(module).getName();

    project = mock(Project.class);

    filePath = modulePath.resolve(fileName);

    mainVmProvider = mock(MainViewModelProvider.class);
    doReturn(mainViewModel).when(mainVmProvider).getMainViewModel(project);

    fileFinder = mock(FileFinder.class);
    doReturn(filePath).when(fileFinder).tryFindFile(modulePath, fileName);
    doCallRealMethod().when(fileFinder).findFile(any(), any());
    doCallRealMethod().when(fileFinder).findFiles(any(), any());

    moduleSource = mock(SubmitExerciseAction.ModuleSource.class);
    doReturn(new Module[] { module }).when(moduleSource).getModules(project);

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

    action = new SubmitExerciseAction(mainVmProvider, fileFinder, moduleSource, dialogs, notifier);
  }

  @Test
  public void testSubmitExerciseAction() throws IOException {
    action.actionPerformed(event);

    ArgumentCaptor<Submission> submissionArg = ArgumentCaptor.forClass(Submission.class);
    verify(exerciseDataSource).submit(submissionArg.capture());

    Submission submission = submissionArg.getValue();
    assertEquals(group, submission.getGroup());
    assertEquals(exercise, submission.getExercise());

    Map<String, Path> files = new HashMap<>();
    files.put(fileKey, filePath);
    assertThat(submission.getFiles(), is(files));

    verifyNoInteractions(notifier);
  }

  @Test
  public void testNotifiesOfMissingFile() throws IOException {
    doReturn(null).when(fileFinder).tryFindFile(modulePath, fileName);

    action.actionPerformed(event);

    verifyNoInteractions(submissionDialog);
    verify(exerciseDataSource, never()).submit(any());

    ArgumentCaptor<MissingFileNotification> notificationArg =
        ArgumentCaptor.forClass(MissingFileNotification.class);

    verify(notifier).notify(notificationArg.capture(), eq(project));

    MissingFileNotification notification = notificationArg.getValue();
    assertEquals(fileName, notification.getFilename());
    assertEquals(modulePath, notification.getPath());

    verifyNoMoreInteractions(notifier);
  }

  @Test
  public void testNotifiesExerciseNotSubmittable() throws IOException {
    doReturn(new SubmissionInfo(1, new SubmittableFile[0]))
        .when(exerciseDataSource)
        .getSubmissionInfo(exercise);

    action.actionPerformed(event);

    verifyNoInteractions(moduleSelectionDialog);
    verifyNoInteractions(submissionDialog);
    verify(exerciseDataSource, never()).submit(any());

    verify(notifier).notify(any(NotSubmittableNotification.class), eq(project));

    verifyNoMoreInteractions(notifier);
  }

  @Test
  public void testNotifiesOfNetworkErrorWhenGettingSubmissionHistory() throws IOException {
    IOException exception = new IOException();
    doThrow(exception).when(exerciseDataSource).getSubmissionHistory(exercise);

    action.actionPerformed(event);

    verifyNoInteractions(submissionDialog);
    verify(exerciseDataSource, never()).submit(any());

    ArgumentCaptor<NetworkErrorNotification> notificationArg =
        ArgumentCaptor.forClass(NetworkErrorNotification.class);

    verify(notifier).notify(notificationArg.capture(), eq(project));

    NetworkErrorNotification notification = notificationArg.getValue();
    assertSame(exception, notification.getException());

    verifyNoMoreInteractions(notifier);
  }

  @Test
  public void testNotifiesOfNetworkErrorWhenGettingGroups() throws IOException {
    IOException exception = new IOException();
    doThrow(exception).when(exerciseDataSource).getGroups(course);

    action.actionPerformed(event);

    verifyNoInteractions(submissionDialog);
    verify(exerciseDataSource, never()).submit(any());

    ArgumentCaptor<NetworkErrorNotification> notificationArg =
        ArgumentCaptor.forClass(NetworkErrorNotification.class);

    verify(notifier).notify(notificationArg.capture(), eq(project));

    NetworkErrorNotification notification = notificationArg.getValue();
    assertSame(exception, notification.getException());

    verifyNoMoreInteractions(notifier);
  }

  @Test
  public void testNotifiesOfNetworkErrorWhenGettingSubmissionInfo() throws IOException {
    IOException exception = new IOException();
    doThrow(exception).when(exerciseDataSource).getSubmissionInfo(exercise);

    action.actionPerformed(event);

    verifyNoInteractions(moduleSelectionDialog);
    verifyNoInteractions(submissionDialog);
    verify(exerciseDataSource, never()).submit(any());

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
    doThrow(exception).when(exerciseDataSource).submit(any());

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

    verify(exerciseDataSource, never()).submit(any());

    verifyNoInteractions(notifier);
  }
}