package fi.aalto.cs.apluscourses.intellij.actions;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import fi.aalto.cs.apluscourses.intellij.services.MainViewModelProvider;
import fi.aalto.cs.apluscourses.model.ExercisesTree;
import fi.aalto.cs.apluscourses.presentation.MainViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExercisesTreeViewModel;
import fi.aalto.cs.apluscourses.presentation.filter.Option;
import fi.aalto.cs.apluscourses.presentation.filter.Options;
import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class FilterOptionsActionGroupTest {

  AnActionEvent event;
  FilterOptionsActionGroup actionGroup;
  Option filterOption1;
  Option filterOption2;

  /**
   * Called before each test.
   */
  @Before
  public void setUp() {
    Project project = mock(Project.class);
    event = mock(AnActionEvent.class);
    when(event.getProject()).thenReturn(project);

    filterOption1 = new Option("Just some filter", null, item -> Optional.of(true)).init();
    filterOption2 = new Option("Another filter", null, item -> Optional.of(false)).init();
    Options filterOptions = new Options(filterOption1, filterOption2);

    MainViewModel mainViewModel = new MainViewModel(new Options());
    ExercisesTreeViewModel exercisesViewModel =
            spy(new ExercisesTreeViewModel(new ExercisesTree(), new Options()));
    when(exercisesViewModel.getFilterOptions()).thenReturn(filterOptions);
    mainViewModel.exercisesViewModel.set(exercisesViewModel);
    MainViewModelProvider mainViewModelProvider = mock(MainViewModelProvider.class);
    when(mainViewModelProvider.getMainViewModel(any())).thenReturn(mainViewModel);

    actionGroup = new FilterOptionsActionGroup(mainViewModelProvider);
  }

  @Test
  public void testGetFilterOptionActions() {
    actionGroup.getChildren(event);
    FilterOptionAction[] actions = actionGroup.getFilterOptionActions();

    assertSame(filterOption1, actions[0].getOption());
    assertSame(filterOption2, actions[1].getOption());
  }

  @Test
  public void testGetChildren() {
    AnAction[] children = actionGroup.getChildren(event);

    assertTrue(children[0] instanceof FilterOptionAction);
    assertTrue(children[1] instanceof FilterOptionAction);
    assertTrue(children[2] instanceof Separator);
    assertTrue(children[3] instanceof SelectAllFiltersAction);
  }

}
