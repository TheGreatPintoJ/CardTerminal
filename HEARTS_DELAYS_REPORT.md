# Hearts: Delays & Animation Timing — Change Report

## Summary

Hearts now has one shared, non-blocking way to schedule delays: the new `Timeline` class. All
pauses in the Hearts engine (trick on the table, end-of-deal score screen, bot "thinking") use it
or the timing constants in `HeartsTiming` instead of calling `Thread.sleep` directly. The UI screen
also has its own `Timeline` for future client-side animations.

Before this change, the game froze when anyone played the last card of a trick.

## What was wrong before

| Problem | Where | Effect |
|---|---|---|
| `Thread.sleep(1000)` inside `finishTrick()`, called from `synchronized playCard()` | `HeartsEngine` | The thread that played the 4th card of a trick was frozen for 1 s **while holding the engine lock**. If that was you, the key-handler thread froze too, so no input was read. Bots, remote client threads and snapshot sends all waited on the lock. |
| `Thread.sleep(4000)` inside `finishDeal()` (same call chain) | `HeartsEngine` | Last trick of a deal: about 5 s total freeze with the lock held. |
| Hard-coded `Thread.sleep(100) // todo: change back to 1000` | bot thread | No single place to tune game pace. |
| Bot thread was `while (true)` and not a daemon | bot thread | It never stopped. Once `state == ENDED` neither branch slept, so it **busy-spun at 100% CPU** for the rest of the program. |
| `InterruptedException` swallowed with "Ignored exception" | bot thread, engine | The bot thread could not be stopped cleanly. |

## Changes

### 1. New: `src/main/java/me/pintoadmin/cardTerminal/Timeline.java` (core, reusable by any game)

A small scheduler that owns a single daemon thread. Tasks run on that thread in order. The caller
returns immediately and no locks are held while waiting.

| API | Purpose |
|---|---|
| `after(ms, task)` | Run `task` once after `ms`. Returns a `ScheduledFuture` you can cancel. |
| `sequence().then(..).pause(ms).frames(n, intervalMs, f -> ..).start()` | Chain steps, pauses and frame-by-frame animations. Returns a `Handle` with `cancel()`. |
| `cancelAll()` | Drop everything still waiting. |
| `close()` | Drop pending tasks and stop the thread. Safe to call from inside a task and safe to call twice. |
| `isTimelineThread()` / `isClosed()` | Introspection. |
| `Timeline.sleep(ms)` (static) | Interrupt-aware blocking sleep for threads that exist to wait (the bot loop). Returns `false` if interrupted, so loops can exit. |

Details:
- The thread starts lazily on the first scheduled task, so an idle `Timeline` costs nothing.
- Exceptions thrown by a task are logged and don't kill the timeline. A step that throws stops the
  rest of its `sequence`.
- The timeline only controls *when* code runs. Tasks that touch shared state must still
  synchronize on it.

### 2. New: `games/multiplayer/hearts/HeartsTiming.java`

All Hearts durations in one place (milliseconds):

| Constant | Value | Used for |
|---|---|---|
| `BOT_PLAY` | 100 | Bot "think" time before playing (kept at the current value; the old TODO suggested 1000) |
| `BOT_POLL` | 100 | How often the bot thread checks for work (not scaled) |
| `TRICK_END` | 1000 | Full trick stays on the table |
| `DEAL_SCORES` | 4000 | Score screen between deals |

`HeartsTiming.speed` (default `1.0`) scales every duration through `HeartsTiming.ms(base)`:
`0.5` is twice as fast, `2.0` is twice as slow, `0` skips waiting. This is ready to hook up to the
"Bot speed: Fast / Normal / Slow" idea in `HEARTS_SUGGESTIONS.md` #22.

### 3. Changed: `HeartsEngine.java`

- **New `pauseThen(delayMs, next)`**: the engine's one way to say "hold for a moment, then
  continue". It sets `paused = true`, schedules `next` on the engine `Timeline`, and when the delay
  ends it takes the engine lock, clears `paused` and runs `next`. If the game ended in the
  meantime, `next` is skipped.
- **New `isPaused()`**: true while a trick or the score screen is on display.
- `playCard`: rejects plays while paused. On the 4th card it now calls
  `pauseThen(TRICK_END, this::finishTrick)` and returns immediately instead of blocking.
