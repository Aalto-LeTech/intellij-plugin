package fi.aalto.cs.apluscourses.presentation.exercise;

import static fi.aalto.cs.apluscourses.utils.PluginResourceBundle.getAndReplaceText;
import static fi.aalto.cs.apluscourses.utils.PluginResourceBundle.getText;

import fi.aalto.cs.apluscourses.model.SubmissionResult;
import fi.aalto.cs.apluscourses.presentation.base.Searchable;
import fi.aalto.cs.apluscourses.presentation.base.SelectableNodeViewModel;
import org.jetbrains.annotations.NotNull;

public class SubmissionResultViewModel extends SelectableNodeViewModel<SubmissionResult>
        implements Searchable {

  private final int submissionNumber;

  /**
   * Construct a view model corresponding to the given submission result.
   */
  public SubmissionResultViewModel(@NotNull SubmissionResult submissionResult,
                                   int submissionNumber) {
    super(submissionResult, null);
    this.submissionNumber = submissionNumber;
  }

  @NotNull
  public String getPresentableName() {
    return getAndReplaceText("presentation.submissionResultViewModel.name",
        submissionNumber, String.valueOf(getId()));
  }


  /**
   * Constructs text appearing in parenthesis.
   *
   * @return Returns status text for assignment.
   *         Methods considers case assignment has not yet been graded
   */
  @NotNull
  public String getStatusText() {
    SubmissionResult model = getModel();
    return (model.getStatus() == SubmissionResult.Status.UNKNOWN)
            ? getText("presentation.submissionResultViewModel.inGrading")
            : getStatus(model);
  }

  private String getStatus(SubmissionResult model) {
    return getAndReplaceText("presentation.submissionResultViewModel.points",
        model.getPoints(), model.getExercise().getMaxPoints());
  }

  @Override
  public @NotNull String getSearchableString() {
    return getPresentableName();
  }

  @Override
  public long getId() {
    return getModel().getId();
  }
}
