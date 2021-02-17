package fi.aalto.cs.apluscourses.utils;

import org.jetbrains.annotations.NotNull;

import java.util.function.IntFunction;

public class ArrayUtil {

  private ArrayUtil() {

  }

  /**
   * Maps array to array.
   *
   * @param source    The source array.
   * @param func      Mapping function that can throw an exception.
   * @param generator Use {@code R[]::new}.
   * @param <T>       Type of source elements.
   * @param <R>       Type of result elements.
   * @param <E>       Type of exception.
   * @return Mapped array.
   * @throws E if mapping function throws.
   */
  public static <T, R, E extends  Exception> R[] mapArray(@NotNull T[] source,
                                                          @NotNull ThrowingFunction<T, R, E> func,
                                                          @NotNull IntFunction<R[]> generator)
      throws E {
    R[] result = generator.apply(source.length);
    for (int i = 0; i < source.length; i++) {
      result[i] = func.apply(source[i]);
    }
    return result;
  }

  public interface ThrowingFunction<T, R, E extends Exception> {
    @NotNull
    R apply(T arg) throws E;
  }
}
