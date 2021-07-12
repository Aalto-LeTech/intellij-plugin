package fi.aalto.cs.apluscourses.intellij.model.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.messages.MessageBusConnection;
import fi.aalto.cs.apluscourses.intellij.psi.ScalaClassDeclaration;
import fi.aalto.cs.apluscourses.model.task.ActivitiesListener;
import fi.aalto.cs.apluscourses.model.task.ListenerCallback;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement;
import org.jetbrains.plugins.scala.lang.psi.api.ScalaRecursiveElementVisitor;
import org.jetbrains.plugins.scala.lang.psi.api.base.ScPrimaryConstructor;
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScTypeParamClause;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass;
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.templates.ScExtendsBlockImpl;

public class ClassDeclarationListener implements ActivitiesListener, BulkFileListener {

  private final ListenerCallback callback;
  private final Project project;
  private final String fileName;
  private final ScalaClassDeclaration modelScalaClass;
  private MessageBusConnection messageBusConnection;
  private final AtomicBoolean isCorrect = new AtomicBoolean(false);

  /**
   * Constructor.
   */
  public ClassDeclarationListener(ListenerCallback callback,
                                  Project project,
                                  String className,
                                  String[] arguments,
                                  String hierarchy,
                                  String[] traitHierarchy,
                                  String[] typeParameters,
                                  String[] parameterModifiers,
                                  String[] parameterAnnotations,
                                  String fileName) {
    this.callback = callback;
    this.project = project;
    this.modelScalaClass = new ScalaClassDeclaration(className,arguments, hierarchy,
        traitHierarchy, typeParameters, parameterModifiers, parameterAnnotations);
    this.fileName = fileName;
  }


  @Override
  public void after(@NotNull List<? extends VFileEvent> events) {
    events.forEach(event -> {
      if (event instanceof VFileContentChangeEvent
          && ((VFileContentChangeEvent) event).getFile().getPath().equals(
              project.getBasePath() + fileName)) {
        checkPsiFile(PsiManager.getInstance(project).findFile(
            ((VFileContentChangeEvent) event).getFile()));
      }
    });
  }

  /**
   * Checks the file when the listener is first registered.
   */
  public boolean checkFile() {
    Path modulePath = Paths.get(project.getBasePath() + fileName);
    VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(modulePath.toFile());
    if (vf != null) {
      checkPsiFile(PsiManager.getInstance(project).findFile(vf));
    }
    return isCorrect.get();
  }

  @Override
  public boolean registerListener() {
    messageBusConnection = project.getMessageBus().connect();
    messageBusConnection.subscribe(VirtualFileManager.VFS_CHANGES, this);
    return checkFile();
  }

  @Override
  public void unregisterListener() {
    if (messageBusConnection != null) {
      messageBusConnection.disconnect();
      messageBusConnection = null;
    }
  }

  private void checkPsiFile(@Nullable PsiFile psiFile) {
    if (psiFile == null) {
      return;
    }
    psiFile.accept(new ScalaRecursiveElementVisitor() {
      @Override
      public void visitScalaElement(ScalaPsiElement element) {
        super.visitScalaElement(element);
        if (element instanceof ScClass
            && modelScalaClass.checkClassName(((ScClass) element).getName())) {
          PsiElement[] children = element.getChildren();
          Optional<PsiElement> typeParameters = Arrays.stream(children).filter(
              ScTypeParamClause.class::isInstance).findFirst();
          Optional<PsiElement> constructor = Arrays.stream(children).filter(
              ScPrimaryConstructor.class::isInstance).findFirst();
          if (modelScalaClass.checkConstructor(constructor)
              && modelScalaClass.checkTypeParameters(typeParameters)) {
            Optional<PsiElement> extendsBlock = Arrays.stream(children).filter(
                ScExtendsBlockImpl.class::isInstance).findFirst();
            if (modelScalaClass.checkExtendsBlock(extendsBlock)) {
              ApplicationManager.getApplication().invokeLater(callback::callback);
              isCorrect.set(true);
            }
          }
        }
      }
    });
  }

}
