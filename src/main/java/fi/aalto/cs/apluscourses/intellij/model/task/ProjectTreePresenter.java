package fi.aalto.cs.apluscourses.intellij.model.task;

import static fi.aalto.cs.apluscourses.ui.ideactivities.ComponentDatabase.PROJECT_TOOL_WINDOW;

import com.intellij.openapi.project.Project;
import fi.aalto.cs.apluscourses.ui.ideactivities.ComponentDatabase;
import fi.aalto.cs.apluscourses.ui.ideactivities.GenericHighlighter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectTreePresenter extends IntelliJComponentPresenterBase {
  public ProjectTreePresenter(@NotNull String instruction,
                              @NotNull String info,
                              @NotNull Project project) {
    super(instruction, info, project);
  }

  @Override
  protected @Nullable GenericHighlighter getHighlighter() {
    var toolWindow = ComponentDatabase.getToolWindow(PROJECT_TOOL_WINDOW, project);
    return toolWindow == null ? null : new GenericHighlighter(toolWindow.getComponent());
  }

  @Override
  public boolean tryToShow() {
    return ComponentDatabase.showToolWindow(PROJECT_TOOL_WINDOW, project);
  }
}
