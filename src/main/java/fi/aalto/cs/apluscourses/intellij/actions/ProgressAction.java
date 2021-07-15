package fi.aalto.cs.apluscourses.intellij.actions;

import static fi.aalto.cs.apluscourses.utils.PluginResourceBundle.getAndReplaceText;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.util.ui.JButtonAction;
import fi.aalto.cs.apluscourses.intellij.services.MainViewModelProvider;
import fi.aalto.cs.apluscourses.intellij.services.PluginSettings;
import icons.PluginIcons;
import org.jetbrains.annotations.NotNull;

public class ProgressAction extends JButtonAction {

  @NotNull
  private final MainViewModelProvider mainViewModelProvider;

  public ProgressAction() {
    this(PluginSettings.getInstance());
  }

  public ProgressAction(@NotNull MainViewModelProvider mainViewModelProvider) {
    super("Progress", "Open IDE Activity dialog", PluginIcons.A_PLUS_INFO);
    this.mainViewModelProvider = mainViewModelProvider;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {

  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    if (e.getProject() == null) {
      e.getPresentation().setVisible(false);
      updateButtonFromPresentation(e);
      return;
    }
    var tutorialViewModel =
            mainViewModelProvider.getMainViewModel(e.getProject()).tutorialViewModel.get();
    if (tutorialViewModel == null) {
      e.getPresentation().setVisible(false);
    } else {
      e.getPresentation().setText(getAndReplaceText("presentation.navbar.progress",
              tutorialViewModel.getCurrentTaskIndex(), tutorialViewModel.getTasksAmount()));
    }
    updateButtonFromPresentation(e);
  }

}
