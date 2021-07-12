package fi.aalto.cs.apluscourses.intellij.model.task;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import fi.aalto.cs.apluscourses.model.task.ActivitiesListener;
import fi.aalto.cs.apluscourses.model.task.ListenerCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.scala.console.ScalaConsoleInfo;
import org.jetbrains.plugins.scala.console.ScalaLanguageConsole;

public class ReplInputListener implements ActivitiesListener, ProcessListener {

  private final ListenerCallback callback;
  private final Project project;
  private final String input;
  private ProcessHandler processHandler;
  private ScalaLanguageConsole console;

  /**
   * A constructor with the wanted input as a string.
   */
  public ReplInputListener(ListenerCallback callback,
                           Project project,
                           String input) {
    this.callback = callback;
    this.project = project;
    this.input = input;
  }

  @Override
  public boolean registerListener() {
    console = ScalaConsoleInfo.getConsole(project);
    processHandler = ScalaConsoleInfo.getProcessHandler(project);

    processHandler.addProcessListener(this);

    return false;
  }

  @Override
  public void unregisterListener() {
    if (processHandler != null) {
      processHandler.removeProcessListener(this);
      processHandler = null;
    }
  }

  @Override
  public void startNotified(@NotNull ProcessEvent event) {
    // Do nothing
  }

  @Override
  public void processTerminated(@NotNull ProcessEvent event) {
    // Do nothing
  }

  @Override
  public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
    if (input.equals(getLastLine())) {
      callback.callback();
    }
  }

  private String getLastLine() {
    var lines = console.getHistory().split("\n");
    return lines[lines.length - 1].trim();
  }

}
