package fi.aalto.cs.apluscourses.intellij.model;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import com.intellij.util.concurrency.annotations.RequiresWriteLock;
import fi.aalto.cs.apluscourses.intellij.services.PluginSettings;
import fi.aalto.cs.apluscourses.intellij.utils.ListDependenciesPolicy;
import fi.aalto.cs.apluscourses.intellij.utils.VfsUtil;
import fi.aalto.cs.apluscourses.model.ComponentLoadException;
import fi.aalto.cs.apluscourses.model.Module;
import fi.aalto.cs.apluscourses.utils.Version;
import fi.aalto.cs.apluscourses.utils.content.RemoteZippedDir;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class IntelliJModule
    extends Module
    implements IntelliJComponent<com.intellij.openapi.module.Module> {

  @NotNull
  private final APlusProject project;

  IntelliJModule(@NotNull String name,
                 @NotNull URL url,
                 @NotNull String changelog,
                 @NotNull Version version,
                 @Nullable Version localVersion,
                 @Nullable ZonedDateTime downloadedAt,
                 @NotNull APlusProject project,
                 @NotNull String originalName) {
    super(name, url, changelog, version, localVersion, downloadedAt, originalName);
    this.project = project;
  }

  IntelliJModule(@NotNull String name,
                 @NotNull URL url,
                 @NotNull String changelog,
                 @NotNull Version version,
                 @Nullable Version localVersion,
                 @Nullable ZonedDateTime downloadedAt,
                 @NotNull APlusProject project) {
    this(name, url, changelog, version, localVersion, downloadedAt, project, name);
  }

  @NotNull
  @Override
  public Path getPath() {
    return Paths.get(name);
  }

  @NotNull
  @Override
  public Path getFullPath() {
    return project.getBasePath().resolve(getPath());
  }

  @Override
  public void fetchInternal() throws IOException {
    new RemoteZippedDir(getUrl().toString(), getOriginalName())
        .copyTo(getFullPath(), project.getProject());
    if (!getFullPath().resolve(getOriginalName() + ".iml").toFile().renameTo(getImlFile())) {
      throw new IOException("Could not rename iml file.");
    }
  }

  @Override
  protected int resolveStateInternal() {
    return project.resolveComponentState(this);
  }

  @Override
  public void load() throws ComponentLoadException {
    WriteAction.runAndWait(this::loadInternal);
  }

  @RequiresWriteLock
  private void loadInternal() throws ComponentLoadException {
    try {
      project.getModuleManager().loadModule(getImlFile().toPath());
      PluginSettings
          .getInstance()
          .getCourseFileManager(project.getProject())
          .addModuleEntry(this);
    } catch (Exception e) {
      throw new ComponentLoadException(getName(), e);
    }
  }

  @Override
  public void unload() {
    super.unload();
    WriteAction.runAndWait(this::unloadInternal);
  }

  @RequiresWriteLock
  private void unloadInternal() {
    com.intellij.openapi.module.Module module = getPlatformObject();
    if (module != null) {
      project.getModuleManager().disposeModule(module);
    }
  }

  @Override
  public void remove() throws IOException {
    FileUtils.deleteDirectory(getFullPath().toFile());
  }

  @Override
  public int getErrorCause() {
    return !this.getImlFile().exists() ? ERR_FILES_MISSING : super.getErrorCause();
  }

  @NotNull
  @Override
  protected List<String> computeDependencies() {
    ModuleRootManager moduleRootManager = project.getModuleRootManager(getName());
    if (moduleRootManager == null) {
      throw new IllegalStateException();
    }
    return Objects.requireNonNull(moduleRootManager
        .orderEntries()
        .withoutSdk()
        .withoutModuleSourceEntries()
        .productionOnly()
        .process(new ListDependenciesPolicy(), new ArrayList<>()));
  }

  @NotNull
  private File getImlFile() {
    return getFullPath().resolve(getName() + ".iml").toFile();
  }

  @NotNull
  public APlusProject getProject() {
    return project;
  }

  @Override
  @RequiresReadLock
  @Nullable
  public com.intellij.openapi.module.Module getPlatformObject() {
    return project.getModuleManager().findModuleByName(getName());
  }

  @Override
  protected boolean hasLocalChanges(@NotNull ZonedDateTime downloadedAt) {
    Path fullPath = getFullPath();
    long timeStamp = downloadedAt.toInstant().toEpochMilli()
        + PluginSettings.REASONABLE_DELAY_FOR_MODULE_INSTALLATION;
    return ReadAction.compute(() -> VfsUtil.hasDirectoryChanges(fullPath, timeStamp));
  }

  @Override
  public Module copy(@NotNull String newName) {
    return new IntelliJModule(newName, url, changelog, version, localVersion, downloadedAt, project, originalName);
  }
}
