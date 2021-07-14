package fi.aalto.cs.apluscourses.ui.ideactivities;

import com.intellij.util.ui.JBUI;
import fi.aalto.cs.apluscourses.ui.GuiObject;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BalloonPopup extends JPanel implements TransparentComponent, MouseListener {
  private final @NotNull Component anchorComponent;

  @GuiObject
  private final JPanel titlePanel;

  @GuiObject
  private final BalloonLabel titleLabel;

  @GuiObject
  private final BalloonLabel messageLabel;

  private final PopupTransparencyHandler transparencyHandler;

  private float transparencyCoefficient;

  private static final int POPUP_MARGIN = 20;

  /**
   * Creates a popup with the given text. The popup is permanently attached to the specified
   * component. Optionally, an icon can be provided which will be displayed to the left of
   * the popup's title.
   */
  public BalloonPopup(@NotNull Component anchorComponent, @NotNull String title,
                      @NotNull String message, @Nullable Icon icon) {
    this.anchorComponent = anchorComponent;
    transparencyHandler = new PopupTransparencyHandler(this);

    addMouseListener(this);

    setOpaque(false);
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(new EmptyBorder(JBUI.insets(0, 5, 10, 5)));

    // introduce a limit to the popup's width (so it doesn't take the entire screen width)
    setMaximumSize(new Dimension(500, 0));

    titleLabel = new BalloonLabel("<html><h1>" + title + "</h1></html>");
    titleLabel.setIcon(icon);

    titlePanel = new JPanel();
    titlePanel.setOpaque(false);
    titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));
    titlePanel.setAlignmentX(LEFT_ALIGNMENT);
    titlePanel.add(titleLabel);
    add(titlePanel);

    messageLabel = new BalloonLabel("<html>" + message + "</html>");
    messageLabel.setAlignmentX(LEFT_ALIGNMENT);
    add(messageLabel);

    setTransparencyCoefficient(0.3f);
    recalculateBounds();
  }

  @Override
  public boolean isVisible() {
    return anchorComponent.isShowing();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    Graphics g2 = g.create();

    var bgColor = getBackground();
    var newColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(),
        (int) (transparencyCoefficient * 255));

    g2.setColor(newColor);
    Rectangle drawBounds = g2.getClipBounds();
    g2.fillRect(drawBounds.x, drawBounds.y, drawBounds.width, drawBounds.height);

    g2.dispose();
  }

  @Override
  public float getTransparencyCoefficient() {
    return transparencyCoefficient;
  }

  /**
   * Sets the popup transparency level.
   *
   * @param coefficient The transparency coefficient - a value between 0.0f and 1.0f.
   *                    0.0f means completely transparent, 1.0f means completely opaque.
   */
  @Override
  public void setTransparencyCoefficient(float coefficient) {
    transparencyCoefficient = coefficient;
    titleLabel.setTransparencyCoefficient(coefficient);
    messageLabel.setTransparencyCoefficient(coefficient);
  }

  /**
   * Recomputes the popups bounds and triggers a reposition if needed. Should ideally be called
   * every time anything changes in the parent frame.
   */
  public void recalculateBounds() {
    // the origin of the component that this popup is attached to must be converted to the
    // overlay pane's coordinate system, because that overlay uses a null layout and requires
    // that this popup specify its bounds
    var componentWindowPos = SwingUtilities.convertPoint(anchorComponent, 0, 0, getParent());

    final var windowSize = JOptionPane.getRootFrame().getSize();
    final var componentSize = anchorComponent.getSize();

    final int popupWidth = titleLabel.getPreferredSize().width + 20;
    final int popupHeight = getMinimumSize().height;

    final int availableSizeLeft = componentWindowPos.x;
    final int availableSizeRight = windowSize.width - (componentWindowPos.x + componentSize.width);
    final int availableSizeTop = componentWindowPos.y;
    final int availableSizeBottom = windowSize.height - (componentWindowPos.y + componentSize.height);

    final int mostHorizontalSpace = Integer.max(availableSizeLeft, availableSizeRight);
    final int mostVerticalSpace = Integer.max(availableSizeTop, availableSizeBottom);

    int popupX;
    int popupY;

    if (mostHorizontalSpace < popupWidth + 2 * POPUP_MARGIN && mostVerticalSpace < popupHeight + 2 * POPUP_MARGIN) {
      popupX = componentWindowPos.x + componentSize.width - popupWidth - POPUP_MARGIN;
      popupY = componentWindowPos.y + POPUP_MARGIN;

      final var popupBounds = new Rectangle(0, 0, popupWidth, popupHeight);
      final var mousePos = getMousePosition();

      transparencyHandler.update(mousePos != null && popupBounds.contains(mousePos));
      if (transparencyHandler.isInAnimation()) {
        revalidate();
        repaint();
      }
    } else {
      if (mostHorizontalSpace > mostVerticalSpace) {
        if (availableSizeRight > availableSizeLeft) {
          popupX = componentWindowPos.x + anchorComponent.getWidth() + 5;
        } else {
          popupX = componentWindowPos.x - popupWidth - 5;
        }

        popupY = componentWindowPos.y + (anchorComponent.getHeight() - popupHeight) / 2;
      } else {
        if (availableSizeBottom > availableSizeTop) {
          popupY = componentWindowPos.y + anchorComponent.getHeight() + 5;
        } else {
          popupY = componentWindowPos.y - popupHeight - 5;
        }

        popupX = componentWindowPos.x + (anchorComponent.getWidth() - popupWidth) / 2;
      }

      setTransparencyCoefficient(1.0f);
    }

    setBounds(popupX, popupY, popupWidth, popupHeight);
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    // not used
  }

  @Override
  public void mousePressed(MouseEvent e) {
    // not used
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    // not used
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    transparencyHandler.resetAnimationProgress();
    transparencyHandler.update(true);

    revalidate();
    repaint();
  }

  @Override
  public void mouseExited(MouseEvent e) {
    transparencyHandler.resetAnimationProgress();
    transparencyHandler.update(false);

    revalidate();
    repaint();
  }
}
