package fi.aalto.cs.apluscourses.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class MapUtil {
  private MapUtil() {

  }

  public static <K, V> int mapValueListsSize(@NotNull Map<K, List<V>> map) {
    return map.values().stream().mapToInt(Collection::size).sum();
  }
}
