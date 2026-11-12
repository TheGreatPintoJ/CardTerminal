# Solitaire (Klondike) Bug Report

**Written:** 2026-11-09, at commit `2b59379` ("Bump version number").

- **Method:** I read all of the Solitaire code, then ran the game headless (a Lanterna virtual terminal driving `handleInput` with real keystrokes) to check the serious findings. Items marked **Confirmed** were reproduced that way, and the result is quoted. Everything else comes from reading the code. For the card-loss bugs, the test counted every card in the stock, waste, tableau and foundation after each step; a correct game always has 52 distinct cards.
- **Paths:** `Solitaire.java`, `SolitaireMove.java` and `SolitaireOptions.java` are in `games/solitaire/`. The AutoPlay option on the options screen is covered separately in `AUTOPLAY_BUG_REPORT.md`.
- **Naming:** in this code, `stock` means the seven tableau columns, `deck` is the face-down stock, and the "discard" is the waste pile. This report uses the code's names.

**Why some of these freeze the game:** `Main.java:168` and `:180` rethrow any unexpected exception as a `RuntimeException`. Nothing catches it, so the key-handler thread dies and the game stops responding. See `CORE_BUG_REPORT.md` #1.

---

## Critical: loses cards, duplicates cards or freezes

Most of these are in `SolitaireMove.undo()`. Each one silently loses or duplicates a card, which makes the game unwinnable without any visible error.

### #1: Undoing a stack move only moves the top card back
- **Location:** `SolitaireMove.java:166-183`, compared with `SolitaireMove.java:118-129`
- **Problem:** moving a card between columns also moves every card below it (`handleRemove` appends the rest of the stack). Undo only moves the selected `card` back and only removes that one card from the target. The rest of the stack stays behind, now sitting on the wrong card.
- **Confirmed:** moved 7♥ 6♣ onto 8♠, then undid → `col1=[8S 6C]`, `col2=[KC* 7H]`. The 6♣ was left on the 8♠.
- **Proposed fix:** in MOVE `execute()`, save the full list of moved cards on the move. In `undo()`, remove that whole list from the target column and append it to the source column in the same order.

### #2: Undoing a draw in draw-3 mode duplicates one card and loses another
- **Location:** `SolitaireMove.java:184-188`, `Solitaire.java:221-231`
- **Problem:** a draw-3 turns over three cards but logs one DRAW move holding only the *first* card. Undo puts that first card back on the stock (where it is now also still in the waste) and removes the *third* card from the waste, which then isn't anywhere.
- **Confirmed:** KLONDIKE3, drew once, undid → `placed=52 distinct=51`: one card in two places and one card gone.
- **Proposed fix:** store every card the draw turned over (1 to 3 of them) on the DRAW move. Undo removes them from the top of the waste and pushes them back on the stock in reverse order.

### #3: Undoing the "stock is empty" press deletes the top waste card
- **Location:** `SolitaireMove.java:194-198`, `Solitaire.java:203-218`
- **Problem:** when the stock is empty, the next draw press logs an EXHAUST move that holds the current top waste card but doesn't move anything. Undo then calls `removeTopDiscard()`, which takes that card out of the game.
- **Confirmed:** KLONDIKE1, drew all 24 cards, pressed draw once more, undid → `placed=51 distinct=51`.
- **Proposed fix:** EXHAUST only changes the "next draw recycles" flag, so its undo should only reset that flag: `solitaire.setNextShuffle(false)`. Remove the `removeTopDiscard()`.

### #4: Undoing a move off the foundation loses the card
- **Location:** `SolitaireMove.java:166-175`
- **Problem:** the `switch (fromLocation)` in `undo()` handles STOCK and DISCARD but not FOUNDATION. Undoing a foundation-to-column move removes the card from the column and never puts it back on the foundation.
- **Confirmed:** moved 2♥ from the foundation onto 3♠ and undid → the card was gone (`placed=50`, down from 51).
- **Proposed fix:** add `case FOUNDATION: solitaire.getFoundation().addCard(card);` to that switch.

### #5: Sending a covered card to the foundation duplicates it
- **Location:** `SolitaireMove.java:57-61`, `SolitaireMove.java:130-134`
- **Problem:** `execute()` adds the card to the foundation first, then `handleRemove` notices that cards are lying on it and returns `false`. By then the foundation already has it, so the card is in both places. You can reach this by selecting a card that isn't at the bottom of its column with the row keys and pressing `q`/`w`/`e`/`r`.
- **Confirmed:** column `[2♠ A♦]`, A♠ on the foundation, selected 2♠ and pressed `q` → `col1=[2S AD]`, foundation `[AS 2S]`, `distinct=51`.
- **Proposed fix:** check that the card is the last one in its column *before* calling `addCard`, and refuse the move otherwise.

