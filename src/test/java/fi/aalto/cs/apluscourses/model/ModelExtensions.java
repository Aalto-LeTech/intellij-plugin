package fi.aalto.cs.apluscourses.model;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelExtensions {

  private static final Logger logger = LoggerFactory.getLogger(ModelExtensions.class);

  private ModelExtensions() {

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
    public boolean isUpToDate() {
      return true;
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
      this(name, testURL, "", null, null);
    }

    public TestModule(@NotNull String name,
                      @NotNull URL url,
                      @NotNull String versionId,
                      @Nullable String localVersionId,
                      @Nullable ZonedDateTime downloadedAt) {
      super(name, url, versionId, localVersionId, downloadedAt);
    }

    @NotNull
    @Override
    public Path getPath() {
      return Paths.get(name);
    }

    @Override
    public void fetchInternal() throws IOException {
      // do nothing
    }

    @Override
    public void load() throws ComponentLoadException {
      // do nothing
    }

    @Nullable
    @Override
    protected String readVersionId() {
      return null;
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

    @Override
    public boolean hasLocalChanges(@NotNull ZonedDateTime downloadedAt) {
      return false;
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
    public void fetch() throws IOException {
      // do nothing
    }

    @Override
    public void load() throws ComponentLoadException {
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
    public Course createCourse(@NotNull String name,
                               @NotNull List<Module> modules,
                               @NotNull List<Library> libraries,
                               @NotNull Map<String, String> requiredPlugins,
                               @NotNull Map<String, URL> resourceUrls) {
      return new Course(name, modules, libraries, requiredPlugins, resourceUrls);
    }

    @Override
    public Module createModule(@NotNull String name, @NotNull URL url, @NotNull String versionId) {
      return new TestModule(name, url, versionId, null, null);
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
