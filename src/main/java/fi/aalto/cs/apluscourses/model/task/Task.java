package fi.aalto.cs.apluscourses.model.task;

import com.intellij.openapi.application.ApplicationManager;
import fi.aalto.cs.apluscourses.utils.Event;
import fi.aalto.cs.apluscourses.utils.JsonUtil;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

public class Task {
  public static final int REFRESH_INTERVAL = 1000;
  public final @NotNull Event taskCompleted = new Event();

  private final @NotNull String instruction;
  private final @NotNull String info;
  private final @NotNull String component;
  private final @NotNull Arguments componentArguments;
  private final @NotNull String action;
  private final @NotNull Arguments actionArguments;
  private Timer timer;

  private ActivitiesListener listener;
  private ComponentPresenter presenter;

  /**
   * Task constructor.
   */
  public Task(@NotNull String instruction,
              @NotNull String info,
              @NotNull String component,
              @NotNull Arguments componentArguments,
              @NotNull String action,
              @NotNull Arguments actionArguments) {
    this.instruction = instruction;
    this.action = action;
    this.actionArguments = actionArguments;
    this.info = info;
    this.component = component;
    this.componentArguments = componentArguments;
  }

  public @NotNull String getAction() {
    return action;
  }

  /**
   * Ends the task.
   */
  public synchronized void endTask() {
    timer.cancel();
    if (listener != null) {
      listener.unregisterListener();
      listener = null;
    }
    if (presenter != null) {
      presenter.removeHighlight();
      presenter = null;
    }
  }

  /**
   * Starts a task.
   *
   * @param activityFactory Creator for listener and presenter objects.
   * @return True, if task was already completed, otherwise false
   */
  public synchronized boolean startTask(ActivityFactory activityFactory) {
    if (presenter != null || listener != null) {
      throw new IllegalStateException();
    }
    presenter = activityFactory.createPresenter(component, instruction, info, componentArguments,
        actionArguments);
    this.timer = new Timer();
    startTimer();
    listener = activityFactory.createListener(action, actionArguments, taskCompleted::trigger);
    if (listener.registerListener()) {
      return true;
    }
    presenter.highlight();
    return false;
  }

  /**
   * Builds a Task object from JSON.
   *
   * @param jsonObject JSON.
   * @return A task.
   */
  public static @NotNull Task fromJsonObject(@NotNull JSONObject jsonObject) {
    return new Task(
        jsonObject.getString("instruction"),
        jsonObject.getString("info"),
        jsonObject.getString("component"),
        parseArguments(jsonObject.optJSONObject("componentArguments")),
        jsonObject.getString("action"),
        parseArguments(jsonObject.optJSONObject("actionArguments"))
    );
  }

  protected static @NotNull Arguments parseArguments(@Nullable JSONObject jsonObject) {
    return jsonObject == null ? Arguments.empty()
        : JsonUtil.parseObject(jsonObject, JSONObject::get,
            Function.identity(), Function.identity())::get;
  }

  private void startTimer() {
    timer.scheduleAtFixedRate(new TaskRefresher(), REFRESH_INTERVAL, REFRESH_INTERVAL);
  }

  private class TaskRefresher extends TimerTask {

    @Override
    public void run() {
      ApplicationManager.getApplication().invokeLater(() -> {
        if (presenter == null) {
          return;
        }
        if (!presenter.isVisible()) {
          presenter.highlight();
        }
      });
    }
  }
}