### #6: A long seed crashes the options screen
- **Location:** `SolitaireOptions.java:159`
- **Problem:** `Long.parseLong` throws `NumberFormatException` once the typed seed is bigger than `Long.MAX_VALUE`.
- **Confirmed:** typing twenty 9s → `NumberFormatException: For input string: "9999999999999999999"`.
- **Proposed fix:** ignore a digit once the seed has 18 digits, or catch `NumberFormatException`. The same bug is in `SpiderOptions`; see `MENUS_BUG_REPORT.md` #1.

### #7: The win replay can crash
- **Location:** `SolitaireMove.java:52-54`, `Solitaire.java:89-132`, `Deck.java:164-165`
- **Problem:** if you press draw when both the stock and the waste are empty, the EXHAUST move stores a `null` card. In normal play that's harmless, but the win replay runs `execute()` on it, which calls `discardCard(null)` and puts `null` into the waste. The next redraw calls `null.draw(...)`, and `drawDiscard` only catches `NoSuchElementException`, so the `NullPointerException` escapes and freezes the game right after the win.
- **Proposed fix:** don't log an EXHAUST move when the waste is empty, and have `Deck.discardCard` ignore `null`. Also see #12: the replay blocks input for its whole length.

---

## High: wrong state or wrong rules

### #8: Undo turns face-up cards face down
- **Location:** `SolitaireMove.java:168-169`
- **Problem:** undoing a column move always flips the source column's last card face down before putting the moved card back. That's only right if the move uncovered that card. If you moved part of a face-up run, the card above it was already face up, and undo hides it. Face-down cards can't be selected, so that run is stuck until the card is uncovered again.
- **Confirmed:** column `[K♣ 7♥ 6♣]` all face up, moved 7♥ away and undid → `K♣` is face down.
- **Proposed fix:** in `execute()`, record whether the card that ends up on top of the source column was face down. On undo, flip it back only in that case. (`SpiderMove.faceDownBelow` already does this for Spider.)

### #9: Redo records moves that failed
- **Location:** `Solitaire.java:453-458`
- **Problem:** `redo()` ignores the result of `execute()` and always moves the entry to `moves`. After any of the bugs above, a failed redo stays in history, and undoing it then changes the board for a move that never happened.
- **Proposed fix:** as Spider does now: only push the move to `moves` when `execute()` returns `true`, and clear `movesUndone` when it fails.

### #10: Undo and redo keep the selection
- **Location:** `Solitaire.java:345-352`, `Solitaire.java:445-458`
- **Problem:** the selected card can be moved by an undo. `selectedCardLocation` then works out the wrong location. For example, a selected foundation card that undo returns to the waste is reported as FOUNDATION, and moving it puts it in a column while it also stays in the waste.
- **Proposed fix:** call `clearSelected()` at the start of `undo()` and `redo()`.

### #11: F10 always starts a draw-1 Klondike game
- **Location:** `Solitaire.java:135-137`
- **Problem:** `newGame()` calls `new Solitaire()`, which hardcodes `KLONDIKE1`. A KLONDIKE3 or Vegas player gets switched to draw-1.
- **Proposed fix:** `return new Solitaire(type, new Random().nextLong());`

### #12: The win replay blocks input
- **Location:** `Solitaire.java:121-131`, `Solitaire.java:410-425`
- **Problem:** after the 2 s win banner, every move in the game is replayed at 50 ms each on the key thread, followed by another 1 s sleep. A 300-move game is unresponsive for about 18 seconds. Keys pressed meanwhile queue up and are then applied to the main menu.
- **Proposed fix:** shorten the delay, let any key skip the replay, and drain queued input before switching to the menu. Or drop the replay.

### #13: Vegas redeal limit has two problems
- **Location:** `Solitaire.java:180`, `Solitaire.java:197`, `SolitaireMove.java:189-193`
- **Problem:**
  - Undoing a redeal doesn't give the redeal back: `shuffleCounter` is never decremented. Undo a redeal, then press draw, and two of your redeals are used up.
  - When the limit is reached, the draw key silently does nothing, and the empty stock is still drawn with the `⇄` recycle symbol.
- **Proposed fix:** decrement `shuffleCounter` in SHUFFLE `undo()` and increment it in `execute()` instead of in `drawCard()`. When no redeals are left, draw the empty stock without the symbol (or with an ✕).
- **Note:** the Vegas types have no scoring, so they are only "Klondike with limited redeals." That's fine, but it may be worth saying so on the options screen.

