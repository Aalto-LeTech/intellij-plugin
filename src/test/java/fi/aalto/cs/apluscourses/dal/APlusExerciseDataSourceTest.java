package fi.aalto.cs.apluscourses.dal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.intellij.openapi.util.io.FileUtilRt;
import fi.aalto.cs.apluscourses.model.Authentication;
import fi.aalto.cs.apluscourses.model.Course;
import fi.aalto.cs.apluscourses.model.Exercise;
import fi.aalto.cs.apluscourses.model.ExerciseDataSource;
import fi.aalto.cs.apluscourses.model.ExerciseGroup;
import fi.aalto.cs.apluscourses.model.Group;
import fi.aalto.cs.apluscourses.model.ModelExtensions;
import fi.aalto.cs.apluscourses.model.Submission;
import fi.aalto.cs.apluscourses.model.SubmissionInfo;
import fi.aalto.cs.apluscourses.model.SubmittableFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class APlusExerciseDataSourceTest {

  final Authentication authentication = mock(Authentication.class);
  final String url = "https://example.com/";

  Client client;
  Parser parser;
  ExerciseDataSource exerciseDataSource;

  /**
   * Called before each test.
   */
  @Before
  public void setUp() {
    client = mock(Client.class);
    parser = mock(Parser.class);
    doCallRealMethod().when(parser).parseArray(any(), any());
    exerciseDataSource = new APlusExerciseDataSource(url, client, parser);
  }

  @Test
  public void testDefaultConstructor() {
    var exerciseDataSource = new APlusExerciseDataSource(
        url, Paths.get(FileUtilRt.getTempDirectory()));
    assertEquals(url, exerciseDataSource.getApiUrl());
    assertTrue(exerciseDataSource.getClient() instanceof APlusExerciseDataSource.DefaultDataAccess);
    assertTrue(exerciseDataSource.getParser() instanceof APlusExerciseDataSource.DefaultDataAccess);
  }

  @Test
  public void testGetGroups() throws IOException {
    JSONObject object0 = new JSONObject();
    JSONObject object1 = new JSONObject();

    JSONObject response = new JSONObject()
        .put("results", new JSONArray()
            .put(object0)
            .put(object1));

    Group group0 = new Group(101, new ArrayList<>());
    Group group1 = new Group(102, new ArrayList<>());

    doReturn(response).when(client)
        .fetch("https://example.com/courses/123/mygroups/", authentication);
    doReturn(group0).when(parser).parseGroup(object0);
    doReturn(group1).when(parser).parseGroup(object1);

    Course course = new ModelExtensions.TestCourse("123");

    List<Group> groups = exerciseDataSource.getGroups(course, authentication);

    assertEquals(2, groups.size());
    assertSame(group0, groups.get(0));
    assertSame(group1, groups.get(1));
  }

  @Test
  public void testGetExerciseGroups() throws IOException {
    JSONObject object0 = new JSONObject();
    JSONObject object1 = new JSONObject();
    JSONArray array = new JSONArray().put(object0).put(object1);

    JSONObject response = new JSONObject().put("results", array);

    ExerciseGroup exGroup0 = new ExerciseGroup(0, "First Week", "", true);
    ExerciseGroup exGroup1 = new ExerciseGroup(1, "Second Week", "", true);

    doReturn(response).when(client)
        .fetch("https://example.com/courses/99/exercises/", authentication);
    doReturn(List.of(exGroup0, exGroup1))
        .when(parser)
        .parseExerciseGroups(same(array));

    Course course = new ModelExtensions.TestCourse("99");

    var exGroups = exerciseDataSource.getExerciseGroups(course, authentication);

    assertEquals(2, exGroups.size());
    assertSame(exGroup0, exGroups.get(0));
    assertSame(exGroup1, exGroups.get(1));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSubmit() throws IOException {
    String key0 = "firstKey";
    String key1 = "anotherKey";
    SubmittableFile subFile0 = new SubmittableFile(key0, "first.scala");
    SubmittableFile subFile1 = new SubmittableFile(key1, "another.scala");

    Path path0 = Paths.get("somePath");
    Path path1 = Paths.get("anotherPath");
    Map<String, Path> paths = new HashMap<>();
    paths.put(key0, path0);
    paths.put(key1, path1);

    var info = new SubmissionInfo(Collections.singletonMap("fi", List.of(subFile0, subFile1)));

    Exercise exercise = new Exercise(71, "newex", "https://example.com", info, 0, 0, OptionalLong.empty());

    Group group = new Group(435, new ArrayList<>());

    Submission submission = new Submission(exercise, paths, group, "fi");

    exerciseDataSource.submit(submission, authentication);

    ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
    verify(client).post(
        eq("https://example.com/exercises/71/submissions/submit/"),
        eq(authentication),
        dataCaptor.capture()
    );

    Map<String, Object> data = dataCaptor.getValue();
    assertEquals(3, data.size());

    assertEquals(path0, ((File) data.get(key0)).toPath());
    assertEquals(path1, ((File) data.get(key1)).toPath());

    String aplusArg = (String) data.get("__aplus__");
    JSONObject object = new JSONObject(new JSONTokener(aplusArg));
    assertEquals(2, object.length());
    assertEquals(435, object.getInt("group"));
    assertEquals("fi", object.getString("lang"));
  }
}
