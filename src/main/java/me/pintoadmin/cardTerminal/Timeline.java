package me.pintoadmin.cardTerminal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

/**
 * Non-blocking delays for animations, pauses between game steps, and other timed work.
 *
 * <p>A {@code Timeline} owns one background daemon thread. Every task scheduled on it runs on that
 * thread, one at a time, in the order its delay expires. That means the thread that asked for the
 * delay (the key-handler thread, a socket thread, a bot thread) is never blocked, and no locks are
 * held while waiting.
 *
 * <pre>{@code
 * Timeline timeline = new Timeline("Hearts UI");
 *
 * // Run something once, later:
 * timeline.after(500, this::resize);
 *
 * // Chain steps with pauses between them:
 * timeline.sequence()
 *         .then(() -> highlight = true)
 *         .then(this::resize)
 *         .pause(300)
 *         .frames(8, 40, frame -> { slideOffset = frame; resize(); })
 *         .then(() -> highlight = false)
 *         .start();
 *
 * // Stop everything (e.g. when the screen is closed):
 * timeline.close();
 * }</pre>
 *
 * <p>Tasks that touch shared state must still synchronize on that state themselves; the timeline
 * only decides <i>when</i> code runs, not what it may access. Exceptions thrown by a task are
 * logged and do not stop the timeline.
 */
public final class Timeline implements AutoCloseable {
    private final ScheduledThreadPoolExecutor executor;
    private volatile Thread thread;

    public Timeline(String name) {
        executor =
                new ScheduledThreadPoolExecutor(
                        1,
                        runnable -> {
                            Thread t = new Thread(runnable, name);
                            t.setDaemon(true);
                            thread = t;
                            return t;
                        });
        // On close(), drop anything still waiting instead of running it.
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        executor.setRemoveOnCancelPolicy(true);
    }

    /**
     * Runs {@code task} once on the timeline thread after {@code delayMs} milliseconds. A delay of
     * zero or less runs it as soon as the thread is free.
     *
     * @return a handle that can cancel the task before it runs; {@code null} if the timeline is
     *     closed
     */
    public ScheduledFuture<?> after(long delayMs, Runnable task) {
        if (executor.isShutdown()) return null;
        return executor.schedule(guard(task), Math.max(0, delayMs), TimeUnit.MILLISECONDS);
    }

    /** Starts building a chain of steps and pauses. Nothing runs until {@link Sequence#start()}. */
    public Sequence sequence() {
        return new Sequence();
    }

    /** Cancels every task that is waiting to run. A task that is already running finishes. */
    public void cancelAll() {
        executor.getQueue().forEach(r -> ((ScheduledFuture<?>) r).cancel(false));
        executor.purge();
    }

    /** Whether the calling code is running on this timeline's thread. */
    public boolean isTimelineThread() {
        return Thread.currentThread() == thread;
    }

    public boolean isClosed() {
        return executor.isShutdown();
    }

    /**
     * Cancels everything that is waiting and stops the thread once the current task (if any)
     * finishes. Safe to call from inside a timeline task, and safe to call more than once.
     */
    @Override
    public void close() {
        executor.shutdown();
    }

    /**
     * Blocks the <b>calling</b> thread for {@code ms} milliseconds. Only use this on a thread that
     * exists to wait (such as a bot loop); never on the key-handler thread or while holding a lock
     * other threads need. Restores the interrupt flag and returns {@code false} if interrupted, so
     * loops can stop cleanly.
     */
    public static boolean sleep(long ms) {
        if (ms <= 0) return !Thread.currentThread().isInterrupted();
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static Runnable guard(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                System.err.println("Timeline task failed: " + e);
                e.printStackTrace();
            }
        };
    }

    /** A chain of steps that run one after another, with optional pauses between them. */
    public final class Sequence {
        private final List<Step> steps = new ArrayList<>();

        private record Step(long delayMs, Runnable action) {}

        private Sequence() {}

        /** Runs {@code action} right after the previous step. */
        public Sequence then(Runnable action) {
            steps.add(new Step(0, action));
            return this;
        }

        /** Waits {@code ms} milliseconds before the next step. */
        public Sequence pause(long ms) {
            steps.add(new Step(Math.max(0, ms), () -> {}));
            return this;
        }

        /**
         * Calls {@code frame} {@code count} times with frame numbers {@code 0..count-1}, waiting
         * {@code intervalMs} between frames. Useful for simple frame-by-frame animations.
         */
        public Sequence frames(int count, long intervalMs, IntConsumer frame) {
            for (int i = 0; i < count; i++) {
                int frameNumber = i;
                steps.add(new Step(i == 0 ? 0 : intervalMs, () -> frame.accept(frameNumber)));
            }
            return this;
        }

        /**
         * Starts running the steps on the timeline.
         *
         * @return a handle that stops the remaining steps when cancelled
         */
        public Handle start() {
            Handle handle = new Handle();
            runFrom(0, 0, handle);
            return handle;
        }

        private void runFrom(int index, long delayMs, Handle handle) {
            if (index >= steps.size() || handle.cancelled) return;
            Step step = steps.get(index);
            handle.current =
                    after(
                            delayMs + step.delayMs,
                            () -> {
                                if (handle.cancelled) return;
                                step.action.run();
                                runFrom(index + 1, 0, handle);
                            });
        }
    }

    /** Controls a running {@link Sequence}. */
    public static final class Handle {
        private volatile boolean cancelled;
        private volatile ScheduledFuture<?> current;

        /** Stops the sequence. Steps that have not started yet will not run. */
        public void cancel() {
            cancelled = true;
            ScheduledFuture<?> f = current;
            if (f != null) f.cancel(false);
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }
}
