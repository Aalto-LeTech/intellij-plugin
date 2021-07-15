package fi.aalto.cs.apluscourses.presentation.base;

import fi.aalto.cs.apluscourses.presentation.filter.Options;
import fi.aalto.cs.apluscourses.utils.Event;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BaseTreeViewModel<T> extends SelectableNodeViewModel<T> {

  @NotNull
  public final Event filtered = new Event();
  @NotNull
  protected final Options options;
  @Nullable
  private Thread filterThread = null;
  @NotNull
  private final Object filterLock = new Object();
  @Nullable
  protected volatile SelectableNodeViewModel<?> selectedItem = null; //  NOSONAR

  public void setSelectedItem(@Nullable SelectableNodeViewModel<?> selectedItem) {
    this.selectedItem = selectedItem;
  }

  public @Nullable SelectableNodeViewModel<?> getSelectedItem() {
    return selectedItem;
  }

  /**
   * Base class for tree view models.
   */
  public BaseTreeViewModel(@NotNull T model,
                           @Nullable List<SelectableNodeViewModel<?>> children,
                           @NotNull Options options) {
    super(model, children);
    this.options = options;
    this.options.optionsChanged.addListener(this, BaseTreeViewModel::filter);
    filter();
  }

  protected void filter() {
    synchronized (filterLock) {
      filterThread = new FilterThread(filterThread);
      filterThread.start();
    }
  }

  @NotNull
  public Options getFilterOptions() {
    return options;
  }

  @Override
  public long getId() {
    return 0;
  }

  private class FilterThread extends Thread {
    @Nullable
    private final Thread previous;

    public FilterThread(@Nullable Thread previous) {
      this.previous = previous;
    }

    @Override
    public void run() {
      try {
        if (previous != null) {
          previous.interrupt();
          previous.join();
        }
        applyFilter(options);
        if (done()) {
          filtered.trigger();
        }
      } catch (InterruptedException e) {
        interrupt();
      }
    }

    private boolean done() {
      synchronized (filterLock) {
        if (filterThread == this) {
          filterThread = null;
          return true;
        }
      }
      return false;
    }
  }

  @NotNull
  public Selection findSelected() {
    return new Selection(traverseAndFind(SelectableNodeViewModel::isSelected));
  }

  public static class Selection {
    protected final List<SelectableNodeViewModel<?>> path;

    public Selection(SelectableNodeViewModel<?>... pathToSelected) {
      this(List.of(pathToSelected));
    }

    public Selection(@Nullable List<SelectableNodeViewModel<?>> pathToSelected) {
      path = pathToSelected;
    }

    @Nullable
    public SelectableNodeViewModel<?> getLevel(int level) {
      return path != null && level < path.size() ? path.get(level) : null;
    }
  }
}
