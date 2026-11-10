# Core App, Threading and Build Bug Report

**Written:** 2026-11-09, at commit `2b59379` ("Bump version number").

- **Method:** I read the code, the build files and the crash log in the repo root. Nothing in this report was run directly. The exceptions #1 refers to were reproduced as escaping `handleInput` in headless runs of the games (see those reports).
- **Scope:** `Main.java` (startup, input and resize threads), `Game.java` and `CustomScreen.java`, plus `build.gradle`, `.github/workflows/gradle.yml` and `hs_err_pid16200.log`.

---

## Critical

### #1: Any unexpected exception freezes the app
- **Location:** `Main.java:167-181`
- **Problem:** the key thread catches exceptions from `handleInput`, but anything that isn't one of the three control-flow messages is rethrown as a `RuntimeException`. Nothing catches that, so the key thread dies. The window stays open and keeps resizing, but no key does anything again. The only way out is closing the window. Every crash in the other reports ends this way, for example:
  - Spider: winning (`NullPointerException`) and undoing a deal with an empty column (`NoSuchElementException`),
  - both options screens: typing a long seed (`NumberFormatException`).
- **Proposed fix:** catch `Exception` (and ideally `Throwable` except `Error`) inside the loop, log it with the stack trace, and keep the loop running. Better: show a short error line at the bottom of the screen and send the player back to the main menu, so a bug costs them one game instead of the whole session.

