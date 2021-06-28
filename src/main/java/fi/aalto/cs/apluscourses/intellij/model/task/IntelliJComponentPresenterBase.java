package fi.aalto.cs.apluscourses.intellij.model.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import fi.aalto.cs.apluscourses.model.task.ComponentPresenter;
import fi.aalto.cs.apluscourses.ui.ideactivities.OverlayPane;
import java.awt.Component;
import org.jetbrains.annotations.NotNull;

public abstract class IntelliJComponentPresenterBase implements ComponentPresenter {

  private final @NotNull String instruction;
  private final @NotNull String info;
  protected final @NotNull Project project;
  private OverlayPane overlayPane;

  protected IntelliJComponentPresenterBase(@NotNull String instruction,
                                           @NotNull String info,
                                           @NotNull Project project) {
    this.instruction = instruction;
    this.info = info;
    this.project = project;
  }

  @Override
  public void highlight() {
    ApplicationManager.getApplication()
        .invokeLater(this::highlightInternal,ModalityState.NON_MODAL);
  }

  @RequiresEdt
  private void highlightInternal() {
    if (overlayPane != null) {
      overlayPane.remove();
    }

    Component component = getComponent();
    if (component == null) {
      if (tryToShow()) {
        highlightInternal();
        return;
      }
      throw new IllegalStateException("Component was not found!");
    }
    overlayPane = OverlayPane.installOverlay();
    overlayPane.showComponent(component);
    overlayPane.addPopup(component, instruction, info);
  }

  @Override
  public void removeHighlight() {
    ApplicationManager.getApplication().invokeLater(this::removeHighlightInternal);
  }

  @RequiresEdt
  private void removeHighlightInternal() {
    if (overlayPane != null) {
      overlayPane.remove();
      overlayPane = null;
    }
  }

  protected abstract Component getComponent();

  protected abstract boolean tryToShow();

  @Override
  public boolean isVisible() {
    return getComponent() != null && getComponent().isShowing();
  }
}
