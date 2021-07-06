package fi.aalto.cs.apluscourses.model;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class SubmissionResult implements Browsable {

  public SubmissionFileInfo[] getFilesInfo() {
    return filesInfo;
  }

  public enum Status {
    UNKNOWN,
    GRADED,
    UNOFFICIAL,
    WAITING
  }

  private final long submissionId;

  private final int points;

  @NotNull
  private final Status status;

  @NotNull
  private final Exercise exercise;

  private final SubmissionFileInfo @NotNull [] filesInfo;

  /**
   * Construct an instance with the given ID and exercise URL.
   */
  public SubmissionResult(long submissionId,
                          int points,
                          @NotNull Status status,
                          @NotNull Exercise exercise) {
    this(submissionId, points, status, exercise, new SubmissionFileInfo[0]);
  }

  /**
   * Construct an instance with the given ID and exercise URL.
   */
  public SubmissionResult(long submissionId,
                          int points,
                          @NotNull Status status,
                          @NotNull Exercise exercise,
                          SubmissionFileInfo @NotNull [] filesInfo) {
    this.submissionId = submissionId;
    this.points = points;
    this.status = status;
    this.exercise = exercise;
    this.filesInfo = filesInfo;
  }

  /**
   * Construct a {@link SubmissionResult} instance from the given JSON object. The JSON object must
   * contain an integer for the "id" key, and optionally a string value for the "status" key.
   */
  @NotNull
  public static SubmissionResult fromJsonObject(@NotNull JSONObject jsonObject,
                                                @NotNull Exercise exercise) {
    long id = jsonObject.getLong("id");
    int points = jsonObject.getInt("grade");

    Status status = Status.UNKNOWN;
    String statusString = jsonObject.optString("status");
    if ("ready".equals(statusString)) {
      status = Status.GRADED;
    } else if ("unofficial".equals(statusString)) {
      status = Status.UNOFFICIAL;
    } else if ("waiting".equals(statusString)) {
      status = Status.WAITING;
    }

    var files = jsonObject.getJSONArray("files");
    List<SubmissionFileInfo> filesInfo = new ArrayList<>();
    for (int i = 0; i < files.length(); i++) {
      filesInfo.add(SubmissionFileInfo.fromJsonObject(files.getJSONObject(i)));
    }

    return new SubmissionResult(id, points, status, exercise, new SubmissionFileInfo[0]);
  }

  public long getId() {
    return submissionId;
  }

  public int getPoints() {
    return points;
  }

  @NotNull
  public Status getStatus() {
    return status;
  }

  @Override
  public @NotNull String getHtmlUrl() {
    return exercise.getHtmlUrl() + "submissions/" + submissionId + "/";
  }

  public @NotNull Exercise getExercise() {
    return exercise;
  }


}
