package fi.aalto.cs.apluscourses.intellij.model.task;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import fi.aalto.cs.apluscourses.ui.ideactivities.ComponentDatabase;
import fi.aalto.cs.apluscourses.ui.ideactivities.GenericHighlighter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReplPresenter extends IntelliJComponentPresenterBase {
  @NotNull
  private final Module module;

  /**
   * A constructor.
   */
  public ReplPresenter(@NotNull String instruction,
                       @NotNull String info,
                       @NotNull String module,
                       @NotNull Project project) throws IllegalArgumentException {
    super(instruction, info, project);
    var m = ModuleManager.getInstance(project).findModuleByName(module);
    if (m == null) {
      throw new IllegalArgumentException("Module not found: " + module);
    }
    this.module = m;
  }

  @Nullable
  @Override
  protected GenericHighlighter getHighlighter() {
    var toolWindow = ComponentDatabase.getToolWindow(ComponentDatabase.RUN_TOOL_WINDOW, project);
    return toolWindow == null ? null : new GenericHighlighter(toolWindow.getComponent());
  }

  @Override
  protected boolean tryToShow() {
    return ComponentDatabase.startAndShowRepl(module, project);
  }
}
