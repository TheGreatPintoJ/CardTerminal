package me.pintoadmin.cardTerminal.games.multiplayer.hearts;

/**
 * Every pause and animation length used by Hearts, in one place.
 *
 * <p>All values are in milliseconds and go through {@link #ms(long)}, so a single {@link #speed}
 * value can make the whole game faster or slower (for example, a future "bot speed" option, or
 * {@code 0} in tests to skip waiting entirely).
 *
 * <p>To add a new delay, add a constant here and use it with the engine's or UI's {@code Timeline}
 * — do not call {@code Thread.sleep} directly from game code.
 */
public final class HeartsTiming {
    private HeartsTiming() {}

    /**
     * Multiplier applied to every duration. {@code 1.0} = normal, {@code 0.5} = twice as fast,
     * {@code 2.0} = twice as slow, {@code 0} = no waiting.
     */
    public static volatile double speed = 1.0;

    /** How long a bot "thinks" before playing a card. Was hard-coded with a TODO to use 1000. */
    public static final long BOT_PLAY = 100;

    /** How often the bot thread checks whether it has something to do. Not scaled by speed. */
    public static final long BOT_POLL = 100;

    /** How long a finished trick stays on the table before it is collected. */
    public static final long TRICK_END = 1000;

    /** How long the end-of-deal score screen stays up before the next deal is dealt. */
    public static final long DEAL_SCORES = 4000;

    /** Returns {@code base} scaled by {@link #speed}. */
    public static long ms(long base) {
        return Math.max(0, Math.round(base * speed));
    }
}
