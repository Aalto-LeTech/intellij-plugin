package fi.aalto.cs.apluscourses.intellij.actions;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.intellij.notification.Notification;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightIdeaTestCase;

import fi.aalto.cs.apluscourses.intellij.notifications.MissingFileNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.MissingModuleNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.NetworkErrorNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.NotSubmittableNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.Notifier;
import fi.aalto.cs.apluscourses.intellij.services.MainViewModelProvider;
import fi.aalto.cs.apluscourses.model.APlusAuthentication;
import fi.aalto.cs.apluscourses.model.Course;
import fi.aalto.cs.apluscourses.model.Exercise;
import fi.aalto.cs.apluscourses.model.Group;
import fi.aalto.cs.apluscourses.model.SubmissionHistory;
import fi.aalto.cs.apluscourses.model.SubmissionInfo;
import fi.aalto.cs.apluscourses.presentation.APlusAuthenticationViewModel;
import fi.aalto.cs.apluscourses.presentation.CourseViewModel;
import fi.aalto.cs.apluscourses.presentation.MainViewModel;
import fi.aalto.cs.apluscourses.presentation.ModuleSelectionViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExerciseViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExercisesTreeViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.SubmissionViewModel;
import fi.aalto.cs.apluscourses.ui.exercise.ModuleSelectionDialog;
import fi.aalto.cs.apluscourses.ui.exercise.SubmissionDialog;
import fi.aalto.cs.apluscourses.utils.observable.CompoundObservableProperty;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

public class SubmitExerciseActionTest extends LightIdeaTestCase {

  private AnActionEvent actionEvent;

  private Module module;

  private MainViewModel mainViewModel;
  private MainViewModelProvider mainViewModelProvider;

  private SubmitExerciseAction.SubmissionInfoFactory submissionInfoFactory;
  private AtomicInteger submissionInfoCallCount = new AtomicInteger(0);

  private SubmitExerciseAction.SubmissionHistoryFactory submissionHistoryFactory;
  private AtomicInteger submissionHistoryCallCount = new AtomicInteger(0);

  private SubmitExerciseAction.GroupsFactory groupsFactory;
  private AtomicInteger groupsCallCount = new AtomicInteger(0);

  private SubmitExerciseAction.FileFinder fileFinder;

  private ModuleSelectionDialog.Factory moduleDialogFactory;
  private AtomicBoolean moduleDialogIsShown;

  private SubmissionDialog.Factory submissionDialogFactory;
  private AtomicBoolean submissionDialogIsShown;

  private TestNotifier notifier;

  class TestModuleSelectionDialog extends ModuleSelectionDialog {

    private ModuleSelectionViewModel viewModel;
    private boolean ok;

    public TestModuleSelectionDialog(ModuleSelectionViewModel viewModel, boolean ok) {
      super(viewModel);
      this.viewModel = viewModel;
      this.ok = ok;
    }

    @Override
    public boolean showAndGet() {
      viewModel.setSelectedModule(module);
      moduleDialogIsShown.set(true);
      return ok;
    }
  }

  class TestSubmissionDialog extends SubmissionDialog {

    public TestSubmissionDialog(SubmissionViewModel viewModel) {
      super(viewModel);
    }

    @Override
    public boolean showAndGet() {
      submissionDialogIsShown.set(true);
      return true;
    }

  }

  class TestNotifier implements Notifier {
    private Notification notification;

    @Override
    public void notify(@NotNull Notification notification, @Nullable Project project) {
      this.notification = notification;
    }

    public Notification getNotification() {
      return notification;
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    actionEvent = mock(AnActionEvent.class);
    Project project = mock(Project.class);
    doReturn(project).when(actionEvent).getProject();

    module = mock(Module.class);
    doReturn(Paths.get("parent", "child").toString())
        .when(module).getModuleFilePath();
    doReturn("name").when(module).getName();

    ExerciseViewModel exercise = new ExerciseViewModel(new Exercise(0, ""));
    ExercisesTreeViewModel exercisesViewModel = mock(ExercisesTreeViewModel.class);
    doReturn(exercise).when(exercisesViewModel).getSelectedExercise();

    mainViewModel = new MainViewModel();
    CourseViewModel courseViewModel = new CourseViewModel(mock(Course.class));
    APlusAuthenticationViewModel authenticationViewModel = mock(APlusAuthenticationViewModel.class);
    doReturn(mock(APlusAuthentication.class)).when(authenticationViewModel).getAuthentication();
    ((CompoundObservableProperty) mainViewModel.exercisesViewModel).setConverter(
        (p1, p2) -> exercisesViewModel);
    mainViewModel.courseViewModel.set(courseViewModel);
    mainViewModel.authenticationViewModel.set(authenticationViewModel);

    mainViewModelProvider = p -> mainViewModel;

    submissionInfoFactory = (ex, auth) -> {
      submissionInfoCallCount.incrementAndGet();
      return new SubmissionInfo(10, Collections.singletonList("file.scala"));
    };

    submissionHistoryFactory = (ex, auth) -> {
      submissionHistoryCallCount.incrementAndGet();
      return new SubmissionHistory(4);
    };

    groupsFactory = (course, auth) -> {
      groupsCallCount.incrementAndGet();
      return Collections.singletonList(new Group(0, Collections.singletonList("Submit alone")));
    };

    fileFinder = (directory, name) -> Paths.get("file.scala");

    moduleDialogFactory = viewModel -> new TestModuleSelectionDialog(viewModel, true);
    moduleDialogIsShown = new AtomicBoolean(false);

    submissionDialogFactory = TestSubmissionDialog::new;
    submissionDialogIsShown = new AtomicBoolean(false);

    notifier = new TestNotifier();
  }

