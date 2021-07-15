package fi.aalto.cs.apluscourses.model;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class SubmissionFileInfo {
  @NotNull
  private final String fileName;

  @NotNull
  private final String url;

  /**
   * A constructor.
   */
  public SubmissionFileInfo(@NotNull String fileName,
                            @NotNull String url) {

    this.fileName = fileName;
    this.url = url;
  }

  public static SubmissionFileInfo fromJsonObject(@NotNull JSONObject jsonObject) {
    return new SubmissionFileInfo(jsonObject.getString("filename"), jsonObject.getString("url"));
  }

  @NotNull
  public String getFileName() {
    return fileName;
  }

  @NotNull
  public String getUrl() {
    return url;
  }
}
