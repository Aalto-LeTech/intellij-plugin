package fi.aalto.cs.apluscourses.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import fi.aalto.cs.apluscourses.intellij.notifications.FeedbackAvailableNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.Notifier;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

public class SubmissionStatusUpdaterTest {

  class TestDataSource extends ModelExtensions.TestExerciseDataSource {

    private int limit = 3;
    private AtomicInteger submissionResultFetchCount = new AtomicInteger(0);

    public int getSubmissionResultFetchCount() {
      return submissionResultFetchCount.get();
    }

    @NotNull
    @Override
    public SubmissionResult getSubmissionResult(@NotNull String submissionUrl,
                                                @NotNull Authentication authentication)
        throws IOException {

      if (submissionResultFetchCount.incrementAndGet() >= limit) {
        return new SubmissionResult(
          123L,
          SubmissionResult.Status.GRADED,
          "https://example.org"
        );
      } else {
        return new SubmissionResult(
            123L,
            SubmissionResult.Status.UNKNOWN,
            "https://example.com/"

        );
      }
    }
  }

  private TestDataSource dataSource;
  private Notifier notifier;

  @Before
  public void setUp() {
    dataSource = new TestDataSource();
    notifier = mock(Notifier.class);
  }

  @Test
  public void testSubmissionStatusUpdater() throws InterruptedException {
    new SubmissionStatusUpdater(
        dataSource,
        mock(Authentication.class),
        notifier,
        "http://localhost:1000",
        "Cool Exercise Name",
        25L, // 0.025 seconds
        10000L
    ).start();
    Thread.sleep(800L);

    assertEquals("The submission results are not fetched anymore after feedback is available",
        3, dataSource.getSubmissionResultFetchCount());
    verify(notifier).notify(any(FeedbackAvailableNotification.class), isNull());
  }

  @Test
  public void testSubmissionStatusUpdaterTimeLimit() throws InterruptedException {
    dataSource.limit = 9999;
    new SubmissionStatusUpdater(
        dataSource,
        mock(Authentication.class),
        notifier,
        "http://localhost:1000",
        "Cool Exercise Name",
        25L, // 0.025 seconds
        200L
    ).start();
    Thread.sleep(800L);
    assertTrue(dataSource.getSubmissionResultFetchCount() <= 8);
    verifyNoInteractions(notifier);
  }

}
