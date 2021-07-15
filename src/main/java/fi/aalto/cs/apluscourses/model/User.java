package fi.aalto.cs.apluscourses.model;

import org.jetbrains.annotations.NotNull;

public class User {
  @NotNull
  private final Authentication authentication;

  @NotNull
  private final String userName;

  /**
   * A constructor.
   */
  public User(@NotNull Authentication authentication,
              @NotNull String userName) {
    this.authentication = authentication;
    this.userName = userName;
  }

  @NotNull
  public Authentication getAuthentication() {
    return authentication;
  }

  @NotNull
  public String getUserName() {
    return userName;
  }
}
