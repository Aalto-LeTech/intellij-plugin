package fi.aalto.cs.apluscourses.presentation.ideactivities;

import org.jetbrains.annotations.NotNull;

public interface TutorialDialogs {

  boolean confirmStart(@NotNull TutorialViewModel tutorialViewModel);

  boolean confirmCancel(@NotNull TutorialViewModel tutorialViewModel);

  void end(@NotNull TutorialViewModel tutorialViewModel);
}
