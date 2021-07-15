package fi.aalto.cs.apluscourses.presentation;

import fi.aalto.cs.apluscourses.BannerViewModel;
import fi.aalto.cs.apluscourses.model.Authentication;
import fi.aalto.cs.apluscourses.model.ExercisesTree;
import fi.aalto.cs.apluscourses.presentation.exercise.EmptyExercisesTreeViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExercisesTreeViewModel;
import fi.aalto.cs.apluscourses.presentation.filter.Options;
import fi.aalto.cs.apluscourses.presentation.ideactivities.TutorialViewModel;
import fi.aalto.cs.apluscourses.utils.Event;
import fi.aalto.cs.apluscourses.utils.observable.ObservableProperty;
import fi.aalto.cs.apluscourses.utils.observable.ObservableReadWriteProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainViewModel {

  public static final Logger logger = LoggerFactory.getLogger(MainViewModel.class);

  public final Event disposing = new Event();

  @NotNull
  public final ObservableProperty<CourseViewModel> courseViewModel =
      new ObservableReadWriteProperty<>(null);

  @NotNull
  public final ObservableProperty<ExercisesTreeViewModel> exercisesViewModel =
      new ObservableReadWriteProperty<>(new EmptyExercisesTreeViewModel());

  @NotNull
  public final ProgressViewModel progressViewModel = new ProgressViewModel();

  @NotNull
  public final ObservableProperty<BannerViewModel> bannerViewModel =
      new ObservableReadWriteProperty<>(null);

  @NotNull
  public final ObservableProperty<TutorialViewModel> tutorialViewModel =
      new ObservableReadWriteProperty<>(null);

  @NotNull
  private final Options exerciseFilterOptions;

  /**
   * Instantiates a class representing the whole main view of the plugin.
   */
  public MainViewModel(@NotNull Options exerciseFilterOptions) {
    this.exerciseFilterOptions = exerciseFilterOptions;
  }

  /**
   * Creates a new {@link ExercisesTreeViewModel} with the given exercise groups, which is then set
   * to {@link MainViewModel#exercisesViewModel}.
   */
  public void updateExercisesViewModel(@NotNull ExercisesTree exercisesTree,
                                       Authentication auth) {
    var viewModel = new ExercisesTreeViewModel(exercisesTree, exerciseFilterOptions);
    viewModel.setAuthenticated(auth != null);
    viewModel.setProjectReady(exercisesViewModel.get().isProjectReady());
    exercisesViewModel.set(viewModel);
  }

  public void dispose() {
    disposing.trigger();
  }

  @Nullable
  public ExercisesTreeViewModel getExercises() {
    return exercisesViewModel.get();
  }

  @NotNull
  public Options getExerciseFilterOptions() {
    return exerciseFilterOptions;
  }

  /**
   * Calling this method informs the main view model that the corresponding project has been
   * initialized (by InitializationActivity). If needed, this method notifies the listeners of
   * exercisesViewModel.
   */
  public void setProjectReady(boolean isReady) {
    var viewModel = exercisesViewModel.get();
    if (viewModel == null) {
      return;
    }
    var changed = viewModel.setProjectReady(isReady);
    if (changed) {
      exercisesViewModel.valueChanged();
    }
  }
}
