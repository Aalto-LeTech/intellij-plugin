package fi.aalto.cs.apluscourses.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import fi.aalto.cs.apluscourses.intellij.model.task.IntelliJActivityFactory;
import fi.aalto.cs.apluscourses.intellij.notifications.DefaultNotifier;
import fi.aalto.cs.apluscourses.intellij.notifications.ExerciseNotSelectedNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.Notifier;
import fi.aalto.cs.apluscourses.intellij.services.MainViewModelProvider;
import fi.aalto.cs.apluscourses.intellij.services.PluginSettings;
import fi.aalto.cs.apluscourses.model.Authentication;
import fi.aalto.cs.apluscourses.model.TutorialExercise;
import fi.aalto.cs.apluscourses.presentation.CourseViewModel;
import fi.aalto.cs.apluscourses.presentation.MainViewModel;
import fi.aalto.cs.apluscourses.presentation.base.BaseTreeViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExerciseGroupViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExerciseViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExercisesTreeViewModel;
import fi.aalto.cs.apluscourses.presentation.ideactivities.TutorialDialogs;
import fi.aalto.cs.apluscourses.presentation.ideactivities.TutorialViewModel;
import java.util.Optional;
import javax.swing.JOptionPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class TutorialAction extends AnAction {
  private final @NotNull MainViewModelProvider mainViewModelProvider;
  private final @NotNull TutorialAuthenticationProvider authenticationProvider;
  private final @NotNull Notifier notifier;
  private final @NotNull TutorialDialogs dialogs;

  /**
   * Empty Constructor.
   */
  public TutorialAction() {
    this(PluginSettings.getInstance(), new DefaultNotifier(), project -> {
      var courseProject = PluginSettings.getInstance().getCourseProject(project);
      return courseProject == null ? null : courseProject.getAuthentication();
    }, new DefaultDialogs());
  }

  /**
   * Constructor.
   */
  public TutorialAction(@NotNull MainViewModelProvider mainViewModelProvider,
                        @NotNull Notifier notifier,
                        @NotNull TutorialAuthenticationProvider authenticationProvider,
                        @NotNull TutorialDialogs dialogs) {
    this.mainViewModelProvider = mainViewModelProvider;
    this.notifier = notifier;
    this.authenticationProvider = authenticationProvider;
    this.dialogs = dialogs;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    if (e.getProject() == null) {
      return;
    }
    doTutorial(e.getProject());
  }

  private void doTutorial(@NotNull Project project) {
    MainViewModel mainViewModel = mainViewModelProvider.getMainViewModel(project);
    CourseViewModel courseViewModel = mainViewModel.courseViewModel.get();
    ExercisesTreeViewModel exercisesViewModel = mainViewModel.exercisesViewModel.get();
    Authentication authentication = authenticationProvider.getAuthentication(project);

    if (courseViewModel == null || exercisesViewModel == null || authentication == null) {
      return;
    }

    BaseTreeViewModel.Selection selection = exercisesViewModel.findSelected();
    ExerciseViewModel selectedExercise = (ExerciseViewModel) selection.getLevel(2);
    ExerciseGroupViewModel selectedExerciseGroup = (ExerciseGroupViewModel) selection.getLevel(1);
    if (selectedExercise == null || selectedExerciseGroup == null
            || !ExerciseViewModel.Status.TUTORIAL.equals(selectedExercise.getStatus())) {
      notifier.notifyAndHide(new ExerciseNotSelectedNotification(), project);
      return;
    }
    TutorialExercise tutorialExercise = (TutorialExercise) selectedExercise.getModel();

    Optional.ofNullable(mainViewModel.tutorialViewModel.get())
        .ifPresent(TutorialViewModel::cancelTutorial);

    TutorialViewModel tutorialViewModel =
        new TutorialViewModel(tutorialExercise, new IntelliJActivityFactory(project), dialogs);
    if (dialogs.confirmStart(tutorialViewModel)) {
      mainViewModelProvider.getMainViewModel(project).tutorialViewModel.set(tutorialViewModel);
      tutorialViewModel.getTutorial().tutorialCompleted
          .addListener(mainViewModel, this::onTutorialComplete);
      tutorialViewModel.startNextTask();
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    MainViewModel mainViewModel = mainViewModelProvider.getMainViewModel(e.getProject());
    CourseViewModel courseViewModel = mainViewModel.courseViewModel.get();
    Authentication authentication = authenticationProvider.getAuthentication(e.getProject());
    ExercisesTreeViewModel exercisesViewModel = mainViewModel.exercisesViewModel.get();
    boolean isTutorialSelected =
            exercisesViewModel != null
            && authentication != null && courseViewModel != null
            && exercisesViewModel.getSelectedItem() != null
            && !(exercisesViewModel.getSelectedItem() instanceof ExerciseGroupViewModel)
            && exercisesViewModel.findSelected().getLevel(2) != null
            && ExerciseViewModel.Status.TUTORIAL.equals(
               ((ExerciseViewModel) exercisesViewModel.findSelected().getLevel(2)).getStatus());

    e.getPresentation().setVisible(e.getProject() != null && isTutorialSelected);
  }

  @FunctionalInterface
  public interface TutorialAuthenticationProvider {
    Authentication getAuthentication(@Nullable Project project);
  }

  private static class DefaultDialogs implements TutorialDialogs {

    @Override
    public boolean confirmStart(@NotNull TutorialViewModel tutorialViewModel) {
      return JOptionPane.showConfirmDialog(null,
        "A tutorial will start.",
        tutorialViewModel.getTitle(),
        JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    @Override
    public boolean confirmCancel(@NotNull TutorialViewModel tutorialViewModel) {
      return JOptionPane.showConfirmDialog(null,
        "Cancel tutorial?",
        tutorialViewModel.getTitle(),
        JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    @Override
    public void end(@NotNull TutorialViewModel tutorialViewModel) {
      JOptionPane.showMessageDialog(null,
          "The tutorial has ended.",
          tutorialViewModel.getTitle(),
          JOptionPane.INFORMATION_MESSAGE);
    }
  }

  private void onTutorialComplete(@NotNull MainViewModel mainViewModel) {
    TutorialViewModel viewModel = mainViewModel.tutorialViewModel.get();
    if (viewModel != null) {
      viewModel.getTutorial().tutorialCompleted.removeCallback(mainViewModel);
      mainViewModel.tutorialViewModel.set(null);
      dialogs.end(viewModel);
    }
  }
}
