package fi.aalto.cs.apluscourses.model;

import static fi.aalto.cs.apluscourses.utils.MapUtil.mapValueListsSize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

public class Points {

  @NotNull
  private static final Pattern submissionIdPattern = Pattern.compile("/submissions/(\\d+)/?$");

  @NotNull
  private final Map<Long, List<Long>> exercises;

  @NotNull
  private final Map<Long, List<Long>> submissions;

  @NotNull
  private final Map<Long, Long> bestSubmissions;

  private Integer exercisesCount;
  private Integer submissionsCount;

  /**
   * Construct an instance with the given maps.
   *
   * @param exercises       A map of exercise group IDs to a list of exercises for that exercise
   *                        group.
   * @param submissions     A map of exercise IDs to a list of submission IDs for that exercise.
   *                        The first element of the list should be the ID of the oldest submission
   *                        and the last element should be the ID of the latest submission.
   * @param bestSubmissions A map of exercise IDs to the IDs of the best submission for each
   *                        exercise.
   */
  public Points(@NotNull Map<Long, List<Long>> exercises,
                @NotNull Map<Long, List<Long>> submissions,
                @NotNull Map<Long, Long> bestSubmissions) {
    this.exercises = exercises;
    this.submissions = submissions;
    this.bestSubmissions = bestSubmissions;
  }

  @NotNull
  public List<Long> getSubmissions(long exerciseId) {
    return submissions.getOrDefault(exerciseId, Collections.emptyList());
  }

  /**
   * Returns the amount of exercises.
   */
  public int getSubmissionsCount() {
    if (submissionsCount == null) {
      submissionsCount = mapValueListsSize(submissions);
    }
    return submissionsCount;
  }

  @NotNull
  public List<Long> getExercises(long exerciseGroupId) {
    return exercises.getOrDefault(exerciseGroupId, Collections.emptyList());
  }

  /**
   * Returns the amount of exercises.
   */
  public int getExercisesCount() {
    if (exercisesCount == null) {
      exercisesCount = mapValueListsSize(exercises);
    }
    return exercisesCount;
  }

  @NotNull
  public Map<Long, Long> getBestSubmissionIds() {
    return Collections.unmodifiableMap(bestSubmissions);
  }

  /**
   * Constructs a {@link Points} instance from the given JSON object.
   *
   * @param jsonObject The JSON object from which the {@link Points} instance is constructed.
   */
  @NotNull
  public static Points fromJsonObject(@NotNull JSONObject jsonObject) {
    JSONArray modulesArray = jsonObject.getJSONArray("modules");
    Map<Long, List<Long>> exercises = new HashMap<>();
    Map<Long, List<Long>> submissions = new HashMap<>();
    Map<Long, Long> bestSubmissions = new HashMap<>();
    for (int i = 0; i < modulesArray.length(); ++i) {
      JSONObject module = modulesArray.getJSONObject(i);
      var exerciseGroupId = module.getLong("id");
      List<Long> exerciseIds = new ArrayList<>();
      JSONArray exercisesArray = module.getJSONArray("exercises");
      for (int j = 0; j < exercisesArray.length(); ++j) {
        JSONObject exercise = exercisesArray.getJSONObject(j);
        long exerciseId = exercise.getLong("id");
        exerciseIds.add(exerciseId);
        parseSubmissions(exercise, exerciseId, submissions);

        var bestSubmissionId = parseSubmissionId(exercise.optString("best_submission"));
        if (bestSubmissionId != null) {
          bestSubmissions.put(exerciseId, bestSubmissionId);
        }
      }
      exercises.put(exerciseGroupId, exerciseIds);
    }
    return new Points(exercises, submissions, bestSubmissions);
  }

  /*
   * Parses the submissions (IDs and points) from the given JSON and adds them to the given maps.
   */
  private static void parseSubmissions(@NotNull JSONObject exerciseJson,
                                       long exerciseId,
                                       @NotNull Map<Long, List<Long>> submissions) {
    JSONArray submissionsArray = exerciseJson.getJSONArray("submissions_with_points");
    List<Long> submissionIds = new ArrayList<>(submissionsArray.length());
    for (int i = submissionsArray.length() - 1; i >= 0; --i) {
      JSONObject submission = submissionsArray.getJSONObject(i);
      long submissionId = submission.getLong("id");
      submissionIds.add(submissionId);
    }
    submissions.put(exerciseId, submissionIds);
  }

  @Nullable
  private static Long parseSubmissionId(String submissionUrl) {
    var matcher = submissionIdPattern.matcher(submissionUrl);
    if (!matcher.find()) {
      return null;
    }
    return Long.parseLong(matcher.group(1));
  }

}
