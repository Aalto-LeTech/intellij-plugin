package fi.aalto.cs.apluscourses.model;

import fi.aalto.cs.apluscourses.utils.BuildInfo;
import fi.aalto.cs.apluscourses.utils.Version;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelExtensions {

  private static final Logger logger = LoggerFactory.getLogger(ModelExtensions.class);

  private ModelExtensions() {

  }

  public static class TestExerciseDataSource implements ExerciseDataSource {

    @Override
    public <T> List<T> getPaginatedResults(@NotNull String url,
                                           @NotNull Authentication authentication,
                                           @Nullable ZonedDateTime zonedDateTime,
                                           @NotNull Function<JSONObject, T> parseFunction) {
      return null;
    }

    @Override
    public <T> List<T> getPaginatedResults(@NotNull String url,
                                           @NotNull Authentication authentication,
                                           @NotNull Function<JSONObject, T> parseFunction) {
      return null;
    }

    @NotNull
    @Override
    public List<Group> getGroups(@NotNull Course course, @NotNull Authentication authentication) {
      return Collections.singletonList(new Group(0, Collections.singletonList("Only you")));
    }

    @NotNull
    @Override
    public List<ExerciseGroup> getExerciseGroups(@NotNull Course course,
                                                 @NotNull Authentication authentication) {
      return Collections.emptyList();
    }

    @NotNull
    @Override
    public Points getPoints(@NotNull Course course, @NotNull Authentication authentication) {
      return new Points(
          Collections.emptyMap(),
          Collections.emptyMap(),
          Collections.emptyMap()
      );
    }

    @Override
    public @NotNull Points getPoints(@NotNull Course course,
                                     @NotNull Authentication authentication,
                                     @Nullable Student student) {
      return new Points(
          Collections.emptyMap(),
          Collections.emptyMap(),
          Collections.emptyMap());
    }

    @NotNull
    @Override
    public Exercise getExercise(long exerciseId,
                                @NotNull Points points,
                                @NotNull Map<Long, Tutorial> tutorials,
                                @NotNull Authentication authentication,
                                @NotNull ZonedDateTime minCacheEntryTime) {
      return new Exercise(1, "lol", "http://example.com",
          new SubmissionInfo(Collections.emptyMap()), 20, 10, OptionalLong.empty()
      );
    }

    @NotNull
    @Override
    public SubmissionResult getSubmissionResult(@NotNull String submissionUrl,
                                                @NotNull Exercise exercise,
                                                @NotNull Authentication authentication,
                                                @NotNull ZonedDateTime minCacheEntryTime) {
      return new SubmissionResult(0, 20, 0.0, SubmissionResult.Status.GRADED, exercise);
    }

    @Override
    public @NotNull User getUser(@NotNull Authentication authentication) {
      return new User(authentication, "test");
    }

    @Override
    @NotNull
    public List<Student> getStudents(@NotNull Course course,
                                     @NotNull Authentication authentication,
                                     @NotNull ZonedDateTime minCacheEntryTime) {
      return new ArrayList<>();
    }

    @Override
    public @NotNull ZonedDateTime getEndingTime(@NotNull Course course,
                                                @NotNull Authentication authentication) {
      return ZonedDateTime.of(2020, 1, 2, 0, 0, 0, 0,
          ZoneId.systemDefault());
    }

    @Override
    public String submit(@NotNull Submission submission, @NotNull Authentication authentication) {
      // do nothing
      return "";
    }
  }

  public static class TestCourse extends Course {

    private final ExerciseDataSource exerciseDataSource;

    /**
     * Constructor matching superclass.
     */
    public TestCourse(@NotNull String id,
                      @NotNull String name,
                      @NotNull String aplusUrl,
                      @NotNull List<String> languages,
                      @NotNull List<Module> modules,
                      @NotNull List<Library> libraries,
                      @NotNull Map<Long, Map<String, String>> exerciseModules,
                      @NotNull Map<String, URL> resourceUrls,
                      @NotNull List<String> autoInstallComponentNames,
                      @NotNull Map<String, String[]> replInitialCommands,
                      @NotNull Version courseVersion,
                      @NotNull Map<Long, Tutorial> tutorials) {
      super(id, name, aplusUrl, languages, modules, libraries, exerciseModules, resourceUrls,
          autoInstallComponentNames, replInitialCommands, courseVersion, tutorials);
      exerciseDataSource = new TestExerciseDataSource();
    }

    public TestCourse(@NotNull String id) {
      this(id, "", new TestExerciseDataSource());
    }

    /**
     * Creates a dummy {@link Course} for testing purposes.
     *
     * @param id                 {@link String} id for the {@link Course}
     * @param name               {@link String} for the {@link Course}.
     * @param exerciseDataSource Data source for exercises.
     */
    public TestCourse(@NotNull String id, @NotNull String name,
                      @NotNull ExerciseDataSource exerciseDataSource) {
      super(
          id,
          name,
          "https://example.com/",
          Collections.emptyList(),
          //  modules
          Collections.emptyList(),
          //  libraries
          Collections.emptyList(),
          //  exerciseModules
          Collections.emptyMap(),
          //  resourceUrls
          Collections.emptyMap(),
          //  autoInstallComponentNames
          Collections.emptyList(),
          //  replInitialCommands
          Collections.emptyMap(),
          //  courseVersion
          BuildInfo.INSTANCE.courseVersion,
          // tutorials
          Collections.emptyMap());
      this.exerciseDataSource = exerciseDataSource;
    }

    @NotNull
    @Override
    public ExerciseDataSource getExerciseDataSource() {
      return exerciseDataSource;
    }
  }

  public static class TestComponent extends Component {

    public TestComponent() {
      super("");
    }

    public TestComponent(@NotNull String name) {
      super(name);
    }

    @NotNull
    @Override
    public Path getPath() {
      return Paths.get(getName());
    }

    @Override
    public void fetch() {
      // do nothing
    }

    @Override
    public void load() {
      // do nothing
    }

    @NotNull
    @Override
    public Path getFullPath() {
      return getPath().toAbsolutePath();
    }

    @Override
    protected int resolveStateInternal() {
      return 0;
    }

    @NotNull
    @Override
    protected List<String> computeDependencies() {
      return Collections.emptyList();
    }

    @Override
    public boolean isUpdatable() {
      return false;
    }

    @Override
    public boolean hasLocalChanges() {
      return false;
    }
  }

  public static class TestModule extends Module {

    private static URL testURL;

    static {
      try {
        testURL = new URL("https://example.com");
      } catch (MalformedURLException e) {
        logger.error("Test URL is malformed", e);
      }
    }

    public TestModule(@NotNull String name) {
      this(name, testURL, new Version(1, 0), null, "changes", null);
    }

    public TestModule(@NotNull String name,
                      @NotNull URL url,
                      @NotNull Version version,
                      @Nullable Version localVersion,
                      @NotNull String changelog,
                      @Nullable ZonedDateTime downloadedAt) {
      super(name, url, changelog, version, localVersion, downloadedAt);
    }

    @NotNull
    @Override
    public Path getPath() {
      return Paths.get(name);
    }

    @Override
    public void fetchInternal() {
      // do nothing
    }

    @Override
    public void load() throws ComponentLoadException {
      // do nothing
    }

    @Override
    protected boolean hasLocalChanges(@NotNull ZonedDateTime downloadedAt) {
      return false;
    }

    @Override
    public Module copy(@NotNull String newName) {
      return new TestModule(newName, url, version, localVersion, changelog, downloadedAt);
    }

    @NotNull
    @Override
    public Path getFullPath() {
      return getPath().toAbsolutePath();
    }

    @Override
    protected int resolveStateInternal() {
      return Component.NOT_INSTALLED;
    }

    @NotNull
    @Override
    protected List<String> computeDependencies() {
      return Collections.emptyList();
    }
  }

  public static class TestLibrary extends Library {

    public TestLibrary(@NotNull String name) {
      super(name);
    }

    @NotNull
    @Override
    public Path getPath() {
      return Paths.get("lib", name);
    }

    @Override
    public void fetch() {
      // do nothing
    }

    @Override
    public void load() {
      // do nothing
    }

    @NotNull
    @Override
    public Path getFullPath() {
      return getPath().toAbsolutePath();
    }

    @Override
    protected int resolveStateInternal() {
      return Component.NOT_INSTALLED;
    }
  }

  public static class TestModelFactory implements ModelFactory {

    @Override
    public Course createCourse(@NotNull String id,
                               @NotNull String name,
                               @NotNull String aplusUrl,
                               @NotNull List<String> languages,
                               @NotNull List<Module> modules,
                               @NotNull List<Library> libraries,
                               @NotNull Map<Long, Map<String, String>> exerciseModules,
                               @NotNull Map<String, URL> resourceUrls,
                               @NotNull List<String> autoInstallComponentNames,
                               @NotNull Map<String, String[]> replInitialCommands,
                               @NotNull Version courseVersion,
                               @NotNull Map<Long, Tutorial> tutorials) {
      return new ModelExtensions.TestCourse(
          id,
          name,
          aplusUrl,
          languages,
          modules,
          libraries,
          exerciseModules,
          resourceUrls,
          autoInstallComponentNames,
          replInitialCommands,
          courseVersion,
          tutorials
      );
    }

    @Override
    public Module createModule(@NotNull String name,
                               @NotNull URL url,
                               @NotNull Version version,
                               @NotNull String changelog) {
      return new TestModule(name, url, version, null, changelog, null);
    }

    @Override
    public Library createLibrary(@NotNull String name) {
      throw new UnsupportedOperationException("Only common libraries are supported.");
    }
  }

  public static class TestComponentSource implements ComponentSource {

    @Nullable
    @Override
    public Component getComponentIfExists(@NotNull String componentName) {
      return null;
    }
  }
}
