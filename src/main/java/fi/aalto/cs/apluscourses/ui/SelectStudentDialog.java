package fi.aalto.cs.apluscourses.ui;

import static fi.aalto.cs.apluscourses.utils.PluginResourceBundle.getText;

import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import fi.aalto.cs.apluscourses.intellij.notifications.DefaultNotifier;
import fi.aalto.cs.apluscourses.intellij.notifications.NetworkErrorNotification;
import fi.aalto.cs.apluscourses.model.Student;
import fi.aalto.cs.apluscourses.presentation.SelectStudentViewModel;
import fi.aalto.cs.apluscourses.ui.base.OurDialogWrapper;
import fi.aalto.cs.apluscourses.ui.base.SingleSelectionList;
import icons.PluginIcons;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.concurrent.Executors;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SelectStudentDialog extends OurDialogWrapper {
  private final SelectStudentViewModel viewModel;
  private final Project project;
  private JPanel basePanel;
  @GuiObject
  private SingleSelectionList<Student> studentList;
  @GuiObject
  private JScrollPane scrollPane;

  /**
   * A constructor.
   */
  public SelectStudentDialog(@NotNull SelectStudentViewModel viewModel, @NotNull Project project) {
    super(project);
    this.viewModel = viewModel;
    this.project = project;
    setTitle(getText("ui.selectStudentDialog.title"));
    registerValidationItem(studentList.selectionBindable);
    init();
  }

  @NotNull
  @Override
  protected Action @NotNull [] createActions() {
    return new Action[]{new RefreshStudentsAction(), getOKAction(), getCancelAction()};
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    return basePanel;
  }

  @Override
  @Nullable
  public JComponent getPreferredFocusedComponent() {
    return studentList;
  }

  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  private void createUIComponents() {
    scrollPane = new JBScrollPane();

    studentList = new SingleSelectionList<>(viewModel.getStudents());
    studentList.selectionBindable.bindToSource(viewModel.selectedStudent);
    new ListSpeedSearch<>(studentList, Student::getPresentableName);

    studentList.setCellRenderer(new ColoredListCellRenderer<>() {
      @Override
      protected void customizeCellRenderer(@NotNull JList<? extends Student> list,
                                           Student value,
                                           int index,
                                           boolean selected,
                                           boolean hasFocus) {
        setIcon(PluginIcons.A_PLUS_USER_ACTIVE);
        append(value.getPresentableName());
        SpeedSearchUtil.applySpeedSearchHighlighting(list, this, true, selected);
      }
    });
  }

  private class RefreshStudentsAction extends DialogWrapperAction {
    public RefreshStudentsAction() {
      super(getText("ui.selectStudentDialog.refresh"));
    }

    @Override
    protected void doAction(ActionEvent e) {
      var course = viewModel.getCourse();
      var auth = viewModel.getAuthentication();

      SwingUtilities.invokeLater(() -> studentList.setPaintBusy(true));

      Executors.newSingleThreadExecutor().submit(() -> {
        try {
          var students = course.getExerciseDataSource()
              .getStudents(course, auth, OffsetDateTime.MAX.toZonedDateTime());
          SwingUtilities.invokeLater(() -> {
            viewModel.setStudents(students);
            viewModel.sortStudents();
            studentList.setListData(viewModel.getStudents().toArray(Student[]::new));
            studentList.setPaintBusy(false);
          });
        } catch (IOException ex) {
          new DefaultNotifier().notify(new NetworkErrorNotification(ex), project);
        }
        return null;
      });
    }


  }
}