### #2: Exceptions are used for control flow
- **Location:** `Main.java:169-180`, `Solitaire.java:305`/`376`/`379`, `Spider.java:216`/`248`/`251`, both options screens
- **Problem:** screens signal "back to menu", "restart" and "new game" by throwing `new Exception("override")`, `"reset"` or `"newseed"`. This is fragile:
  - any real exception whose message happens to be one of those strings is treated as a command,
  - real exceptions without a message are rethrown and freeze the app (#1),
  - it's the reason `handleInput` has to be declared `throws Exception`.
- **Proposed fix:** have `handleInput` return a result, for example `enum Action { NONE, BACK_TO_MENU, RESTART, NEW_GAME }`, and switch on that in `Main`. Then #1 only has to handle genuine errors.

---

## High

### #3: Two threads spin the CPU constantly
- **Location:** `Main.java:155-157`, `Main.java:197-207`
- **Problem:** the key thread calls `screen.pollInput()` in a loop with no sleep, and the resize thread calls `doResizeIfNecessary()` in a loop with no sleep. Together they likely keep two CPU cores at 100% the whole time the app is open, which drains laptop batteries and spins up fans.
- **Proposed fix:** use the blocking `screen.readInput()` in the key thread. For resizes, either sleep 50 ms per loop or check `doResizeIfNecessary()` in the key thread using `pollInput` plus a short sleep, which also fixes #4.

### #4: Two threads draw at the same time
- **Location:** `Main.java:159`, `Main.java:200`, `Main.java:85`
- **Problem:** the resize thread calls `currentScreen.resize()` while the key thread may be in the middle of `handleInput`. Both draw through the same `TextGraphics`, which isn't thread-safe, and the game code iterates its card lists while the other thread may be changing them. Expected symptoms:
  - garbled or half-drawn frames after a resize,
  - a `ConcurrentModificationException` that kills one of the threads.

  `currentScreen` is also a plain field written from three threads (main, key and AutoPlay), so one thread may not see another's change.
- **Proposed fix:** the simplest is a single thread that handles both input and resizes (see #3). Otherwise, wrap both `handleInput` and `resize` in `synchronized (Main.class)` and make `currentScreen` `volatile`.

### #5: Long pauses on the input thread
- **Location:** `Game.java:42-45`, `Solitaire.java:121-131`/`416-419`, `Spider.java:83-94`/`315`/`324-327`, `AutoPlay.java:45-46`
- **Problem:** the win banner sleeps 2 s, both replays sleep 50 ms per move, Spider sleeps 250 ms per completed run, and AutoPlay runs a search that can take a long time. All of this happens inside `handleInput`, on the key thread, so input piles up and is replayed into whatever screen comes next.
- **Proposed fix:** keep `handleInput` fast. For animations, draw a frame, then schedule the next frame (for example with a `ScheduledExecutorService` that posts back to the input thread) and let any key cancel. Discard queued input when switching screens.

---

## Medium

### #6: The resize thread dies on the first error
- **Location:** `Main.java:200-205`
- **Problem:** an `IOException` from `screen.refresh` is rethrown as a `RuntimeException`, and any exception from a screen's `resize()` isn't caught at all. Either kills the resize thread, after which the app no longer redraws when the window changes size.
- **Proposed fix:** catch and log inside the loop, as for #1.

### #7: Native JVM crash in the repo (`hs_err_pid16200.log`)
- **Location:** `hs_err_pid16200.log` (untracked, repo root)
- **What it shows:** `EXCEPTION_ACCESS_VIOLATION` in `awt.dll` on the AWT event thread, inside Swing's repaint through the Direct3D pipeline (`D3DScreenUpdateManager.getGdiSurface` → `Win32GraphicsDevice.makeColorModel`). It happened after 32 minutes, running `CardTerminal-1.0.jar` on Microsoft OpenJDK 25.0.2 under the IntelliJ debugger.
- **Assessment:** this is a crash inside the JDK's Windows graphics code, not in CardTerminal. That code path is typical of a display change, such as a monitor going to sleep, a resolution or DPI change, or a remote-desktop switch. The game can't catch it, but it can avoid the Direct3D pipeline.
- **Proposed fix:**
  - Add `-Dsun.java2d.d3d=false` to the packaged app's Java options (the `jpackage` task in `build.gradle` accepts `javaOptions`), and try it when running from the IDE.
  - If it happens again with that flag, report it to the JDK vendor with the log.
  - Delete the log and add `hs_err_pid*.log` to `.gitignore`.

### #8: The build needs Java 25, but nothing says so
- **Location:** `Main.java:95`, `Main.java:139`, `build.gradle`, `README.md`
- **Problem:** `Main` uses an instance `void main(String[] args)` (finalized in Java 25) and unnamed variables (`catch (RuntimeException _)`, Java 22+). `build.gradle` doesn't set a toolchain, so building with an older JDK fails with confusing errors. The README tells people to run the JAR "if you have JRE installed", but on anything before Java 25 it won't start (`UnsupportedClassVersionError`, or no main method found).
- **Proposed fix:**
  - Add `java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }` to `build.gradle`.
  - Say "requires Java 25 or newer" in the README next to the JAR download.
  - Alternatively, use `public static void main(String[] args)` so the code is less tied to the newest JDK.

### #9: The release workflow has leftovers
- **Location:** `.github/workflows/gradle.yml`
- **Problem:**
  - "Setup Gradle" runs twice.
  - The build step is named "Build with Gradle 8.9" but uses Gradle 9.8.1.
  - It runs a system `gradle` instead of the repo's `./gradlew`, so CI and local builds can use different Gradle versions.
  - Releases only build on manual dispatch, and nothing runs the tests (there are none yet).
- **Proposed fix:** use one `setup-gradle` step and run `./gradlew jpackage`. Add a second workflow that runs `./gradlew build` (which will include tests once there are some) on every push and pull request.

### #10: Two tasks write the same jar file
- **Location:** `build.gradle` (the `shadowJar` block and `stageJpackageInput`)
- **Problem:** `shadowJar` has its classifier set to `''`, so it writes `build/libs/CardTerminal-1.1.jar`, the same path as the plain `jar` task. `stageJpackageInput` depends on both without an order between them. If `jar` happens to run last, the installer gets the thin jar without Lanterna and fails at startup with `ClassNotFoundException`. I haven't seen this happen; it depends on task order.
- **Proposed fix:** disable the plain jar (`tasks.named('jar') { enabled = false }`) or give it a classifier like `'plain'`, and have `stageJpackageInput` depend only on `shadowJar`.

---

## Low

- **Formatting on compile:** `compileJava.dependsOn spotlessApply` rewrites source files on every build, including in CI. Running `spotlessCheck` in CI and `spotlessApply` by hand (or in a pre-commit hook) is more usual.
- **Abstract methods nobody uses:** `Game.redraw()` and the `logged` argument of `Game.drawCard(boolean)` aren't used by `Main`. Spider ignores `logged`. Trim `Game` to what `Main` actually calls.
- **Escape check runs after the screen:** `Main` checks for Escape (`Main.java:160`) only after `handleInput` returns normally, which is why every game has to throw `"override"` to avoid quitting. Fixed by #2.
- **Crash on a real terminal:** when run in a console (not the Swing window), a crash in the key thread leaves the terminal in raw mode. A shutdown hook that calls `screen.stopScreen()` would restore it.
- **Untracked files:** `hs_err_pid16200.log` and the `.idea/dictionaries` and `.idea/inspectionProfiles` folders. Add them to `.gitignore` or commit the IDE files you want shared.
- **No tests:** JUnit 5 is configured, but there is no `src/test`. See the Verification sections in the game reports for where to start.

## Recommended fix order

1. **#1** (keep the input loop alive). It turns every remaining crash into a recoverable error.
2. **#3 and #4 together**, by moving to a single input-and-resize loop.
3. **#2**, then **#5**.
4. **#8, #9, #10** in one build cleanup.
5. **#7** (add the Java option), then the low items.
