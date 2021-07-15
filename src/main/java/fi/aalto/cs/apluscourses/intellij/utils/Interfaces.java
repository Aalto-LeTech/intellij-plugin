package fi.aalto.cs.apluscourses.intellij.utils;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import fi.aalto.cs.apluscourses.model.Authentication;
import java.io.File;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Interfaces {
  @FunctionalInterface
  public interface AuthenticationProvider {
    Authentication getAuthentication(@Nullable Project project);
  }

  @FunctionalInterface
  public interface Tagger {
    void putSystemLabel(@Nullable Project project, @NotNull String tag, int color);
  }

  @FunctionalInterface
  public interface DocumentSaver {
    void saveAllDocuments();
  }

  @FunctionalInterface
  public interface LanguageSource {
    @NotNull String getLanguage(@NotNull Project project);
  }

  @FunctionalInterface
  public interface AssistantModeProvider {
    boolean isAssistantMode();
  }

  @FunctionalInterface
  public interface FileRefresher {
    void refreshPath(VirtualFile file, Runnable callback);
  }

  @FunctionalInterface
  public interface FileBrowser {
    void navigateTo(File file, Project project);
  }

  public static class FileRefresherImpl {
    private FileRefresherImpl() {

    }

    /**
     * Refreshes the VFS.
     */
    public static void refreshPath(VirtualFile file, Runnable callback) {
      var refresher = RefreshQueue.getInstance().createSession(true, true, callback);
      if (file != null) {
        refresher.addFile(file);
      }
      refresher.launch();
    }
  }

  public static class FileBrowserImpl {

    /**
     * Opens the file in the editor.
     */
    public static void navigateTo(File file, Project project) {
      var vf = LocalFileSystem.getInstance().findFileByIoFile(file);
      if (vf != null) {
        new OpenFileDescriptor(project, vf).navigate(true);
      }
    }
  }
}