---

## Medium: controls and display

### #14: Tab doesn't select the waste card
- **Location:** `Solitaire.java:300-384`
- **Problem:** the README and the comment at `Solitaire.java:293` say Tab selects the top waste card, but `handleInput` only handles `KeyType.Character`, F10, F12 and Escape. Tab (`KeyType.Tab`) does nothing. Space does select the waste card, so it's only a mismatch between the docs and the code.
- **Proposed fix:** handle `KeyType.Tab` the same as Space, or remove it from the README and the comment.

### #15: Cards past row 9 can't be selected
- **Location:** `Solitaire.java:324-330`
- **Problem:** row keys 1-9 pick the first nine face-up cards, and 0 picks the last. A column with 10 to 12 face-up cards has cards you can't pick up, so you can't move part of that run. `Solver.java:70` has to work around it.
- **Proposed fix:** add an up/down arrow cursor like Spider's, or let a second press of the same digit add 10.

### #16: Fixed-size layout with no scrolling
- **Location:** `Solitaire.java:28-29`, `Solitaire.java:241`, `Solitaire.java:245`, `Solitaire.java:283-286`
- **Problem:**
  - The tableau is drawn from `width - 63`, so it goes off-screen to the left in narrow windows.
  - The foundation (36 columns wide, bottom left) and the stock and waste overlap the tableau below about 99 columns.
  - Long columns run off the bottom and draw over the seed text in the bottom right corner.
- **Proposed fix:** same approach as Spider: center the tableau, keep the stock, waste and foundation in their own band, stack face-down cards more tightly, and show a "please enlarge the window" message when it still doesn't fit.

### #17: Space leaves the column selected
- **Location:** `Solitaire.java:340-344`
- **Problem:** pressing Space while a column card is selected switches the selection to the waste card but leaves `selectedColumn` set, so the column's highlight logic still runs for the old column.
- **Proposed fix:** call `clearSelected()` before `selectCard(Location.DECK, 0)`.

---

## Low: cleanup

- **`-` redraws twice:** the deselect key calls `redraw()` (`Solitaire.java:311`) and then `drawGame()` at the end of `handleInput`. `redraw()` can be removed entirely; it's not used anywhere else.
- **Redundant check:** `else if (drawnCard != null)` at `Solitaire.java:219` is always true there.
- **Dead code:** `SolitaireMove.shuffleIndex` and its getter/setter, `getUUID()` and `getFromIndex()` are unused, as is `SolitaireOptions.start()` (which also ignores `type` when the seed is 0). The commented-out `setNextShuffleIndex` calls in `SolitaireMove` can go too.
- **SHUFFLE is not a shuffle:** the SHUFFLE move recycles the waste in order (`restartDiscard`), which is correct for Klondike. Renaming it `RECYCLE` would avoid confusion with `Deck.shuffleDiscard`, which is never called.
- **Game logic in the renderer:** the new last card of each column is turned face up inside `drawGame` (`Solitaire.java:280`). Do it in the move logic instead, so state doesn't depend on the screen being drawn.
- **README:** the Solitaire keybinds don't mention `[` (undo) and `]` (redo).

## Recommended fix order

1. **Stop losing cards:** #1, #2, #3, #4, #5. Each is a small, local change in `SolitaireMove`.
2. **One-liners:** #6, #9, #10, #11, #17.
3. **Face-down state:** #8.
4. **Win:** #7 and #12 together.
5. **Rules and controls:** #13, #14, #15.
6. **Layout:** #16, then cleanup.

## Verification

- **Tests:** add JUnit tests under `src/test` (JUnit is already in `build.gradle`). Point `Main.terminal`, `Main.screen` and `Main.g` at a `DefaultVirtualTerminal` before creating a game, then drive it with `handleInput`. The most valuable single check: after any sequence of moves, undos and redos, the stock, waste, tableau and foundation together hold exactly 52 distinct cards. Specific cases:
  - draw then undo in draw-1 and draw-3,
  - draw through the stock, the empty press, the recycle, then undo all three,
  - move a stack of 3 and undo,
  - move from the foundation to a column and undo,
  - send a covered card to the foundation (should be refused),
  - in Vegas draw-3, use a redeal, undo it, and check it can be used again.
- **Playthrough** with the built jar: play a draw-3 game, undo several draws and stack moves, check nothing changes face down, press F10 and check the game type is kept, and win a game. Turning on AutoPlay in the options plays a solvable seed up to its last move, which is the quickest way to reach the win screen (see `AUTOPLAY_BUG_REPORT.md` for its own issues).
