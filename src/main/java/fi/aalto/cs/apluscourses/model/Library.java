package fi.aalto.cs.apluscourses.model;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public abstract class Library extends Component {

  public Library(@NotNull String name) {
    super(name);
  }

  @NotNull
  @Override
  public List<String> getDependencies() {
    return Collections.emptyList();
  }
}