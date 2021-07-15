package fi.aalto.cs.apluscourses.model;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class Student {
  private final long id;
  @NotNull
  private final String fullName;
  @NotNull
  private final String studentId;
  @NotNull
  private final String pointsUrl;

  /**
   * A constructor.
   */
  public Student(long id,
                 @NotNull String fullName,
                 @NotNull String studentId,
                 @NotNull String pointsUrl) {

    this.id = id;
    this.fullName = fullName;
    this.studentId = studentId;
    this.pointsUrl = pointsUrl;
  }

  /**
   * Constructs a Student from JSON.
   */
  public static Student fromJsonObject(@NotNull JSONObject jsonObject) {
    var myId = jsonObject.getLong("id");
    var myFullName = jsonObject.getString("full_name");
    var myStudentId = jsonObject.optString("student_id");
    var myPointsUrl = jsonObject.getString("points");
    return new Student(myId, myFullName, myStudentId, myPointsUrl);
  }

  @NotNull
  public String getFullName() {
    return fullName;
  }

  public long getId() {
    return id;
  }

  @NotNull
  public String getStudentId() {
    return studentId;
  }

  @NotNull
  public String getPresentableName() {
    return fullName + " " + studentId;
  }
}
