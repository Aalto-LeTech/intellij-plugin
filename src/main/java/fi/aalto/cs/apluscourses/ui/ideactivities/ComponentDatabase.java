package fi.aalto.cs.apluscourses.ui.ideactivities;

import com.intellij.openapi.editor.impl.EditorComponentImpl;
import java.awt.Component;
import org.jetbrains.annotations.Nullable;

public class ComponentDatabase {
  public static @Nullable Component getProjectPane() {
    Component component = ComponentLocator.getComponentByClass("ProjectViewPane");
    return component == null ? null : component.getParent();
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

  private ComponentDatabase() {

  }
}
