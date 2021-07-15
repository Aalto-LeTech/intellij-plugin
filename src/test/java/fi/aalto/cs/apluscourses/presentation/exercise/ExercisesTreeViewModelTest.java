package fi.aalto.cs.apluscourses.presentation.exercise;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import fi.aalto.cs.apluscourses.model.ExercisesTree;
import fi.aalto.cs.apluscourses.presentation.filter.Options;
import org.junit.Test;

public class ExercisesTreeViewModelTest {
  @Test
  public void testIsEmptyTextVisible() {
    assertTrue(new EmptyExercisesTreeViewModel().isEmptyTextVisible());
    assertFalse(new ExercisesTreeViewModel(new ExercisesTree(), new Options())
        .isEmptyTextVisible());
  }
}
