package fi.aalto.cs.apluscourses.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareToggleAction;
import fi.aalto.cs.apluscourses.intellij.services.PluginSettings;
import org.jetbrains.annotations.NotNull;

public class AssistantModeAction extends DumbAwareToggleAction {
  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    return PluginSettings.getInstance().isAssistantMode();
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    PluginSettings.getInstance().setAssistantMode(state);
  }
}
