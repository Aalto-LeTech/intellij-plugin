package fi.aalto.cs.apluscourses.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class IdeActivitiesUtil {

  private IdeActivitiesUtil() {}

  /**
   * Separates the given text into a String[] by tokenizing with the specified delimeter.
   * @param phrase The String to be tokenized.
   * @param delim The delimeter used in the tokenizing.
   * @return The String[] containing the tokens.
   */
  public static String[] getSeparateWords(String phrase, String delim) {
    List<String> tokens = new ArrayList<>();
    StringTokenizer tokenizer = new StringTokenizer(phrase, delim);
    while (tokenizer.hasMoreTokens()) {
      tokens.add(tokenizer.nextToken().trim());
    }
    return tokens.toArray(new String[]{});
  }
}
