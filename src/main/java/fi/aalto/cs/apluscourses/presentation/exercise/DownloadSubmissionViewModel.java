package fi.aalto.cs.apluscourses.presentation.exercise;

import static fi.aalto.cs.apluscourses.utils.PluginResourceBundle.getText;

import fi.aalto.cs.apluscourses.model.Course;
import fi.aalto.cs.apluscourses.model.Module;
import fi.aalto.cs.apluscourses.utils.observable.ObservableProperty;
import fi.aalto.cs.apluscourses.utils.observable.ObservableReadWriteProperty;
import fi.aalto.cs.apluscourses.utils.observable.ValidationError;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DownloadSubmissionViewModel {
  @NotNull
  private final List<Module> modules;

  @NotNull
  public final ObservableProperty<Module> selectedModule;

  @NotNull
  public final ObservableProperty<String> moduleName;

  private final long submissionId;

  @NotNull
  private final List<String> installedModules;

  /**
   * A constructor.
   */
  public DownloadSubmissionViewModel(@NotNull Course course,
                                     @Nullable Module currentSelectedModule,
                                     long submissionId,
                                     @NotNull List<String> installedModules) {
    this.modules = course.getModules();
    this.submissionId = submissionId;
    this.installedModules = installedModules;
    this.selectedModule = new ObservableReadWriteProperty<>(currentSelectedModule);
    this.moduleName = new ObservableReadWriteProperty<>(getModuleName(), this::validateName);
    this.selectedModule.addSimpleObserver(this, a -> moduleName.set(getModuleName()));
  }

  @NotNull
  public List<Module> getModules() {
    return modules;
  }

  public String getPrompt() {
    return getText("presentation.moduleSelectionViewModel.prompt");
  }

  @NotNull
  public List<String> getInstalledModules() {
    return installedModules;
  }

  @NotNull
  private String getModuleName() {
    return submissionId + "_" + Optional.ofNullable(selectedModule.get()).map(Module::getName).orElse("");
  }

  /**
   * Checks if the name is taken.
   */
  @Nullable
  public ValidationError validateName(@Nullable String name) {
    if (getInstalledModules().contains(name)) {
      return new NameAlreadyTakenError();
    } else {
      return null;
    }
  }

  private static class NameAlreadyTakenError implements ValidationError {
    @Override
    public @NotNull String getDescription() {
      return getText("ui.toolWindow.subTab.exercises.submission.nameTaken");
    }
  }
}
