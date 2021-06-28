package fi.aalto.cs.apluscourses.intellij.model.task;

import com.intellij.openapi.project.Project;
import fi.aalto.cs.apluscourses.ui.ideactivities.ComponentDatabase;
import java.awt.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EditorPresenter extends IntelliJComponentPresenterBase {
  @NotNull
  private final String path;

  public EditorPresenter(@NotNull String instruction,
                         @NotNull String info,
                         @NotNull String path,
                         @NotNull Project project) {
    super(instruction, info, project);
    this.path = path;
  }

  @Override
  protected @Nullable Component getComponent() {
    return ComponentDatabase.getEditorWindow();
  }

  @Override
  public boolean tryToShow() {
    return ComponentDatabase.showFile(path, project);
  }
}
