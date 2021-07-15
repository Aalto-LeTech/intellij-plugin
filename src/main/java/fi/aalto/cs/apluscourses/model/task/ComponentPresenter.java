package fi.aalto.cs.apluscourses.model.task;

public interface ComponentPresenter {

  void highlight();

  void removeHighlight();

  void setCancelHandler(CancelHandler cancelHandler);
}
