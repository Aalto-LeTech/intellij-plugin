package fi.aalto.cs.apluscourses.ui.exercise;

import static fi.aalto.cs.apluscourses.utils.PluginResourceBundle.getText;

import com.intellij.openapi.project.Project;
import fi.aalto.cs.apluscourses.model.Module;
import fi.aalto.cs.apluscourses.presentation.exercise.DownloadSubmissionViewModel;
import fi.aalto.cs.apluscourses.ui.GuiObject;
import fi.aalto.cs.apluscourses.ui.IconListCellRenderer;
import fi.aalto.cs.apluscourses.ui.base.OurComboBox;
import fi.aalto.cs.apluscourses.ui.base.OurDialogWrapper;
import fi.aalto.cs.apluscourses.ui.base.TextField;
import icons.PluginIcons;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DownloadSubmissionDialog extends OurDialogWrapper {
  @NotNull
  private final DownloadSubmissionViewModel viewModel;
  private JPanel basePanel;
  @GuiObject
  private OurComboBox<Module> moduleComboBox;
  @GuiObject
  private TextField nameField;

  /**
   * A constructor.
   */
  public DownloadSubmissionDialog(@NotNull DownloadSubmissionViewModel viewModel, @NotNull Project project) {
    super(project);
    this.viewModel = viewModel;
    setTitle(getText("ui.toolWindow.subTab.exercises.submission.download"));
    registerValidationItem(nameField.textBindable);
    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return basePanel;
  }

  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  private void createUIComponents() {
    moduleComboBox = new OurComboBox<>(viewModel.getModules().toArray(Module[]::new), Module.class);
    moduleComboBox.setRenderer(new IconListCellRenderer<>(viewModel.getPrompt(),
        Module::getName, PluginIcons.A_PLUS_MODULE));
    moduleComboBox.selectedItemBindable.bindToSource(viewModel.selectedModule);
    nameField = new TextField();
    nameField.textBindable.bindToSource(viewModel.moduleName);
  }
}
