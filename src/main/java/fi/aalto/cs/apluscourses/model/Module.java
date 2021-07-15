package fi.aalto.cs.apluscourses.model;

import fi.aalto.cs.apluscourses.utils.Version;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

public abstract class Module extends Component {

  @NotNull
  protected final URL url;

  @NotNull
  protected Version version;
  @Nullable
  protected Version localVersion;
  @NotNull
  protected String changelog;
  @Nullable
  protected ZonedDateTime downloadedAt;
  @NotNull
  protected String originalName;

  /* synchronize with this when accessing variable fields of this class */
  private final Object moduleLock = new Object();

  /**
   * Constructs a module with the given name and URL.
   *
   * @param name         The name of the module.
   * @param url          The URL from which the module can be downloaded.
   * @param changelog    A string containing changes for an update.
   * @param version      A version number that uniquely identifies different versions of the same
   *                     module.
   * @param localVersion The current downloaded version number.
   * @param downloadedAt The time when the module was downloaded.
   */
  protected Module(@NotNull String name,
                   @NotNull URL url,
                   @NotNull String changelog,
                   @NotNull Version version,
                   @Nullable Version localVersion,
                   @Nullable ZonedDateTime downloadedAt,
                   @NotNull String originalName) {
    super(name);
    this.url = url;
    this.version = version;
    this.localVersion = localVersion;
    this.changelog = changelog;
    this.downloadedAt = downloadedAt;
    this.originalName = originalName;
  }

  protected Module(@NotNull String name,
                   @NotNull URL url,
                   @NotNull String changelog,
                   @NotNull Version version,
                   @Nullable Version localVersion,
                   @Nullable ZonedDateTime downloadedAt) {
    this(name, url, changelog, version, localVersion, downloadedAt, name);
  }

  /**
   * Returns a module constructed from the given JSON object. The object should contain the name of
   * the module and the URL from which the module can be downloaded. The object may optionally also
   * contain a version and a changelog. Additional members in the object don't cause any errors.
   * Example of a valid JSON object:
   * <pre>
   * {
   *   "name": "My Module",
   *   "url": "https://example.com",
   *   "version": "2.1",
   *   "changelog": "changelog here"
   * }
   * </pre>
   *
   * @param jsonObject The JSON object containing information about a single module.
   * @param factory    A {@link ModelFactory} object that is responsible for actual object
   *                   creation.
   * @return A module constructed from the given JSON object.
   * @throws MalformedURLException  If the URL of the module is malformed.
   */
  @NotNull
  public static Module fromJsonObject(@NotNull JSONObject jsonObject, @NotNull ModelFactory factory)
      throws MalformedURLException {
    String name = jsonObject.getString("name");
    URL url = new URL(jsonObject.getString("url"));
    Version version = Version.fromString(jsonObject.optString("version", "1.0"));
    String changelog = jsonObject.optString("changelog", "");
    return factory.createModule(name, url, version, changelog);
  }

  @Override
  public void fetch() throws IOException {
    fetchInternal();
    synchronized (moduleLock) {
      downloadedAt = ZonedDateTime.now();
      localVersion = version;
    }
  }

  /**
   * Tells whether or not the module is updatable.
   *
   * @return True, if the module is loaded and the local version is not the newest one; otherwise
   *         false.
   */
  @Override
  public boolean isUpdatable() {
    if (stateMonitor.get() != LOADED) {
      return false;
    }
    synchronized (moduleLock) {
      return !version.equals(localVersion);
    }
  }

  /**
   * Returns true if the major version number has changed.
   */
  public boolean isMajorUpdate() {
    if (stateMonitor.get() != LOADED) {
      return false;
    }
    synchronized (moduleLock) {
      return version.major != Optional.ofNullable(localVersion).orElse(new Version(1, 0)).major;
    }
  }

  protected abstract void fetchInternal() throws IOException;

  @NotNull
  public URL getUrl() {
    return url;
  }

  /**
   * Tells whether or not the module has local changes.
   *
   * @return True if there are local changes, otherwise false.
   */
  @Override
  public boolean hasLocalChanges() {
    ZonedDateTime downloadedAtVal;
    synchronized (moduleLock) {
      downloadedAtVal = this.downloadedAt;
    }
    if (downloadedAtVal == null) {
      return false;
    }
    return hasLocalChanges(downloadedAtVal);
  }

  protected abstract boolean hasLocalChanges(@NotNull ZonedDateTime downloadedAt);

  /**
   * Returns metadata (data that should be stored locally) of the module.
   *
   * @return A {@link ModuleMetadata} object.
   */
  @NotNull
  public ModuleMetadata getMetadata() {
    synchronized (moduleLock) {
      return new ModuleMetadata(Optional.ofNullable(localVersion).orElse(version), downloadedAt);
    }
  }

  /**
   * Returns the version ID (not local).
   *
   * @return Version ID.
   */
  public Version getVersion() {
    synchronized (moduleLock) {
      return version;
    }
  }

  @NotNull
  public String getChangelog() {
    return changelog;
  }

  /**
   * Updates the non-local version. Returns true if the version changed, false otherwise.
   */
  public boolean updateVersion(@NotNull Version newVersion) {
    synchronized (moduleLock) {
      boolean changed = !version.equals(newVersion);
      if (changed) {
        version = newVersion;
      }
      return changed;
    }
  }

  /**
   * Updates the changelog.
   */
  public void updateChangelog(@NotNull String newChangelog) {
    synchronized (moduleLock) {
      changelog = newChangelog;
    }
  }

  public abstract Module copy(@NotNull String newName);

  @Override
  public @NotNull String getOriginalName() {
    return originalName;
  }

}
