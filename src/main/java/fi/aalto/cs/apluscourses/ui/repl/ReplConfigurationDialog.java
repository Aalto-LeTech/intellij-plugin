package fi.aalto.cs.apluscourses.ui.repl;

import com.intellij.uiDesigner.core.GridConstraints;
import fi.aalto.cs.apluscourses.ui.base.DialogBaseHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static fi.aalto.cs.apluscourses.utils.PluginResourceBundle.getText;

public class ReplConfigurationDialog extends DialogBaseHelper {

  private JPanel contentPane;
  private JButton buttonOk;
  private JButton buttonCancel;
  private ReplConfigurationForm replConfigurationForm;
  private JPanel form;

  /**
   * Creates a REPL configuration dialog without a form (required later on by autogenerated code).
   */
  public ReplConfigurationDialog() {
    setContentPane(contentPane);
    setModal(true);
    setResizable(false);
    getRootPane().setDefaultButton(buttonOk);
    addDefaultListeners(buttonOk, buttonCancel, contentPane);
    setTitle(getText("ui.repl.configuration.form.title"));

    //  location "center" (it's still a big question "center" of what)
    this.setLocationRelativeTo(null);
    this.pack();
  }

  /**
   * Creates a REPL configuration dialog without a form (required later on by autogenerated code).
   */
  public ReplConfigurationDialog(@NotNull ReplConfigurationForm replConfigurationForm) {
    this();
    setReplConfigurationForm(replConfigurationForm);
  }

  public void setReplConfigurationForm(
      @NotNull ReplConfigurationForm replConfigurationForm) {
    this.replConfigurationForm = replConfigurationForm;
    replaceReplConfigurationFormWithIn(replConfigurationForm, form);
  }

  protected void replaceReplConfigurationFormWithIn(
      @NotNull ReplConfigurationForm replConfigurationForm,
      @NotNull JPanel jpanelForm) {
    //  this is copied from (IJ) autogenerated code
    final GridConstraints gridConstraints = new GridConstraints(
        0, 0, 1, 1,
        GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        null, null, null, 0, false);

    //  the only one in this particular implementation
    if (jpanelForm.getComponentCount() > 0) {
      jpanelForm.remove(0);
    }
    jpanelForm.add(replConfigurationForm.getContentPane(), gridConstraints);
    jpanelForm.revalidate();
    jpanelForm.repaint();
  }

  @Override
  public void onOk() {
    replConfigurationForm.updateModel();
    super.onOk();
  }

  @Override
  public void onCancel() {
    replConfigurationForm.cancelReplStart();
    super.onCancel();
  }

  public ReplConfigurationForm getReplConfigurationForm() {
    return replConfigurationForm;
  }
}