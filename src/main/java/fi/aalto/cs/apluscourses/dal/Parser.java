package fi.aalto.cs.apluscourses.dal;

import fi.aalto.cs.apluscourses.model.Exercise;
import fi.aalto.cs.apluscourses.model.ExerciseGroup;
import fi.aalto.cs.apluscourses.model.Group;
import fi.aalto.cs.apluscourses.model.Points;
import fi.aalto.cs.apluscourses.model.SubmissionInfo;
import fi.aalto.cs.apluscourses.model.SubmissionResult;
import fi.aalto.cs.apluscourses.model.Tutorial;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

public interface Parser {

  <T> List<T> parsePaginatedResults(@NotNull JSONObject object,
                                    @NotNull Function<JSONObject, T> parseFunction);

  String parseNextPageUrl(@NotNull JSONObject object);

  SubmissionInfo parseSubmissionInfo(@NotNull JSONObject object);

  Group parseGroup(@NotNull JSONObject object);

  List<ExerciseGroup> parseExerciseGroups(@NotNull JSONArray array);

  Points parsePoints(@NotNull JSONObject object);

  Exercise parseExercise(@NotNull JSONObject jsonObject,
                         @NotNull Points points,
                         @NotNull Map<Long, Tutorial> tutorials);

  SubmissionResult parseSubmissionResult(@NotNull JSONObject jsonObject,
                                         @NotNull Exercise exercise);

  String parseUserName(@NotNull JSONObject object);

  ZonedDateTime parseEndingTime(@NotNull JSONObject object);

  /**
   * Parses an JSON array to a list using a given parsing function.
   *
   * @param array JSON array.
   * @param parse Function that parses an JSONObject to T object.
   * @param <T>   Type of the result items.
   * @return A list whose items are parsed from JSON objects of the given array.
   */
  default <T> List<T> parseArray(@NotNull JSONArray array, @NotNull Function<JSONObject, T> parse) {
    int length = array.length();
    List<T> results = new ArrayList<>(length);
    for (int i = 0; i < length; i++) {
      results.add(parse.apply(array.getJSONObject(i)));
    }
    return results;
  }

}
