package fi.aalto.cs.apluscourses.intellij.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.SyntaxTraverser;
import org.jetbrains.annotations.NotNull;

public class PsiUtil {
  private PsiUtil() {

  }

  public static boolean psiSiblingHasErrors(@NotNull PsiElement element) {
    return psiElementHasErrors(element.getPrevSibling()) || psiElementHasErrors(element.getNextSibling());
  }

  public static boolean psiElementHasErrors(@NotNull PsiElement element) {
    return !SyntaxTraverser.psiTraverser(element).traverse().filter(PsiErrorElement.class).isEmpty();
  }
}
