package fi.aalto.cs.apluscourses.presentation.exercise;

import fi.aalto.cs.apluscourses.model.ExercisesTree;
import fi.aalto.cs.apluscourses.presentation.filter.Options;


public class EmptyExercisesTreeViewModel extends ExercisesTreeViewModel {

  /**
   * Construct an exercises tree view model from the given exercise groups.
   */
  public EmptyExercisesTreeViewModel() {
    super(new ExercisesTree(), new Options());
  }

  @Override
  public boolean isEmptyTextVisible() {
    return true;
  }
}
