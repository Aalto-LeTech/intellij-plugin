package fi.aalto.cs.apluscourses.intellij.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiParameter;
import fi.aalto.cs.apluscourses.utils.IdeActivitiesUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.plugins.scala.lang.psi.api.base.ScAnnotations;
import org.jetbrains.plugins.scala.lang.psi.api.base.ScPrimaryConstructor;
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScSimpleTypeElement;
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScTypeParamClause;
import org.jetbrains.plugins.scala.lang.psi.impl.base.ScModifierListImpl;
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.templates.ScTemplateParentsImpl;

public class ScalaClassDeclaration {

  private final String className;
  private final String[] arguments;
  private final String hierarchy;
  private final String[] traits;
  private final List<String> parameterModifiers;
  private final List<String> parameterAnnotations;
  private final String[] typeParameters;

  /**
   * Constructor.
   */
  public ScalaClassDeclaration(String className, String[] arguments,
                               String hierarchy, String[] traitHierarchy, String[] typeParameters,
                               String[] parameterModifiers, String[] parameterAnnotations) {
    this.className = className;
    this.arguments = arguments;
    this.parameterModifiers = Arrays.asList(parameterModifiers);
    this.parameterAnnotations = Arrays.asList(parameterAnnotations);
    this.hierarchy = hierarchy;
    this.traits = traitHierarchy;
    this.typeParameters = typeParameters;
  }

  public boolean checkClassName(String className) {
    return this.className.equals(className);
  }

  /**
   * Checks if the class' constructor is correct.
   * @param element The ScPrimaryConstructor to be checked.
   * @return whether the constructor is in the desired state.
   */
  public boolean checkConstructor(Optional<PsiElement> element) {
    if (element.isPresent()) {
      ScPrimaryConstructor constructor = (ScPrimaryConstructor) element.get();
      PsiParameter[] parameters = constructor.parameterList().getParameters();
      List<String> params = new ArrayList<>();
      for (PsiParameter param: parameters) {
        if (!checkParameterModifiers(param, params.size())
            || !checkParameterAnnotationsList(param, params.size())) {
          return false;
        }
        if (param.getText().contains("val ")) {
          params.add("val " + param.getName() + ":" + param.getType().getPresentableText());
        } else if (param.getText().contains("var ")) {
          params.add("var " + param.getName() + ":" + param.getType().getPresentableText());
        } else {
          params.add(param.getName() + ":" + param.getType().getPresentableText());
        }
      }
      return Arrays.equals(arguments, params.toArray());
    }
    return  false;
  }

  /**
   * Checks the type parameters of the function.
   * Certain characters are removed. In the case of type parameters, some text from the PSI
   * comes in the form of [A, B] and so the brackets need to be removed.
   * @param clause The ScTypeParamClause to be checked
   * @return true if the type parameters are correct, false otherwise.
   */
  public boolean checkTypeParameters(Optional<PsiElement> clause) {
    if (clause.isPresent() && typeParameters.length != 0) {
      ScTypeParamClause typeParamClause = (ScTypeParamClause) clause.get();
      return Arrays.equals(typeParameters, IdeActivitiesUtil.getSeparateWords(
          typeParamClause.getText().replace("[", "").replace("]",""), ","));
    } else {
      return clause.isEmpty() && typeParameters.length == 0;
    }
  }

  /**
   * Checks the hierachy of the class.
   * @param element The ScTemplateParentsImpl to be checked
   * @return true if the hierarchy is correct, false otherwise.
   */
  public boolean checkExtendsBlock(Optional<PsiElement> element) {
    if (element.isPresent()) {
      Set<String> hierarchies = new HashSet<>();
      PsiElement[] children = element.get().getChildren();
      Optional<PsiElement> extendsElement = Arrays.stream(children).filter(
          ScTemplateParentsImpl.class::isInstance).findFirst();
      // Peculiarity: If 'extends' is written without specifying a class name
      // it is considered correct by Scala
      // (the keyword extends is not visible when traversing the Psi tree)
      if (extendsElement.isPresent()) {
        if (hierarchy != null && !hierarchy.isEmpty()) {
          //traits may not be present!
          children = extendsElement.get().getChildren();
          if (children.length > 0 && hierarchy.equals(children[0].getText())) {
            Arrays.stream(children).filter(
                ScSimpleTypeElement.class::isInstance).forEach(
                    child -> hierarchies.add(child.getText()));
            return traits.length == 0 || hierarchies.equals(new HashSet<>(Arrays.asList(traits)));
          }
          //The order does not matter?!!
          //The first one should be present first - ScConstructorInvocationImpl -> class hierarchy
          // The with order does not matter ScSimpleTypeElementImpl
          // -> trait hierarchy compare Sets since the order does not matter?
        } else {
          return false;
        }
      } else {
        return !element.get().getText().startsWith("extends")
            && hierarchy.isEmpty() && traits.length == 0;
      }
    }
    return false;
  }

  /**
   * Checks the modifiers (sealed, implicit, etc.) of the parameters.
   * @param element The ScModifierListImpl to be checked
   * @param index the index indicating which parameter
   *              (in parameterModifiers) these modifiers refer to
   * @return true if the modifiers are correct, false otherwise.
   */
  public boolean checkParameterModifiers(PsiParameter element, int index) {
    PsiElement[] children = element.getChildren();
    Optional<PsiElement> modifiersElement = Arrays.stream(children).filter(
        ScModifierListImpl.class::isInstance).findFirst();
    if (modifiersElement.isPresent()) {
      ScModifierListImpl modifierList = (ScModifierListImpl) modifiersElement.get();
      String[] psiModifiers = IdeActivitiesUtil.getSeparateWords(
          modifierList.getText(), " ");
      String[] configModifiers =
          IdeActivitiesUtil.getSeparateWords(parameterModifiers.get(index), " ");
      return Arrays.equals(psiModifiers, configModifiers);
    }
    return false;
  }

  /**
   * Checks the annotations of the parameters.
   * @param element The ScAnnotations to be checked
   * @param index the index indicating which parameter
   *              (in parameterAnnotations) these modifiers refer to
   * @return true if the annotations are correct, false otherwise.
   */
  public boolean checkParameterAnnotationsList(PsiParameter element, int index) {
    PsiElement[] children = element.getChildren();
    Optional<PsiElement> annotationsElement = Arrays.stream(children).filter(
        ScAnnotations.class::isInstance).findFirst();
    if (annotationsElement.isPresent()) {
      ScAnnotations annotationList = (ScAnnotations) annotationsElement.get();
      String[] psiAnnotations =
          IdeActivitiesUtil.getSeparateWords(annotationList.getText(), " ");
      String[] configAnnotations =
          IdeActivitiesUtil.getSeparateWords(parameterAnnotations.get(index), " ");
      return Arrays.equals(psiAnnotations, configAnnotations);
    }
    return false;
  }

  public String[] getArguments() {
    return arguments;
  }

}
