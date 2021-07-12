package fi.aalto.cs.apluscourses.ui.ideactivities;

import com.intellij.execution.DefaultExecutionTarget;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunManager;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import java.awt.Component;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ComponentDatabase {
  public static final String PROJECT_TOOL_WINDOW = "Project";
  public static final String APLUS_TOOL_WINDOW = "A+ Courses";
  public static final String RUN_TOOL_WINDOW = "Run";

  public static @Nullable Component getProjectPane() {
    Component component = ComponentLocator.getComponentByClass("ProjectViewPane");
    return component == null ? null : component.getParent();
  }

  public static @Nullable ToolWindow getToolWindow(@NotNull String id, @NotNull Project project) {
    return ToolWindowManager.getInstance(project).getToolWindow(id);
  }

  /**
   * Returns any of the open editors - it is not deterministic which one will be returned.
   */
  public static @Nullable EditorComponentImpl getEditorWindow() {
    return (EditorComponentImpl) ComponentLocator.getComponentByClass("EditorComponentImpl");
  }

  /**
   * Out of all open editors, retrieves the one with a specific file open.
   */
  public static @Nullable EditorComponentImpl getEditorWindow(String fileNameSubstring) {
    var editors = ComponentLocator.getComponentsByClass("EditorComponentImpl");
    for (var editorComponent : editors) {
      var editor = (EditorComponentImpl) editorComponent;
      if (editor.getEditor().getVirtualFile().getName().contains(fileNameSubstring)) {
        return editor;
      }
    }
    return null;
  }

  public static @Nullable Component getProgressButton() {
    return ComponentLocator.getComponentByClass("TutorialProgressAction");
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

  /**
   * Opens a tool window.
   * @param id      The name of the tool window
   * @param project The project
   */
  public static boolean showToolWindow(@NotNull String id, @NotNull Project project) {
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

  /**
   * Opens the Run tool window and starts the REPL for a given module.
   */
  public static boolean startAndShowRepl(@NotNull Module module, @NotNull Project project) {
    var toolWindow = ToolWindowManager.getInstance(project).getToolWindow(RUN_TOOL_WINDOW);
    if (toolWindow == null) {
      return false;
    }
    showToolWindow(RUN_TOOL_WINDOW, project);

    if (Arrays.stream(toolWindow.getContentManager().getContents()).anyMatch(content ->
        content.toString().contains("REPL") && content.toString().contains(module.getName()))) {
      return true;
    }

    var runConfigs = RunManager.getInstance(project).getAllConfigurationsList();
    var opened = false;
    for (var config : runConfigs) {
      var name = config.getName();
      if (name.contains("REPL") && name.contains(module.getName())) {

        var settings = RunManager.getInstance(project).findSettings(config);
        var context = DataManager.getInstance().getDataContext(toolWindow.getComponent());
        var runContentDescriptor = LangDataKeys.RUN_CONTENT_DESCRIPTOR.getData(context);
        if (runContentDescriptor != null) {
          ExecutionManager.getInstance(project).restartRunProfile(project,
              DefaultRunExecutor.getRunExecutorInstance(),
              DefaultExecutionTarget.INSTANCE,
              settings,
              runContentDescriptor.getProcessHandler());
          opened = true;
          break;
        }
      }
    }
    return opened;
  }

  /**
   * Closes all REPLs for a project.
   */
  public static void closeRepls(@NotNull Project project) {
    var descriptors = ExecutionManagerImpl.getAllDescriptors(project);
    for (var descriptor : descriptors) {
      var processHandler = descriptor.getProcessHandler();
      if (processHandler != null && descriptor.getDisplayName().contains("REPL")) {
        processHandler.destroyProcess();
        var started = new Date();
        while (!processHandler.isProcessTerminated()) {
          var now = new Date();
          if (now.getTime() - started.getTime() > 500) {
            break;
          }
        }
      }
    }

    var toolWindow = getToolWindow(RUN_TOOL_WINDOW, project);
    if (toolWindow != null) {
      Arrays.stream(toolWindow.getContentManager().getContents()).forEach(content -> {
        if (content.toString().contains("REPL")) {
          toolWindow.getContentManager().removeContent(content, true);
        }
      });
    }
  }

  /**
   * Closes file in the editor for a given project.
   */
  public static void closeFile(@NotNull String path, @NotNull Project project) {
    var modulePath = Paths.get(project.getBasePath() + path);
    var vf = LocalFileSystem.getInstance().findFileByIoFile(modulePath.toFile());
    if (vf == null) {
      return;
    }
    FileEditorManager.getInstance(project).closeFile(vf);
  }

  /**
   * Hides a tool window.
   * @param id      The name of the tool window
   * @param project The project
   */
  public static boolean hideToolWindow(@NotNull String id, @NotNull Project project) {
    var toolWindow = ToolWindowManager.getInstance(project).getToolWindow(id);
    if (toolWindow != null) {
      try {
        toolWindow.hide();
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
