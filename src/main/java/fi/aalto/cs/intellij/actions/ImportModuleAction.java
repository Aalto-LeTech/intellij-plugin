package fi.aalto.cs.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import fi.aalto.cs.intellij.model.ModuleInstallation;
import fi.aalto.cs.intellij.presentation.CourseModel;
import fi.aalto.cs.intellij.presentation.base.BaseModel;
import fi.aalto.cs.intellij.services.PluginSettings;
import fi.aalto.cs.intellij.utils.SimpleAsyncTaskManager;
import org.jetbrains.annotations.NotNull;

public class ImportModuleAction extends AnAction {

  public static final String ACTION_ID = "fi.aalto.cs.intellij.actions.ImportModuleAction";

  @Override
  public void update(@NotNull AnActionEvent e) {
    CourseModel course = PluginSettings.getInstance().getMainModel().course.get();
    boolean isModuleSelected = course != null
        && !course.getModules().getSelectionModel().isSelectionEmpty();
    e.getPresentation().setEnabled(isModuleSelected);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    CourseModel course = PluginSettings.getInstance().getMainModel().course.get();
    if (course != null) {
      course.getModules().getSelectedElements()
          .stream()
          .map(BaseModel::getModel)
          .forEach(module -> new ModuleInstallation<>(module, course.getModel(),
              new SimpleAsyncTaskManager()).installAsync());
    }
  }
}
