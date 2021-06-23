package fi.aalto.cs.apluscourses.intellij.model.task;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import fi.aalto.cs.apluscourses.model.task.ActivitiesListener;
import fi.aalto.cs.apluscourses.model.task.ListenerCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IdeActionListener implements AnActionListener, ActivitiesListener {
  private final Project project;
  private MessageBusConnection messageBusConnection;
  private final String actionName;
  private final ListenerCallback callback;
  private String filePath;

  /**
   * Constructor.
   * @param callback The callback for when the task is complete
   * @param project The project where the Tutorial is happening
   * @param action The action to be performed
   * @param filePath The file's path (optional).
   */
  public IdeActionListener(ListenerCallback callback, Project project,
                           String action, @Nullable String filePath) {
    this.callback = callback;
    this.project = project;
    this.actionName = action;
    this.filePath = filePath;
  }

  @Override
  public boolean registerListener() {
    messageBusConnection = project.getMessageBus().connect();
    messageBusConnection.subscribe(AnActionListener.TOPIC, this);
    return false;
  }

  @Override
  public void unregisterListener() {
    if (messageBusConnection != null) {
      messageBusConnection.disconnect();
      messageBusConnection = null;
    }
  }

  @Override
  public void beforeActionPerformed(@NotNull AnAction action, @NotNull DataContext dataContext,
                                    @NotNull AnActionEvent event) {
    //perhaps create subclasses?
    boolean complete = true;
    if (filePath != null && !filePath.isEmpty()) {
      this.filePath = project.getBasePath() + filePath;
      VirtualFile file = event.getDataContext().getData(PlatformDataKeys.VIRTUAL_FILE);
      complete = file != null && filePath.equals(file.getPath());
    }
    if (complete && actionName.equals(action.getTemplateText())) {
      callback.callback();
    }
  }
}
