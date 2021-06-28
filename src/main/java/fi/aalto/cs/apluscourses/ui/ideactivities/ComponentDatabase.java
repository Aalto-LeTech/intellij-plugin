package fi.aalto.cs.apluscourses.ui.ideactivities;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.wm.ToolWindowManager;
import java.awt.Component;
import java.nio.file.Paths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ComponentDatabase {
  public static @Nullable Component getProjectPane() {
    Component component = ComponentLocator.getComponentByClass("ProjectViewPane");
    return component == null ? null : component.getParent();
  }

  public static @Nullable Component getEditorWindow() {
    return ComponentLocator.getComponentByClass("EditorWindow");
  }

  /**
   * Opens a file in the editor, return true if successful.
   */
  public static boolean showFile(@NotNull String path, @NotNull Project project) {
    var modulePath = Paths.get(project.getBasePath() + path);
    var vf = LocalFileSystem.getInstance().findFileByIoFile(modulePath.toFile());
    if (vf == null) {
      return false;
    }
    new OpenFileDescriptor(project, vf).navigate(true);
    return true;
  }

  public static boolean showProjectToolWindow(@NotNull Project project) {
    return showToolWindow("Project", project);
  }

  public static boolean showAPlusToolWindow(@NotNull Project project) {
    return showToolWindow("A+ Courses", project);
  }

  private static boolean showToolWindow(@NotNull String id, @NotNull Project project) {
    var toolWindow = ToolWindowManager.getInstance(project).getToolWindow(id);
    if (toolWindow != null) {
      try {
        toolWindow.activate(null);
        return true;
      } catch (IllegalStateException e) {
        return false;
      }
    }
    return false;
  }

  private ComponentDatabase() {

  }
}
