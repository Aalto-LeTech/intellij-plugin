package fi.aalto.cs.apluscourses.presentation.base;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An abstract extension to {@link ListModel} that co-operates with {@link ListElementViewModel}.
 * Provides the following functionality:
 * <ul>
 * <li>selection state to be stored inside the element objects</li>
 * <li>the view to be informed about changes in the presentation model</li>
 * </ul>
 * @param <E> Type of the list elements (presentation models), subtype of
 *            {@link ListElementViewModel}
 */
public class BaseListViewModel<E extends ListElementViewModel<?>> extends AbstractListModel<E>
    implements SelectableListModel<E> {

  @NotNull
  private final ListSelectionModel selectionModel;

  @NotNull
  protected final List<E> elements;

  /**
   * A constructor.
   * @param models                      A List of model elements.
   * @param listElementViewModelFactory A function that creates a list element view model object of
   *                                    a model object.
   */
  public <M> BaseListViewModel(@NotNull List<M> models,
                               @NotNull Function<M, E> listElementViewModelFactory) {
    this.selectionModel = new SelectionModel();
    this.elements = new ArrayList<>(models.size());
    int index = 0;
    for (M model : models) {
      E element = listElementViewModelFactory.apply(model);
      element.setListModel(this);
      element.setIndex(index++);
      elements.add(element);
    }
  }

  public void onElementChanged(int index) {
    fireContentsChanged(this, index, index);
  }

  /**
   * Get currently selected elements as a list.  The list is a snapshot of the current selection
   * state and is not updated after this method call.
   *
   * @return A {@link List}.
   */
  public List<E> getSelectedElements() {
    return elements
        .stream()
        .filter(ListElementViewModel::isSelected)
        .collect(Collectors.toList());
  }

  @Override
  @NotNull
  public ListSelectionModel getSelectionModel() {
    return selectionModel;
  }

  @Override
  public int getSize() {
    return elements.size();
  }

  @Override
  @Nullable
  public E getElementAt(int i) {
    try {
      return elements.get(i);
    } catch (IndexOutOfBoundsException e) {
      return null;
    }
  }

  private class SelectionModel extends DefaultListSelectionModel implements ListSelectionListener {

    public SelectionModel() {
      addListSelectionListener(this);
    }

    @Override
    public void valueChanged(ListSelectionEvent selectionEvent) {
      int firstIndex = selectionEvent.getFirstIndex();
      int lastIndex = selectionEvent.getLastIndex();
      for (int index = firstIndex; index <= lastIndex; index++) {
        E element = BaseListViewModel.this.getElementAt(index);
        if (element != null) {
          boolean selected = isSelectedIndex(index);
          element.setSelected(selected);
        }
      }
    }
  }
}
