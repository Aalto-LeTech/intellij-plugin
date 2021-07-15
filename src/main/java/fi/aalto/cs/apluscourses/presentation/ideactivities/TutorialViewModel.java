package fi.aalto.cs.apluscourses.presentation.ideactivities;

import fi.aalto.cs.apluscourses.model.Tutorial;
import fi.aalto.cs.apluscourses.model.TutorialExercise;
import fi.aalto.cs.apluscourses.model.task.ActivityFactory;
import fi.aalto.cs.apluscourses.model.task.Task;
import fi.aalto.cs.apluscourses.utils.APlusLocalizationUtil;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class TutorialViewModel {

  private final TutorialExercise tutorialExercise;
  private final ActivityFactory activityFactory;

  private final Object lock = new Object();

  private Task currentTask = null;

  private int currentTaskIndex;

  private final int tasksAmount;

  /**
   * Constructor.
   */
  public TutorialViewModel(@NotNull TutorialExercise tutorialExercise,
                           @NotNull ActivityFactory activityFactory) {
    this.tutorialExercise = tutorialExercise;
    List<Task> tasks = tutorialExercise.getTutorial().getTasks();
    if (!tasks.isEmpty()) {
      this.currentTask = tasks.get(0);
    }
    currentTaskIndex = 0;
    tasksAmount = tasks.size();
    this.activityFactory = activityFactory;
  }

  /**
   * Begins the nextTask which is indicated by the currentTask variable.
   */
  public void startNextTask() {
    synchronized (lock) {
      currentTask.taskCompleted.addListener(this, TutorialViewModel::currentTaskCompleted);
      incrementIndex();
      if (currentTask.startTask(activityFactory)) {
        currentTaskCompleted();
      }
      // The Task/Tutorial has been completed prematurely
      // because the Activity was already performed.
      // E.g. the file was open already, variable renamed etc.
      // No need to show the instructions for this Task
      // as we are proceeding directly to the next one.
      // We can instead inform the user that they have already completed it
      // through the alreadyComplete boolean.

    }
  }

  /**
   * Sets the currentTask as completed and fress up any resources associated with it.
   * If this task was the last one the Tutorial is completed,
   * if not, then the currentTask is set to point to the next Task to be done.
   */
  public void currentTaskCompleted() {
    synchronized (lock) {
      Tutorial tutorial = tutorialExercise.getTutorial();
      currentTask.endTask();
      currentTask.taskCompleted.removeCallback(this);
      currentTask = tutorial.getNextTask(currentTask);
      if (currentTask == null) {
        tutorial.onComplete();
      } else {
        startNextTask();
      }
    }
  }

  /**
   * Functionality for canceling the Tutorial, mainly frees up resources.
   */
  public void cancelTutorial() {
    synchronized (lock) {
      if (currentTask != null) {
        currentTask.endTask();
        currentTask.taskCompleted.removeCallback(this);
        currentTask = null;
        tutorialExercise.getTutorial().onComplete();
      }
    }
  }


  public @NotNull String getTitle() {
    return APlusLocalizationUtil.getEnglishName(tutorialExercise.getName());
  }

  public @NotNull Tutorial getTutorial() {
    return tutorialExercise.getTutorial();
  }

  public int getCurrentTaskIndex() {
    return currentTaskIndex;
  }

  private void incrementIndex() {
    currentTaskIndex++;
  }

  public int getTasksAmount() {
    return tasksAmount;
  }
}
