package fi.aalto.cs.apluscourses.ui.base;

import com.intellij.ui.components.JBList;
import fi.aalto.cs.apluscourses.ui.utils.TwoWayBindable;
import java.util.List;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jetbrains.annotations.NotNull;

public class SingleSelectionList<T> extends JBList<T> {
  public final transient TwoWayBindable<SingleSelectionList<T>, T> selectionBindable =
      new TwoWayBindable<>(this, SingleSelectionList::setSelectedValue, SingleSelectionList::getSelectedValue);

  /**
   * Constructs a new SingleSelectionListener.
   * @param items A list of items.
   */
  public SingleSelectionList(@NotNull List<T> items) {
    super(items);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    addListSelectionListener(new SelectionListener());
  }

  public void setSelectedValue(Object anObject) {
    setSelectedValue(anObject, true);
  }

  private class SelectionListener implements ListSelectionListener {
    @Override
    public void valueChanged(ListSelectionEvent e) {
      selectionBindable.updateSource();
    }
  }
}
