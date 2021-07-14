package fi.aalto.cs.apluscourses.ui.ideactivities;

public class PopupTransparencyHandler {

  private final TransparentComponent component;

  private long lastTransitionUpdate;

  private static final float MIN_TRANSPARENCY = 0.55f;
  private static final float MAX_TRANSPARENCY = 1.0f;
  private static final float TRANSITION_STEP = 0.0015f;

  public PopupTransparencyHandler(TransparentComponent component) {
    this.component = component;
  }

  public boolean isInAnimation() {
    var coefficient = component.getTransparencyCoefficient();
    return !(coefficient == MIN_TRANSPARENCY || coefficient == MAX_TRANSPARENCY);
  }

  public void resetAnimationProgress() {
    lastTransitionUpdate = 0;
  }

  /**
   * Updates the status of transparency fading animation.
   *
   * @param isMouseOnPopup If true, the mouse cursor is currently on the popup.
   */
  public boolean update(boolean isMouseOnPopup) {
    if (lastTransitionUpdate == 0) {
      lastTransitionUpdate = System.currentTimeMillis();
      return true;
    }

    float transitionStep = isMouseOnPopup ? TRANSITION_STEP : -TRANSITION_STEP;

    long timeSinceLastUpdate = System.currentTimeMillis() - lastTransitionUpdate;
    float currentCoefficient = component.getTransparencyCoefficient();

    boolean shouldRedraw = true;

    currentCoefficient += transitionStep * timeSinceLastUpdate;

    if (currentCoefficient < MIN_TRANSPARENCY) {
      currentCoefficient = MIN_TRANSPARENCY;
      resetAnimationProgress();

      shouldRedraw = false;
    }

    if (currentCoefficient > MAX_TRANSPARENCY) {
      currentCoefficient = MAX_TRANSPARENCY;
      resetAnimationProgress();

      shouldRedraw = false;
    }

    component.setTransparencyCoefficient(currentCoefficient);
    lastTransitionUpdate = System.currentTimeMillis();

    return shouldRedraw;
  }
}
