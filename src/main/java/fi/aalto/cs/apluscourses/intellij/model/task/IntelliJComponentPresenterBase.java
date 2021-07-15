package fi.aalto.cs.apluscourses.intellij.model.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import fi.aalto.cs.apluscourses.model.task.ComponentPresenter;
import fi.aalto.cs.apluscourses.ui.ideactivities.ComponentDatabase;
import fi.aalto.cs.apluscourses.ui.ideactivities.GenericHighlighter;
import fi.aalto.cs.apluscourses.ui.ideactivities.OverlayPane;
import org.jetbrains.annotations.NotNull;

public abstract class IntelliJComponentPresenterBase implements ComponentPresenter {

  private final @NotNull String instruction;
  private final @NotNull String info;
  private OverlayPane overlayPane;

  protected IntelliJComponentPresenterBase(@NotNull String instruction, @NotNull String info) {
    this.instruction = instruction;
    this.info = info;
  }

  @Override
  public void highlight() {
    ApplicationManager.getApplication()
        .invokeLater(this::highlightInternal, ModalityState.NON_MODAL);
  }

  @RequiresEdt
  private void highlightInternal() {
    GenericHighlighter highlighter = getHighlighter();
    if (highlighter == null) {
      throw new IllegalStateException("Component was not found!");
    }
    overlayPane = OverlayPane.installOverlay();
    overlayPane.addHighlighter(highlighter);
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
}
