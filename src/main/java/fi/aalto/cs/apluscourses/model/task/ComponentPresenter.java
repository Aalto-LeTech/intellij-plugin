package fi.aalto.cs.apluscourses.model.task;

public interface ComponentPresenter {

  void highlight();

  void removeHighlight();

  boolean isVisible();

  void setCancelHandler(CancelHandler cancelHandler);
}
