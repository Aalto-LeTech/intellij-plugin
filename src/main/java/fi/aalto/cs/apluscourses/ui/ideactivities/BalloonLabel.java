package fi.aalto.cs.apluscourses.ui.ideactivities;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JLabel;

public class BalloonLabel extends JLabel implements TransparentComponent {
  private float transparencyCoefficient;

  /**
   * Constructs a transparent label for the balloon popup.
   *
   * @param text Text of the label.
   */
  public BalloonLabel(String text) {
    super(text);
    transparencyCoefficient = 1.0f;

    setOpaque(false);
  }

  @Override
  public float getTransparencyCoefficient() {
    return transparencyCoefficient;
  }

  @Override
  public void setTransparencyCoefficient(float coefficient) {
    transparencyCoefficient = coefficient;
  }

  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setComposite(AlphaComposite.SrcOver.derive(transparencyCoefficient));

    // let Swing do the text drawing using the customized Graphics object
    super.paintComponent(g2);
    g2.dispose();
  }
}
