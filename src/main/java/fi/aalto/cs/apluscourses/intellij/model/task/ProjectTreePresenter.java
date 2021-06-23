package fi.aalto.cs.apluscourses.intellij.model.task;

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
    return ComponentDatabase.getProjectPane() == null ? null
        : new GenericHighlighter(ComponentDatabase.getProjectPane());
  }

  @Override
  public boolean tryToShow() {
    return ComponentDatabase.showProjectToolWindow(project);
  }
}
