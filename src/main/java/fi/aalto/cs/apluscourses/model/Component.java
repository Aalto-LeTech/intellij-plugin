package fi.aalto.cs.apluscourses.model;

import fi.aalto.cs.apluscourses.utils.Event;
import fi.aalto.cs.apluscourses.utils.StateMonitor;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public abstract class Component {
  public static final int NOT_INSTALLED = StateMonitor.INITIAL;
  public static final int FETCHING = NOT_INSTALLED + 1;
  public static final int FETCHED = FETCHING + 1;
  public static final int LOADING = FETCHED + 1;
  public static final int LOADED = LOADING + 1;
  public static final int UNINSTALLING = LOADED + 1;
  public static final int ERROR = StateMonitor.ERROR;
  public static final int UNRESOLVED = ERROR - 1;
  public static final int UNINSTALLED = UNRESOLVED - 1;
  public static final int ACTION_ABORTED = 99;

  public static final int DEP_INITIAL = StateMonitor.INITIAL;
  public static final int DEP_WAITING = DEP_INITIAL + 1;
  public static final int DEP_LOADED = DEP_WAITING + 1;
  public static final int DEP_ERROR = StateMonitor.ERROR;

  public static final int ERR_FILES_MISSING = StateMonitor.INITIAL;
  public static final int ERR_UNKNOWN = ERR_FILES_MISSING + 1;

  public final Event stateChanged = new Event();
  public final Event onError = new Event();

  public final StateMonitor stateMonitor = new StateMonitor(UNRESOLVED, this::onStateChanged);
  public final StateMonitor dependencyStateMonitor =
      new StateMonitor(DEP_INITIAL, this::onStateChanged);

  @NotNull
  protected final String name;

  private List<String> dependencies;

  protected Component(@NotNull String name) {
    this.name = name;
  }

  @NotNull
  public String getName() {
    return name;
  }

  @NotNull
  public String getOriginalName() {
    return getName();
  }

  @NotNull
  public abstract Path getPath();

  public abstract void fetch() throws IOException;

  public abstract void load() throws ComponentLoadException;

  public void unload() {
    dependencies = null;
  }

  public void remove() throws IOException {
    // subclasses may do their removal operations
  }

  protected void onStateChanged(int newState) {
    stateChanged.trigger();
    if (StateMonitor.isError(newState)) {
      onError.trigger();
    }
  }

  /**
   * Tells whether the component is in an error state.
   * @return True if error, otherwise false.
   */
  public boolean hasError() {
    int state = stateMonitor.get();
    return StateMonitor.isError(state) || state == LOADED && dependencyStateMonitor.hasError();
  }

  /**
   * Returns the names of the dependencies.  This method should not be called unless the component
   * is in LOADED state.
   *
   * @return Names of the dependencies, as a {@link List}.
   */
  @NotNull
  public List<String> getDependencies() {
    if (dependencies == null) {
      dependencies = computeDependencies();
    }
    return dependencies;
  }

  public int getErrorCause() {
    return ERR_UNKNOWN;
  }

  @NotNull
  public abstract Path getFullPath();

  protected abstract int resolveStateInternal();

  /**
   * If the state is UNRESOLVED, sets it to a state resolved by subclasses.
   */
  public void resolveState() {
    if (stateMonitor.get() == UNRESOLVED) {
      stateMonitor.setConditionallyTo(resolveStateInternal(), UNRESOLVED);
    }
  }

  @NotNull
  protected abstract List<String> computeDependencies();

  /**
   * Checks whether this component's dependencies are in LOADED state.
   * @param componentSource A component source which should have the dependencies of this component.
   * @return True if the dependencies are LOADED, otherwise false.
   */
  private boolean areDependenciesLoaded(ComponentSource componentSource) {
    return getDependencies().stream()
        .map(componentSource::getComponentIfExists)
        .allMatch(component -> component != null && component.stateMonitor.get() == LOADED);
  }

  /**
   * Sets component to the unresolved state, unless it is active.
   */
  public void setUnresolved() {
    stateMonitor.setConditionallyTo(UNRESOLVED,
        NOT_INSTALLED, FETCHED, LOADED, ERROR, UNINSTALLED, ACTION_ABORTED);
  }

  /**
   * Sets the component in DEP_ERROR state if it does not conform dependency integrity constraints.
   * Sets the component that is in DEP_ERROR state to DEP_LOADED state if dependency integrity
   * constraints are conformed.
   */
  public void validate(ComponentSource componentSource) {
    int depState;
    if (stateMonitor.get() == LOADED
        && (depState = dependencyStateMonitor.get()) != Component.DEP_WAITING) {
      dependencyStateMonitor.setConditionallyTo(
          areDependenciesLoaded(componentSource) ? DEP_LOADED : DEP_ERROR, depState);
    }
  }

  public abstract boolean isUpdatable();

  public abstract boolean hasLocalChanges();

  @FunctionalInterface
  public interface InitializationCallback {
    void initialize(Component component);
  }
}
