package fi.aalto.cs.intellij.notifications;

import fi.aalto.cs.intellij.common.Version;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class NotificationsUtilityTest {

  @Test
  public void testBetaVersionWarning() {
    BetaVersionWarning notification =
        new BetaVersionWarning(new Version(0, 22, 315));

    Assert.assertEquals("0.22.315", notification.getVersion().toString());
    Assert.assertEquals("A+", notification.getGroupId());
    Assert.assertEquals("A+ Courses plugin is under development", notification.getTitle());
    Assert.assertThat(notification.getContent(), CoreMatchers.containsString("0.22.315"));
  }
}
