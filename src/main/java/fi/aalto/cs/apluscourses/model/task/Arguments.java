package fi.aalto.cs.apluscourses.model.task;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;


@FunctionalInterface
public interface Arguments {
  @Nullable Object get(@NotNull String key);

  /**
   * Gets an argument or throws a runtime exception if the argument is missing.
   * @param key Key
   * @return The (non-null) value of the argument.
   */
  default @NotNull String getOrThrow(@NotNull String key) {
    if (!(get(key) instanceof String)) {
      throw new IllegalArgumentException("Argument is not a String: " + key);
    }
    String value = (String) get(key);
    if (value == null) {
      throw new IllegalArgumentException("Argument missing: " + key);
    }
    return value;
  }

  /**
   * Gets an argument or throws a runtime exception if the argument is missing.
   * @param key Key
   * @return The (non-null) array values of the argument.
   */
  default @NotNull String[] getArrayOrThrow(@NotNull String key) {
    if (! (get(key) instanceof JSONArray)) {
      throw new IllegalArgumentException("Argument is not a JSONArray: " + key);
    }
    JSONArray jsonArray = (JSONArray) get(key);
    if (jsonArray == null) {
      throw new IllegalArgumentException("Argument missing: " + key);
    }

    List<String> array = new ArrayList<>();
    int length = jsonArray.length();
    for (int i = 0; i < length; i++) {
      array.add(jsonArray.getString(i));
    }
    return array.toArray(new String[]{});
  }

  static @NotNull Arguments empty() {
    return key -> null;
  }
}
