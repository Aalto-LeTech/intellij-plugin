package fi.aalto.cs.apluscourses.dal;

import static fi.aalto.cs.apluscourses.utils.ListUtil.appendTwoLists;

import fi.aalto.cs.apluscourses.model.Authentication;
import fi.aalto.cs.apluscourses.model.Cache;
import fi.aalto.cs.apluscourses.model.Course;
import fi.aalto.cs.apluscourses.model.Exercise;
import fi.aalto.cs.apluscourses.model.ExerciseDataSource;
import fi.aalto.cs.apluscourses.model.ExerciseGroup;
import fi.aalto.cs.apluscourses.model.Group;
import fi.aalto.cs.apluscourses.model.InvalidAuthenticationException;
import fi.aalto.cs.apluscourses.model.JsonCache;
import fi.aalto.cs.apluscourses.model.Points;
import fi.aalto.cs.apluscourses.model.Student;
import fi.aalto.cs.apluscourses.model.Submission;
import fi.aalto.cs.apluscourses.model.SubmissionInfo;
import fi.aalto.cs.apluscourses.model.SubmissionResult;
import fi.aalto.cs.apluscourses.model.Tutorial;
import fi.aalto.cs.apluscourses.model.User;
import fi.aalto.cs.apluscourses.utils.CoursesClient;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.apache.http.Header;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class APlusExerciseDataSource implements ExerciseDataSource {

  private static final String EXERCISES = "exercises";
  private static final String SUBMISSIONS = "submissions";
  private static final String COURSES = "courses";
  private static final String POINTS = "points";
  private static final String USERS = "users";
  private static final String STUDENTS = "students";
  public static final String RESULTS = "results";
  public static final String ME = "me";

  @NotNull
  private final Client client;

  @NotNull
  private final String apiUrl;

  @NotNull
  private final Parser parser;


  /**
   * Default constructor.
   */
  public APlusExerciseDataSource(@NotNull String apiUrl, @NotNull Path cacheFile) {
    var dataAccess = new DefaultDataAccess(new JsonCache(cacheFile));
    this.client = dataAccess;
    this.parser = dataAccess;
    this.apiUrl = apiUrl;
  }

  /**
   * Constructor for demanding use (e.g. tests).
   *
   * @param client Client to fetch and post.
   * @param apiUrl The base URL of API.
   * @param parser JSON parser.
   */
  public APlusExerciseDataSource(@NotNull String apiUrl,
                                 @NotNull Client client,
                                 @NotNull Parser parser) {
    this.client = client;
    this.apiUrl = apiUrl;
    this.parser = parser;
  }

  @Override
  public <T> List<T> getPaginatedResults(@NotNull String url,
                                         @NotNull Authentication authentication,
                                         @Nullable ZonedDateTime zonedDateTime,
                                         @NotNull Function<JSONObject, T> parseFunction)
      throws IOException {
    JSONObject response = zonedDateTime == null ? client.fetch(url, authentication)
        : client.fetch(url, authentication, zonedDateTime);
    var results = parser.parsePaginatedResults(response, parseFunction);
    var nextPage = parser.parseNextPageUrl(response);

    return nextPage == null ? results
        : appendTwoLists(results, getPaginatedResults(nextPage, authentication, zonedDateTime, parseFunction));
  }

  @Override
  public <T> List<T> getPaginatedResults(@NotNull String url,
                                         @NotNull Authentication authentication,
                                         @NotNull Function<JSONObject, T> parseFunction)
      throws IOException {
    return getPaginatedResults(url, authentication, null, parseFunction);
  }

  /**
   * Get all of the groups from the A+ API for the user corresponding to the given authentication. A
   * group with id 0 and a single member name "Submit alone" is added to the beginning of the list.
   *
   * @return A list of {@link Group}s that the user is a member of in the given course.
   * @throws IOException If an error occurs (e.g. network error).
   */
  @NotNull
  public List<Group> getGroups(@NotNull Course course, @NotNull Authentication authentication)
      throws IOException {
    String url = apiUrl + COURSES + "/" + course.getId() + "/mygroups/";
    return getPaginatedResults(url, authentication, Group::fromJsonObject);
  }

  /**
   * Get all of the exercise groups in the given course by making a request to the A+ API.
   *
   * @throws IOException If an IO error occurs (for an example a network issue). This is an instance
   *                     of {@link InvalidAuthenticationException} if authentication is invalid.
   */
  @Override
  @NotNull
  public List<ExerciseGroup> getExerciseGroups(@NotNull Course course,
                                               @NotNull Authentication authentication)
      throws IOException {
    String url = apiUrl + COURSES + "/" + course.getId() + "/" + EXERCISES + "/";
    return getPaginatedResults(url, authentication, ExerciseGroup::fromJsonObject);
  }

  /**
   * Get all of the points for the given course by making a request to the A+ API.
   *
   * @throws IOException If an IO error occurs (for an example a network issue). This is an instance
   *                     of {@link InvalidAuthenticationException} if authentication is invalid.
   */
  @Override
  @NotNull
  public Points getPoints(@NotNull Course course, @NotNull Authentication authentication)
      throws IOException {
    return getPoints(course, authentication, null);
  }

  @Override
  @NotNull
  public Points getPoints(@NotNull Course course, @NotNull Authentication authentication,
                          @Nullable Student student) throws IOException {
    String url = apiUrl + COURSES + "/" + course.getId() + "/" + POINTS + "/"
        + (student == null ? ME : student.getId()) + "/";
    JSONObject response = client.fetch(url, authentication);
    return parser.parsePoints(response);
  }

  @Override
  @NotNull
  public SubmissionResult getSubmissionResult(@NotNull String submissionUrl,
                                              @NotNull Exercise exercise,
                                              @NotNull Authentication authentication,
                                              @NotNull ZonedDateTime minCacheEntryTime)
      throws IOException {
    JSONObject response = client.fetch(submissionUrl, authentication, minCacheEntryTime);
    return parser.parseSubmissionResult(response, exercise);
  }

  @Override
  @NotNull
  public Exercise getExercise(long exerciseId,
                              @NotNull Points points,
                              @NotNull Map<Long, Tutorial> tutorials,
                              @NotNull Authentication authentication,
                              @NotNull ZonedDateTime minCacheEntryTime) throws IOException {
    var url = apiUrl + "exercises/" + exerciseId + "/";
    var response = client.fetch(url, authentication, minCacheEntryTime);
    return parser.parseExercise(response, points, tutorials);
  }

  @Override
  @NotNull
  public User getUser(@NotNull Authentication authentication) throws IOException {
    String url = apiUrl + USERS + "/" + ME + "/";
    JSONObject response = client.fetch(url, authentication);
    return new User(authentication, parser.parseUserName(response));
  }

  @Override
  @NotNull
  public List<Student> getStudents(@NotNull Course course,
                                   @NotNull Authentication authentication,
                                   @NotNull ZonedDateTime minCacheEntryTime) throws IOException {
    String url = apiUrl + COURSES + "/" + course.getId() + "/" + STUDENTS + "/";
    return getPaginatedResults(url, authentication, minCacheEntryTime, Student::fromJsonObject);
  }

  @Override
  @NotNull
  public ZonedDateTime getEndingTime(@NotNull Course course,
                                     @NotNull Authentication authentication)
          throws IOException {
    String url = apiUrl + COURSES + "/" + course.getId() + "/";
    JSONObject response = client.fetch(url, authentication);
    return parser.parseEndingTime(response);
  }

  /**
   * Sends the submission to the server.
   *
   * @throws IOException If there are IO related errors.
   */
  @Override
  @Nullable
  public String submit(@NotNull Submission submission, @NotNull Authentication authentication)
      throws IOException {
    Map<String, Object> data = new HashMap<>();
    data.put("__aplus__", "{ \"group\": " + submission.getGroup().getId() + ", \"lang\": \""
        + submission.getLanguage() + "\" }");
    for (Map.Entry<String, Path> entry : submission.getFiles().entrySet()) {
      data.put(entry.getKey(), entry.getValue().toFile());
    }
    String url = apiUrl + EXERCISES + "/" + submission.getExercise().getId()
        + "/" + SUBMISSIONS + "/submit/";
    return client.post(url, authentication, data);
  }

  @NotNull
  public Client getClient() {
    return client;
  }

  @NotNull
  public String getApiUrl() {
    return apiUrl;
  }

  @NotNull
  public Parser getParser() {
    return parser;
  }

  public static class DefaultDataAccess implements Client, Parser {

    @NotNull
    private final Cache<String, JSONObject> cache;

    public DefaultDataAccess(@NotNull Cache<String, JSONObject> cache) {
      this.cache = cache;
    }

    @Override
    public JSONObject fetch(@NotNull String url,
                            @Nullable Authentication authentication,
                            @NotNull ZonedDateTime minCacheEntryTime) throws IOException {
      var cacheEntry = cache.getEntry(url);
      if (cacheEntry != null && cacheEntry.getCreationTime().compareTo(minCacheEntryTime) >= 0) {
        return cacheEntry.getValue();
      }
      try (InputStream inputStream = CoursesClient.fetch(new URL(url), authentication)) {
        var response = new JSONObject(new JSONTokener(inputStream));
        cache.putValue(url, response);
        return response;
      }
    }

    @Override
    @Nullable
    public String post(@NotNull String url,
                       @NotNull Authentication authentication,
                       @NotNull Map<String, Object> data) throws IOException {
      return CoursesClient.post(
          new URL(url),
          authentication,
          data,
          response -> Optional
              .ofNullable(response.getFirstHeader("Location"))
              .map(Header::getValue)
              .orElse(null)
      );
    }

    @Override
    public <T> List<T> parsePaginatedResults(@NotNull JSONObject object,
                                             @NotNull Function<JSONObject, T> parseFunction) {
      var jsonResults = object.getJSONArray(RESULTS);
      var results = new ArrayList<T>();
      for (int i = 0; i < jsonResults.length(); i++) {
        results.add(parseFunction.apply(jsonResults.getJSONObject(i)));
      }
      return results;
    }

    @Override
    public String parseNextPageUrl(@NotNull JSONObject object) {
      return object.optString("next", null);
    }

    @Override
    public SubmissionInfo parseSubmissionInfo(@NotNull JSONObject object) {
      return SubmissionInfo.fromJsonObject(object);
    }

    @Override
    public Group parseGroup(@NotNull JSONObject object) {
      return Group.fromJsonObject(object);
    }

    @Override
    public List<ExerciseGroup> parseExerciseGroups(@NotNull JSONArray array) {
      return ExerciseGroup.fromJsonArray(array);
    }

    @Override
    public Points parsePoints(@NotNull JSONObject object) {
      return Points.fromJsonObject(object);
    }

    @Override
    public Exercise parseExercise(@NotNull JSONObject jsonObject,
                                  @NotNull Points points,
                                  @NotNull Map<Long, Tutorial> tutorials) {
      return Exercise.fromJsonObject(jsonObject, points, tutorials);
    }

    @Override
    public SubmissionResult parseSubmissionResult(@NotNull JSONObject object,
                                                  @NotNull Exercise exercise) {
      return SubmissionResult.fromJsonObject(object, exercise);
    }

    @Override
    public String parseUserName(@NotNull JSONObject object) {
      var fullName = object.optString("full_name");
      var username = object.optString("username");
      return fullName.equals("") ? username : fullName;
    }

    @Override
    public ZonedDateTime parseEndingTime(@NotNull JSONObject object) {
      return ZonedDateTime.parse(object.getString("ending_time"));
    }
  }
}
