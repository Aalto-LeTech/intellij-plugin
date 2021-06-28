package fi.aalto.cs.apluscourses.intellij.model.task;

import com.intellij.openapi.project.Project;
import fi.aalto.cs.apluscourses.ui.ideactivities.ComponentDatabase;
import java.awt.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectTreePresenter extends IntelliJComponentPresenterBase {
  public ProjectTreePresenter(@NotNull String instruction,
                              @NotNull String info,
                              @NotNull Project project) {
    super(instruction, info, project);
  }

  @Override
  protected @Nullable Component getComponent() {
    return ComponentDatabase.getProjectPane();
  }

  @Override
  public boolean tryToShow() {
    return ComponentDatabase.showProjectToolWindow(project);
  }
}
