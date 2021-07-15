package fi.aalto.cs.apluscourses.presentation.exercise;

import fi.aalto.cs.apluscourses.model.ExercisesTree;
import fi.aalto.cs.apluscourses.presentation.base.BaseTreeViewModel;
import fi.aalto.cs.apluscourses.presentation.base.Searchable;
import fi.aalto.cs.apluscourses.presentation.base.SelectableNodeViewModel;
import fi.aalto.cs.apluscourses.presentation.filter.Options;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExercisesTreeViewModel extends BaseTreeViewModel<ExercisesTree>
        implements Searchable {

  private boolean isAuthenticated;

  private final AtomicBoolean isProjectReady = new AtomicBoolean(false);

  /**
   * Construct an exercises tree view model from the given exercise groups.
   */
  public ExercisesTreeViewModel(@NotNull ExercisesTree exercisesTree,
                                @NotNull Options filterOptions) {
    super(exercisesTree,
        exercisesTree.getExerciseGroups()
            .stream()
            .map(ExerciseGroupViewModel::new)
            .collect(Collectors.toList()),
        filterOptions);
  }

  public boolean isEmptyTextVisible() {
    return false;
  }

  public boolean isAuthenticated() {
    return isAuthenticated;
  }

  public void setAuthenticated(boolean authenticated) {
    isAuthenticated = authenticated;
  }

  public boolean isProjectReady() {
    return isProjectReady.get();
  }

  /**
   * Returns true if the value changed, false if the value was already equal to the given value.
   */
  public boolean setProjectReady(boolean projectReady) {
    return isProjectReady.getAndSet(projectReady) != projectReady;
  }

  public String getName() {
    var student = getModel().getSelectedStudent();
    return student == null ? null : student.getFullName();
  }

  @Override
  @NotNull
  public Selection findSelected() {
    return new ExerciseTreeSelection(traverseAndFind(SelectableNodeViewModel::isSelected));
  }

  public static class ExerciseTreeSelection extends Selection {
    public ExerciseTreeSelection(@Nullable List<SelectableNodeViewModel<?>> pathToSelected) {
      super(pathToSelected);
    }

    @Nullable
    public ExerciseGroupViewModel getExerciseGroup() {
      var selection = getLevel(1);
      return selection instanceof ExerciseGroupViewModel ? (ExerciseGroupViewModel) selection : null;
    }

    @Nullable
    public ExerciseViewModel getExercise() {
      var selection = getLevel(2);
      return selection instanceof ExerciseViewModel ? (ExerciseViewModel) selection : null;
    }
  }
}
