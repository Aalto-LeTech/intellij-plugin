package fi.aalto.cs.apluscourses.intellij.model.task;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.PsiErrorElementUtil;
import fi.aalto.cs.apluscourses.model.task.Arguments;
import fi.aalto.cs.apluscourses.model.task.ListenerCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ErrorListener extends ScalaCodeListener {

  public ErrorListener(@NotNull ListenerCallback callback,
                       @NotNull Project project,
                       @NotNull String fileName) {
    super(callback, project, fileName);
  }

  /**
   * Creates an instance of ErrorListener based on the provided arguments.
   */
  public static ErrorListener create(ListenerCallback callback,
                                     Project project, Arguments arguments) {
    return new ErrorListener(
        callback, project,
        arguments.getOrThrow("filePath")
    );
  }


  @Override
  protected void checkPsiFile(@Nullable PsiFile file) {
    if (file == null) {
      return;
    }

    if (!PsiErrorElementUtil.hasErrors(project, file.getVirtualFile())) {
      callback.callback();
      isCorrect.set(true);
    }
  }

}
