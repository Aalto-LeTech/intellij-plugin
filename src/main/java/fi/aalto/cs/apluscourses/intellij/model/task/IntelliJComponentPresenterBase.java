package fi.aalto.cs.apluscourses.intellij.model.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import fi.aalto.cs.apluscourses.model.task.CancelHandler;
import fi.aalto.cs.apluscourses.model.task.ComponentPresenter;
import fi.aalto.cs.apluscourses.ui.ideactivities.ComponentDatabase;
import fi.aalto.cs.apluscourses.ui.ideactivities.GenericHighlighter;
import fi.aalto.cs.apluscourses.ui.ideactivities.OverlayPane;
import org.jetbrains.annotations.NotNull;

public abstract class IntelliJComponentPresenterBase implements ComponentPresenter {

  private final @NotNull String instruction;
  private final @NotNull String info;
  protected final @NotNull Project project;
  private OverlayPane overlayPane;
  private volatile CancelHandler cancelHandler; //NOSONAR

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
        .invokeLater(this::highlightInternal, ModalityState.NON_MODAL);
  }

  @RequiresEdt
  private void highlightInternal() {
    if (overlayPane != null) {
      overlayPane.remove();
    }

    var highlighter = getHighlighter();

    if (highlighter == null) {
      if (tryToShow()) {
        highlightInternal();
        return;
      }
      throw new IllegalStateException("Component was not found!");
    }
    overlayPane = OverlayPane.installOverlay();
    overlayPane.addHighlighter(highlighter);
    overlayPane.clickEvent.addListener(cancelHandler, CancelHandler::onCancel);
    overlayPane.addPopup(highlighter.getComponent(), instruction, info);

    var progressButton = ComponentDatabase.getProgressButton();
    if (progressButton != null) {
      overlayPane.addHighlighter(new GenericHighlighter(progressButton));
    }
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

  protected abstract GenericHighlighter getHighlighter();

  protected abstract boolean tryToShow();

  @Override
  public boolean isVisible() {
    return getHighlighter() != null && getHighlighter().getComponent().isShowing();
  }

  @Override
  public void setCancelHandler(CancelHandler cancelHandler) {
    this.cancelHandler = cancelHandler;
  }
}
