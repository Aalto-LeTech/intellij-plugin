package fi.aalto.cs.apluscourses.intellij.model.task;

import com.intellij.openapi.project.Project;
import fi.aalto.cs.apluscourses.model.task.ActivitiesListener;
import fi.aalto.cs.apluscourses.model.task.ActivityFactory;
import fi.aalto.cs.apluscourses.model.task.Arguments;
import fi.aalto.cs.apluscourses.model.task.ComponentPresenter;
import fi.aalto.cs.apluscourses.model.task.ListenerCallback;
import fi.aalto.cs.apluscourses.ui.ideactivities.ComponentDatabase;
import org.jetbrains.annotations.NotNull;

public class IntelliJActivityFactory implements ActivityFactory {
  private final @NotNull Project project;

  private static final String FILE_PATH = "filePath";

  public IntelliJActivityFactory(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public @NotNull ActivitiesListener createListener(@NotNull String action,
                                                    @NotNull Arguments arguments,
                                                    @NotNull ListenerCallback callback) {
    switch (action) {
      case "openEditor":
        return new OpenFileListener(callback, arguments.getOrThrow(FILE_PATH), project);
      case "build":
        return new IdeActionListener(callback, project, arguments.getOrThrow("actionName"), null);
      case "test":
        return new IdeActionListener(callback, project,
            arguments.getOrThrow("actionName"), arguments.getOrThrow(FILE_PATH));
      case "classDeclScala":
        return new ClassDeclarationListener(callback, project,
            arguments.getOrThrow("className"),
            arguments.getArrayOrThrow("classArguments"),
            arguments.getOrThrow("classHierarchy"),
            arguments.getArrayOrThrow("traitHierarchy"),
            arguments.getArrayOrThrow("typeParamClause"),
            arguments.getArrayOrThrow("modifiers"),
            arguments.getArrayOrThrow("annotations"),
            arguments.getOrThrow(FILE_PATH));
      case "functionDefinition":
        return new FunctionDefinitionListener(callback, project,
            arguments.getOrThrow("methodName"),
            arguments.getArrayOrThrow("methodArguments"),
            arguments.getArrayOrThrow("methodBody"),
            arguments.getOrThrow(FILE_PATH));
      case "openRepl":
        return new RunReplListener(callback, project,
            arguments.getOrThrow("module"));
      case "replInput":
        return new ReplInputListener(callback, project,
            arguments.getOrThrow("input"));
      default:
        throw new IllegalArgumentException("Unsupported action: '" + action + "'");
    }
  }

  @Override
  public @NotNull ComponentPresenter createPresenter(@NotNull String component,
                                                     @NotNull String instruction,
                                                     @NotNull String info,
                                                     @NotNull Arguments componentArguments,
                                                     @NotNull Arguments actionArguments,
                                                     String @NotNull [] assertClosed) {
    for (var closedComponent : assertClosed) {
      switch (closedComponent.split("\\|")[0]) {
        case "projectTree":
          ComponentDatabase.hideToolWindow(ComponentDatabase.PROJECT_TOOL_WINDOW, project);
          break;
        case "aPlusCourses":
          ComponentDatabase.hideToolWindow(ComponentDatabase.APLUS_TOOL_WINDOW, project);
          break;
        case "editor":
          ComponentDatabase.closeFile(closedComponent.replaceFirst("editor\\|", ""), project);
          break;
        case "repl":
          ComponentDatabase.closeRepls(project);
          break;
        default:
          throw new IllegalArgumentException("Unsupported component: '" + closedComponent + "'");
      }
    }
    switch (component) {
      case "projectTree":
        return new ProjectTreePresenter(instruction, info, project);
      case "editor":
        return new EditorPresenter(instruction, info, actionArguments.getOrThrow(FILE_PATH),
            project);
      case "repl":
        return new ReplPresenter(instruction, info, actionArguments.getOrThrow("module"), project);
      default:
        throw new IllegalArgumentException("Unsupported component: '" + component + "'");
    }
  }
}
