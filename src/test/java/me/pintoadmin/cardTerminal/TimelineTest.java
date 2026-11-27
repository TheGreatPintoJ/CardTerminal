package me.pintoadmin.cardTerminal;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class TimelineTest {

    @Test
    void afterRunsLaterWithoutBlockingCaller() throws Exception {
        try (Timeline timeline = new Timeline("test")) {
            CountDownLatch ran = new CountDownLatch(1);
            long start = System.nanoTime();
            timeline.after(200, ran::countDown);
            long callMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            assertTrue(callMs < 50, "after() should return immediately, took " + callMs + "ms");
            assertTrue(ran.await(2, TimeUnit.SECONDS));
            long totalMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue(totalMs >= 190, "task ran too early: " + totalMs + "ms");
        }
    }

    @Test
    void sequenceRunsStepsInOrderWithPauses() throws Exception {
        try (Timeline timeline = new Timeline("test")) {
            List<String> events = new CopyOnWriteArrayList<>();
            CountDownLatch done = new CountDownLatch(1);
            long start = System.nanoTime();

            timeline.sequence()
                    .then(() -> events.add("a"))
                    .pause(150)
                    .frames(3, 20, f -> events.add("f" + f))
                    .then(() -> events.add("b"))
                    .then(done::countDown)
                    .start();

            assertTrue(done.await(2, TimeUnit.SECONDS));
            long totalMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertEquals(List.of("a", "f0", "f1", "f2", "b"), events);
            assertTrue(totalMs >= 180, "pauses not respected: " + totalMs + "ms");
        }
    }

    @Test
    void cancelledSequenceStops() throws Exception {
        try (Timeline timeline = new Timeline("test")) {
            List<String> events = new CopyOnWriteArrayList<>();
            Timeline.Handle handle =
                    timeline.sequence()
                            .then(() -> events.add("a"))
                            .pause(200)
                            .then(() -> events.add("b"))
                            .start();
            Thread.sleep(50);
            handle.cancel();
            Thread.sleep(300);
            assertEquals(List.of("a"), events);
        }
    }

    @Test
    void closeDropsPendingTasks() throws Exception {
        Timeline timeline = new Timeline("test");
        CountDownLatch ran = new CountDownLatch(1);
        timeline.after(100, ran::countDown);
        timeline.close();
        assertFalse(ran.await(300, TimeUnit.MILLISECONDS));
        assertNull(timeline.after(0, ran::countDown), "closed timeline should not accept tasks");
    }

    @Test
    void failingTaskDoesNotKillTimeline() throws Exception {
        try (Timeline timeline = new Timeline("test")) {
            timeline.after(
                    0,
                    () -> {
                        throw new IllegalStateException("expected in test");
                    });
            CountDownLatch ran = new CountDownLatch(1);
            timeline.after(10, ran::countDown);
            assertTrue(ran.await(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void sleepReportsInterrupt() {
        Thread.currentThread().interrupt();
        assertFalse(Timeline.sleep(10));
        assertTrue(Thread.interrupted()); // flag restored (and cleared here)
        assertTrue(Timeline.sleep(1));
    }
}
