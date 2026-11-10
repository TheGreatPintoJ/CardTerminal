# Menus Bug Report

**Written:** 2026-11-09, at commit `2b59379` ("Bump version number").

- **Method:** I read the code. The seed crash (#1) was reproduced in a headless test run; everything else comes from reading the code.
- **Scope:** the startup screen (`screens/Startup.java`), the main menu (`screens/MainMenu.java`), the shared options base class (`screens/GameOptions.java`), and the two game options screens (`games/solitaire/SolitaireOptions.java`, `games/spider/SpiderOptions.java`).
- **Duplication:** `SolitaireOptions` and `SpiderOptions` are almost line-for-line copies, so every options-screen bug below is in both. The best fix for most of them is to move the shared code (seed entry, type picker, Play button, drawing) into `GameOptions` and have each game supply only its name, its type list and how to start a game.

---

## Critical

### #1: Typing a long seed crashes the options screen
- **Location:** `SolitaireOptions.java:159`, `SpiderOptions.java:152`
- **Problem:** each digit is appended with `Long.parseLong(String.valueOf(SEED) + c)`, which throws `NumberFormatException` once the number is bigger than `Long.MAX_VALUE` (19 digits). The exception freezes the input thread (`CORE_BUG_REPORT.md` #1).
- **Confirmed:** typing twenty 9s into either screen → `NumberFormatException: For input string: "9999999999999999999"`.
- **Proposed fix:** keep the typed seed as a `String` while editing, cap it at 18 digits (or catch `NumberFormatException` and ignore the key), and only parse it when Play is pressed.

---

## Medium

### #2: Half of all seeds can't be typed back in
- **Location:** `SolitaireOptions.java:26`, `SpiderOptions.java:20`, digit handling at `SolitaireOptions.java:155-169` and `SpiderOptions.java:148-162`
- **Problem:** the suggested seed is `new Random().nextLong()`, which is negative half the time, and `F10` games use negative seeds too. The game shows the seed (`SEED: -4821…`) so you can replay it, but the options screen only accepts digits, so there is no way to type the minus sign. Seed `0` can't be chosen either, because it means "use the suggested seed".
- **Proposed fix:** generate seeds with `new Random().nextLong() & Long.MAX_VALUE` (never negative), or accept `-` as the first character. Use an `OptionalLong` or a separate flag for "no seed typed" instead of the value `0`.

### #3: Options are forgotten when you go back to the menu
- **Location:** `SolitaireOptions.java:110`, `SpiderOptions.java:103`, `Solitaire.java:304`, `Spider.java:215`, `MainMenu.java:29-68`
- **Problem:** every Escape creates a `new MainMenu()`, which creates new options screens. The seed and type you picked are reset each time, and the menu selection goes back to the first game.
- **Proposed fix:** create the main menu (and its options screens) once in `Main` and switch back to that instance instead of making a new one. Call `resize()` on it when switching so it redraws.

### #4: Game previews can spill out of their tile
- **Location:** `MainMenu.java:30-65`
- **Problem:** the preview loops are bounded by the tile's width but not its height. Each extra column of the preview is one row taller, so on wide but short windows the preview draws past the bottom of its tile and over the game's name.
- **Proposed fix:** also stop when `eY` reaches the tile's bottom edge (`y + (H - 2) / gridH - 2`).

### #5: The Spider preview is a copy of the Solitaire preview
- **Location:** `MainMenu.java:48-65`
- **Problem:** the two lambdas are identical, so both tiles show the same Klondike-style triangle.
- **Proposed fix:** give Spider its own preview, for example ten equal columns. Moving each preview into its `GameOptions` subclass (a `drawPreview(x, y, w, h)` method) would also remove the copy.

---

## Low

- **Escape quits without asking:** on the main menu, Escape exits the app (`Main.java:160-166`). Because Escape is also "back" on every other screen, pressing it twice from a game quits. Consider asking for confirmation, or using `q` to quit from the menu.
- **No paging:** the menu draws a `< --- >` hint when there are more than 12 games (`MainMenu.java:82`), but there is no paging. A 13th game would be drawn below the screen. Fine for now; worth fixing before the roadmap games are added.
- **Unused methods:** `GameOptions.setSeed` and `GameOptions.start` are never called. Both `start()` implementations also ignore the chosen type when the seed is 0. Either use `start()` for the Play button (and fix it) or remove both.
- **Enter does nothing on SEED or TYPE:** Play is only reachable with ArrowDown. Making Enter start the game from any option would be friendlier.
- **No wrap-around:** pressing up on the first option or left on the first type relies on catching `IndexOutOfBoundsException` (`SolitaireOptions.java:116-134`). It works, but using `Math.floorMod` to wrap (or clamping) would be clearer.
- **AutoPlay in the release UI:** the Solitaire options screen exposes AutoPlay, which is a developer tool from the `dev` package and has several problems of its own (`AUTOPLAY_BUG_REPORT.md`). Consider hiding it behind a system property.
- **Startup screen:**
  - The banner is 113 characters wide (`Startup.java:34-41`) and is cut off on both sides in narrower windows. Fall back to a plain `CardTerminal` title when `W < 115`.
  - It always waits 2 seconds (`Main.java:125`). Let any key skip it.
- **Inconsistent error output:** `MainMenu` and `Startup` print drawing errors to `System.out`; the options screens use `System.err`.

## Verification

- **Seed entry:** type 25 digits, a backspace and more digits; the app must keep responding. Start a game, note a negative seed, go back and enter it.
- **Menu state:** pick a type and seed, press Escape twice (game, then options), come back and check they're kept (once #3 is fixed).
- **Resize:** drag the window very wide and short and check the previews stay inside their tiles.