  @Test
  public void testSubmitExerciseAction() {
    SubmitExerciseAction action = new SubmitExerciseAction(
        mainViewModelProvider,
        submissionInfoFactory,
        submissionHistoryFactory,
        groupsFactory,
        fileFinder,
        moduleDialogFactory,
        submissionDialogFactory,
        notifier
    );
    action.actionPerformed(actionEvent);

    Assert.assertEquals("The action fetches information necessary for submission",
        1, submissionInfoCallCount.get());
    Assert.assertEquals("The action fetches the submission history",
        1, submissionHistoryCallCount.get());
    Assert.assertEquals("The action fetches the available groups", 1, groupsCallCount.get());
    Assert.assertTrue("The module selection dialog is shown", moduleDialogIsShown.get());
    Assert.assertTrue("The submission dialog is shown", submissionDialogIsShown.get());
  }

  @Test
  public void testNotifiesOfMissingFile() {
    SubmitExerciseAction action = new SubmitExerciseAction(
        mainViewModelProvider,
        submissionInfoFactory,
        submissionHistoryFactory,
        groupsFactory,
        (directory, filename) -> null,
        moduleDialogFactory,
        submissionDialogFactory,
        notifier
    );
    action.actionPerformed(actionEvent);

    Assert.assertFalse("The submission dialog is not shown", submissionDialogIsShown.get());
    Assert.assertTrue("A notification of the missing file is shown",
        notifier.getNotification() instanceof MissingFileNotification);
  }

  @Test
  public void testNotifiesOfMissingModule() {
    SubmitExerciseAction action = new SubmitExerciseAction(
        mainViewModelProvider,
        submissionInfoFactory,
        submissionHistoryFactory,
        groupsFactory,
        fileFinder,
        moduleDialogFactory,
        submissionDialogFactory,
        notifier
    );

    Course course = mock(Course.class);
    Map<Long, Map<String, String>> map
        = Collections.singletonMap(0L, Collections.singletonMap("en", "nonexistent module"));
    doReturn(map).when(course).getExerciseModules();
    mainViewModel.courseViewModel.set(new CourseViewModel(course));

    action.actionPerformed(actionEvent);

    Assert.assertFalse("The module selection dialog is not shown", moduleDialogIsShown.get());
    Assert.assertFalse("The submission dialog is not shown", submissionDialogIsShown.get());
    Assert.assertTrue("A missing module notification is shown",
        notifier.getNotification() instanceof MissingModuleNotification);
  }

  @Test
  public void testNotifiesExerciseNotSubmittable() {
    SubmitExerciseAction action = new SubmitExerciseAction(
        mainViewModelProvider,
        (exercise, authentication) -> new SubmissionInfo(10, Collections.emptyList()),
        submissionHistoryFactory,
        groupsFactory,
        fileFinder,
        moduleDialogFactory,
        submissionDialogFactory,
        notifier
    );
    action.actionPerformed(actionEvent);

    Assert.assertFalse("The submission dialog is not shown", submissionDialogIsShown.get());
    Assert.assertTrue("A notification of the error is shown",
        notifier.getNotification() instanceof NotSubmittableNotification);
  }

  @Test
  public void testNotifiesOfNetworkError1() {
    SubmitExerciseAction action = new SubmitExerciseAction(
        mainViewModelProvider,
        (exercise, authentication) -> {
          throw new IOException();
        },
        submissionHistoryFactory,
        groupsFactory,
        fileFinder,
        moduleDialogFactory,
        submissionDialogFactory,
        notifier
    );
    action.actionPerformed(actionEvent);

    Assert.assertFalse("The submission dialog is not shown", submissionDialogIsShown.get());
    Assert.assertTrue("A network error notification is shown",
        notifier.getNotification() instanceof NetworkErrorNotification);
  }

  @Test
  public void testNotifiesOfNetworkError2() {
    SubmitExerciseAction action = new SubmitExerciseAction(
        mainViewModelProvider,
        submissionInfoFactory,
        (exercise, authentication) -> {
          throw new IOException();
        },
        groupsFactory,
        fileFinder,
        moduleDialogFactory,
        submissionDialogFactory,
        notifier
    );
    action.actionPerformed(actionEvent);

    Assert.assertFalse("The submission dialog is not shown", submissionDialogIsShown.get());
    Assert.assertTrue("A network error notification is shown",
        notifier.getNotification() instanceof NetworkErrorNotification);
  }

  @Test
  public void testNotifiesOfNetworkError3() {
    SubmitExerciseAction action = new SubmitExerciseAction(
        mainViewModelProvider,
        submissionInfoFactory,
        submissionHistoryFactory,
        (course, authentication) -> {
          throw new IOException();
        },
        fileFinder,
        moduleDialogFactory,
        submissionDialogFactory,
        notifier
    );
    action.actionPerformed(actionEvent);

    Assert.assertFalse("The submission dialog is not shown", submissionDialogIsShown.get());
    Assert.assertTrue("A network error notification is shown",
        notifier.getNotification() instanceof NetworkErrorNotification);
  }

  @Test
  public void testModuleSelectionDialogCancel() {
    SubmitExerciseAction action = new SubmitExerciseAction(
        mainViewModelProvider,
        submissionInfoFactory,
        submissionHistoryFactory,
        groupsFactory,
        fileFinder,
        viewModel -> new TestModuleSelectionDialog(viewModel, false),
        submissionDialogFactory,
        notifier
    );
    action.actionPerformed(actionEvent);

    Assert.assertFalse("The submission dialog is not shown", submissionDialogIsShown.get());
    Assert.assertEquals("The submission information is fetched",
        1, submissionInfoCallCount.get());
    Assert.assertEquals("The submission history is fetched",
        1, submissionHistoryCallCount.get());
    Assert.assertEquals("The groups are fetched", 1, groupsCallCount.get());
  }

}
