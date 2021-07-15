package fi.aalto.cs.apluscourses.intellij.actions;

import static fi.aalto.cs.apluscourses.utils.PluginResourceBundle.getAndReplaceText;
import static fi.aalto.cs.apluscourses.utils.PluginResourceBundle.getText;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.project.DumbAware;
import fi.aalto.cs.apluscourses.intellij.services.CourseProjectProvider;
import fi.aalto.cs.apluscourses.intellij.services.PluginSettings;
import icons.PluginIcons;
import org.jetbrains.annotations.NotNull;

public class UserActionGroup extends DefaultActionGroup implements DumbAware {
  private final CourseProjectProvider courseProjectProvider;

  public UserActionGroup() {
    this(PluginSettings.getInstance()::getCourseProject);
  }

  public UserActionGroup(CourseProjectProvider courseProjectProvider) {
    this.courseProjectProvider = courseProjectProvider;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().putClientProperty(ActionButton.HIDE_DROPDOWN_ICON, Boolean.TRUE);
    var project = courseProjectProvider.getCourseProject(e.getProject());
    e.getPresentation().setVisible(project != null);
    if (project != null) {
      var userName = project.getUserName();
      var loggedIn = !userName.equals("");
      var icon = loggedIn ? PluginIcons.A_PLUS_USER_ACTIVE : PluginIcons.A_PLUS_USER;
      var text = loggedIn
              ? getAndReplaceText("presentation.userDropdown.loggedInAs", userName)
              : getText("presentation.userDropdown.notLoggedIn");
      e.getPresentation().setIcon(icon);
      e.getPresentation().setText(text);
    }
  }
}