- `finishTrick`: the sleep and the duplicate `updateClients`/`resizeClients` calls at the top are
  removed (`playCard` already sent them before the pause).
- `finishDeal`: split in two. Scoring and showing the score screen stay in `finishDeal`, which then
  calls `pauseThen(DEAL_SCORES, this::afterDealScores)`. The new `afterDealScores()` contains the
  old tail: resize, game-end check, `newDeal()`.
- Bot thread:
  - Loops `while (state != ENDED && !interrupted)` instead of `while (true)`.
  - Waits while `paused`, and checks `paused` again after its think delay, so it doesn't keep trying
    to play during the trick pause.
  - Sleeps on PREGAME and other idle states, so there's no more busy-spin.
  - Uses `HeartsTiming.ms(BOT_PLAY)` and `BOT_POLL` via `Timeline.sleep`, and exits on interrupt.
  - Now named `"Hearts bots"` and set as a daemon.
- `endGame()`: now `synchronized`. It clears `paused`, closes the engine timeline (pending pauses
  are dropped) and interrupts the bot thread.

**Behaviour that is intentionally unchanged:** the trick still shows for 1 s and the scores for
4 s. During the trick pause, clients see the same snapshot as before (all 4 cards on the table, the
turn arrow pointing at the trick's leader). No protocol changes, so old remote clients still work.

### 4. Changed: `HeartsLocal.java`

- New `protected final Timeline animations`. It's the place for client-side animations and timed
  redraws, and it works for both the host and `HeartsRemote` clients, since remote clients have no
  engine. `drawGame()` is `synchronized`, so calling `resize()` from a timeline task is safe.
- `animations.close()` is called when the player leaves (escape ×3). `HeartsRemote` inherits this.
- Nothing is animated yet. This adds the mechanism without changing how the screen looks.

### 5. New tests (the project had none before)

- `src/test/java/me/pintoadmin/cardTerminal/TimelineTest.java`: 6 tests covering non-blocking
  `after`, sequence order and pauses, cancel, close dropping pending tasks, a failing task not
  killing the timeline, and interrupt handling in `sleep`.
- `src/test/java/.../hearts/HeartsEnginePauseTest.java`: plays seat 0 against the real bots for 2
  tricks. It checks that `playCard` returns in under 250 ms (it used to block about 1000 ms on the
  4th card), that the pause is observed, and that plays are rejected during it.

Result: `./gradlew test` passed, 7/7, 0 skipped.

## How to add a delay or animation

**Engine pacing** (all players see it):
```java
// 1. add a constant in HeartsTiming
public static final long SHOOT_MOON_BANNER = 2500;
// 2. inside a synchronized engine method
server.updateClients();
pauseThen(HeartsTiming.SHOOT_MOON_BANNER, this::continueAfterBanner);
```

**UI animation** (one client only, inside `HeartsLocal` or `HeartsRemote`):
```java
// flash the "Hearts are broken" text three times
animations.sequence()
        .frames(6, 150, f -> { flashOn = (f % 2 == 0); resize(); })
        .then(() -> { flashOn = false; resize(); })
        .start();

// or just redraw later
animations.after(HeartsTiming.ms(300), this::resize);
```

**Rules of thumb**
- Never call `Thread.sleep` on the key-handler thread or inside a `synchronized` engine method. Use
  `pauseThen` (engine) or `animations` (UI).
- Put durations in `HeartsTiming` and wrap them in `HeartsTiming.ms(...)`, so they follow `speed`.
- Keep a `Handle` if an animation may need to be stopped early (for example, a new snapshot
  arrives).

## Notes / not changed (pre-existing, outside this task)

- Bots choose passing cards randomly *with replacement*, so they sometimes pick the same card twice.
  `passCards` rejects that and the bot retries on the next poll. This works, but could be fixed.
- `Game.win()` (Solitaire/Spider) still uses a blocking `Thread.sleep(2000)`. It could move to
  `Timeline` the same way.
- `HeartsEngine.java` has an "unchecked" compiler note from the generic `List<Card>[] hands` array.
  It's not related to this change.
- I could not test real gameplay in a terminal from here. Engine behaviour is verified by the
  automated test above, but a quick manual playthrough (host and a remote client) is recommended.
