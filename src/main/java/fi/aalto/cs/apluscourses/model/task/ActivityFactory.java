package fi.aalto.cs.apluscourses.model.task;

import org.jetbrains.annotations.NotNull;

public interface ActivityFactory {
  @NotNull ActivitiesListener createListener(@NotNull String action,
                                             @NotNull Arguments arguments,
                                             @NotNull ListenerCallback callback);

  @NotNull ComponentPresenter createPresenter(@NotNull String component,
                                              @NotNull String instruction,
                                              @NotNull String info,
                                              @NotNull Arguments componentArguments,
                                              @NotNull Arguments actionArguments);
}
