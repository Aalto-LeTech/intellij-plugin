package fi.aalto.cs.apluscourses.utils;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class ListUtil {
  private ListUtil() {

  }

  /**
   * Appends two lists onto a new one.
   */
  public static <T> List<T> appendTwoLists(@NotNull List<T> first, @NotNull List<T> second) {
    var both = new ArrayList<T>(first.size() + second.size());
    both.addAll(first);
    both.addAll(second);
    return both;
  }

}
