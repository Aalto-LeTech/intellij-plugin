package fi.aalto.cs.apluscourses.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import fi.aalto.cs.apluscourses.intellij.notifications.DefaultNotifier;
import fi.aalto.cs.apluscourses.intellij.notifications.NetworkErrorNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.Notifier;
import fi.aalto.cs.apluscourses.intellij.services.CourseProjectProvider;
import fi.aalto.cs.apluscourses.intellij.services.Dialogs;
import fi.aalto.cs.apluscourses.intellij.services.PluginSettings;
import fi.aalto.cs.apluscourses.intellij.utils.Interfaces;
import fi.aalto.cs.apluscourses.presentation.SelectStudentViewModel;
import java.io.IOException;
import java.time.ZonedDateTime;
import org.jetbrains.annotations.NotNull;

public class SelectStudentAction extends AnAction {
  @NotNull
  private final CourseProjectProvider courseProjectProvider;

  @NotNull
  private final Interfaces.AssistantModeProvider assistantModeProvider;

  @NotNull
  private final Notifier notifier;

  @NotNull
  private final Dialogs dialogs;

  /**
   * Default constructor.
   */
  public SelectStudentAction() {
    this(PluginSettings.getInstance()::getCourseProject,
        () -> PluginSettings.getInstance().isAssistantMode(),
        new DefaultNotifier(),
        Dialogs.DEFAULT);
  }

  /**
   * A constructor.
   */
  public SelectStudentAction(@NotNull CourseProjectProvider courseProjectProvider,
                             @NotNull Interfaces.AssistantModeProvider assistantModeProvider,
                             @NotNull Notifier notifier,
                             @NotNull Dialogs dialogs) {
    this.courseProjectProvider = courseProjectProvider;
    this.assistantModeProvider = assistantModeProvider;
    this.notifier = notifier;
    this.dialogs = dialogs;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    var courseProject = courseProjectProvider.getCourseProject(e.getProject());
    if (courseProject == null) {
      return;
    }
    var course = courseProject.getCourse();
    var auth = courseProject.getAuthentication();
    if (auth == null) {
      return;
    }
    try {
      var students = course.getExerciseDataSource()
          .getStudents(course, auth, ZonedDateTime.now().minusDays(7));
      var studentListViewModel = new SelectStudentViewModel(students, course, auth);
      if (!dialogs.create(studentListViewModel, e.getProject()).showAndGet()) {
        return;
      }
      courseProject.setSelectedStudent(studentListViewModel.selectedStudent.get());
      courseProject.getExercisesUpdater().restart();
    } catch (IOException ex) {
      notifier.notifyAndHide(new NetworkErrorNotification(ex), e.getProject());
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setVisible(assistantModeProvider.isAssistantMode());
    var courseProject = courseProjectProvider.getCourseProject(e.getProject());
    e.getPresentation().setEnabled(courseProject != null && courseProject.getAuthentication() != null);
  }
}
