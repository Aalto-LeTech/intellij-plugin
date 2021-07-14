package fi.aalto.cs.apluscourses.ui.ideactivities;

import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.geom.RectangularShape;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class EditorHighlighter extends GenericHighlighter {
  private final @NotNull EditorImpl editor;

  private final List<Integer> highlightedLines = new ArrayList<>();

  private boolean highlightEverything() {
    return highlightedLines.isEmpty();
  }

  /**
   * Adds particular lines in the editor to the highlight list. The lines are indexed from 1, not 0.
   */
  public void highlightLines(int... lineNumbers) {
    for (int line : lineNumbers) {
      highlightedLines.add(line - 1);
    }
  }

  /**
   * Adds a continuous range of lines in the editor to the highlight list.
   * The lines are indexed from 1, not 0.
   *
   * @param lineBegin The beginning of the intervals of lines, inclusive.
   * @param lineEnd The ending of the intervals of lines, inclusive.
   */
  public void highlightLineRange(int lineBegin, int lineEnd) {
    for (int i = lineBegin; i <= lineEnd; ++i) {
      highlightLines(i);
    }
  }

  @Override
  public @NotNull Component getComponent() {
    return highlightEverything() ? super.getComponent().getParent().getParent() : super.getComponent();
  }

  @Override
  public List<RectangularShape> getArea() {
    if (highlightEverything()) {
      return super.getArea();
    }

    var lineHeight = editor.getLineHeight();
    var rectangles = new ArrayList<RectangularShape>();

    // the parent of the editor is JBViewport, which controls the visible region of the component
    // and the parent of JBViewport is another component that includes the line numbers
    var editorWidth = getComponent().getParent().getParent().getWidth();

    for (int line : highlightedLines) {
      var startPos = new LogicalPosition(line, 0);
      var startPoint = editor.logicalPositionToXY(startPos);
      rectangles.add(new Rectangle(-getComponent().getX(), startPoint.y, editorWidth, lineHeight));
    }

    return rectangles;
  }

  /**
   * A.
   */
  public EditorHighlighter(@NotNull EditorComponentImpl component) {
    super(component);
    editor = component.getEditor();
  }
}
