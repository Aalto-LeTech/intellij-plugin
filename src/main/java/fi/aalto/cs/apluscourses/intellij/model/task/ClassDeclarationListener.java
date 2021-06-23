package fi.aalto.cs.apluscourses.intellij.model.task;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorEventMulticaster;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiParameter;
import fi.aalto.cs.apluscourses.model.task.ActivitiesListener;
import fi.aalto.cs.apluscourses.model.task.ListenerCallback;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement;
import org.jetbrains.plugins.scala.lang.psi.api.ScalaRecursiveElementVisitor;
import org.jetbrains.plugins.scala.lang.psi.api.base.ScPrimaryConstructor;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass;
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.templates.ScExtendsBlockImpl;
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.templates.ScTemplateParentsImpl;

public class ClassDeclarationListener implements ActivitiesListener, DocumentListener {

  private final ListenerCallback callback;
  private final Project project;
  private final String className;
  private String[] arguments;
  private String[] hierarchy;
  private Disposable disposable;

  /**
   * Constructor.
   * @param callback The callback for when the task is complete
   * @param project The project where the Tutorial is happening
   * @param className The class name
   * @param arguments The arguments of the class
   */
  public ClassDeclarationListener(ListenerCallback callback,
                                  Project project,
                                  String className,
                                  String[] arguments,
                                  String[] hierarchy,
                                  String fileName) {
    this.callback = callback;
    this.project = project;
    this.className = className;
    this.arguments = arguments;
    this.hierarchy = hierarchy;
    Path modulePath = Paths.get(project.getBasePath() + fileName);
    VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(modulePath.toFile());
    checkPsiFile(PsiManager.getInstance(project).findFile(vf));
  }

  @Override
  public boolean registerListener() {
    EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
    disposable = Disposer.newDisposable();
    multicaster.addDocumentListener(this, disposable);
    //make sure the correct class file is open?
    return false;
  }

  @Override
  public void unregisterListener() {
    EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
    multicaster.removeDocumentListener(this);
    disposable.dispose();
  }

  @Override
  public void documentChanged(@NotNull DocumentEvent event) {
    PsiDocumentManager.getInstance(project).commitDocument(event.getDocument());
    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(event.getDocument());
    checkPsiFile(psiFile);
  }

  private void checkPsiFile(PsiFile psiFile) {
    if (psiFile != null) {
      psiFile.accept(new ScalaRecursiveElementVisitor() {
        @Override
        public void visitScalaElement(ScalaPsiElement element) {
          super.visitScalaElement(element);
          if ((element instanceof ScClass && checkScClass((ScClass) element))) {
            PsiElement[] children = element.getChildren();
            Optional<PsiElement> constructor = Arrays.stream(children).filter(
                ScPrimaryConstructor.class::isInstance).findFirst();
            if (constructor.isPresent()
                    && checkScPrimaryConstructor((ScPrimaryConstructor) constructor.get())) {
              Optional<PsiElement> extendsBlock = Arrays.stream(children).filter(
                  ScExtendsBlockImpl.class::isInstance).findFirst();
              if (extendsBlock.isPresent()
                      && checkExtendsBlock((ScExtendsBlockImpl) extendsBlock.get())) {
                ApplicationManager.getApplication().invokeLater(callback::callback);
              }
            }
          }
        }
      });
    }
  }

  private boolean checkScClass(ScClass element) {
    return className.equals(element.getName());
  }

  private boolean checkScPrimaryConstructor(ScPrimaryConstructor element) {
    Map<String, String> args = new HashMap<>();
    for (String a: arguments) {
      StringTokenizer tokenizer = new StringTokenizer(a, ":", false);
      String key = tokenizer.nextToken();
      String value = tokenizer.nextToken();
      args.put(key, value);
    }
    boolean correct = true;
    PsiParameter[] parameters = element.parameterList().getParameters();
    for (PsiParameter param: parameters) {
      if (!args.containsKey(param.getName())
          || !args.get(param.getName()).equals(param.getType().getPresentableText())) {
        correct = false;
      }
    }
    return correct;
  }

  private boolean checkExtendsBlock(ScExtendsBlockImpl element) {
    List<String> hierarchies = new ArrayList<>();
    PsiElement[] children = element.getChildren();
    Optional<PsiElement> extendsElement = Arrays.stream(children).filter(
        ScTemplateParentsImpl.class::isInstance).findFirst();
    // Peculirarity: If 'extends' is written without specifying a class name
    // it is considered correct by Scala
    // (the keyword extends is not visible when traversing the Psi tree)
    if (extendsElement.isPresent()) {
      if (hierarchy.length != 0) {
        children = extendsElement.get().getChildren();
        Arrays.stream(children).forEach(child -> hierarchies.add(child.getText()));
        return hierarchies.equals(Arrays.asList(hierarchy));
      } else {
        return false;
      }
    } else {
      return !element.getText().startsWith("extends") && hierarchy.length == 0;
    }
  }
}
