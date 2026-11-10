# Deck, Card and Foundation Bug Report

**Written:** 2026-11-09, at commit `2b59379` ("Bump version number").

- **Method:** I read the code. The decks were also built in a headless test run (all Spider types give 104 cards and Klondike gives 52), but none of the items below needed a test to show them.
- **Paths:** `Card.java`, `Deck.java` and `Foundation.java` are in `me/pintoadmin/cardTerminal/`. `SpiderDeck.java` is covered in `SPIDER_BUG_REPORT.md`.
- **Scope:** these classes are shared by both games, so a bug here can show up in either. Most of what's here is about making the shared code safer to build on before more games are added; the game-breaking bugs are in the game reports.

---

## High

### #1: `Card.flip()` doesn't flip the card
- **Location:** `Card.java:239-242`
- **Problem:** it sets `faceup = !faceup`, then calls `draw(x, y, !faceup)`. `draw` stores its argument back into `faceup` (`Card.java:145`), which undoes the toggle. The card ends up exactly as it was. Nothing calls it today, but it looks like the obvious method to use.
- **Proposed fix:** `public void flip() { flip(!faceup); }`, or remove it.

### #2: Drawing a card changes its state
- **Location:** `Card.java:141-145`
- **Problem:** `draw(x, y, faceup)` stores `faceup` on the card, so drawing a card face up is how the games reveal it (`Solitaire.java:280` via `flip(true)`, `Spider.java:127` via `setFlipped`). That ties game rules to rendering:
  - a card is only revealed when the screen happens to be redrawn,
  - undo has to know about drawing to hide a card again,
  - the games can't be tested or simulated without a screen.
- **Proposed fix:** make `draw` read-only: draw using the card's current `faceup` and never set it. Reveal cards in the move logic with `setFlipped(true)`. Keep `x`/`y` caching if `redraw()` needs it.

### #3: `Deck.getCard` throws instead of returning `null`
- **Location:** `Deck.java:85-90`
- **Problem:** it ends with `.toList().getFirst()`, which throws `NoSuchElementException` when no card matches. Callers can't check for "not found", and in the game screens that exception freezes the input thread (`CORE_BUG_REPORT.md` #1). The two-argument version always asks for copy 0, which is wrong for any multi-deck game.
- **Proposed fix:** use `.filter(...).findFirst().orElse(null)` and have callers handle `null`. Consider giving each card a stable id (for example, its position in `allCards`) and looking up by id; that is simpler than (rank, suit, index) and works for any number of decks.

---

## Medium

### #4: The top waste card is drawn with a gray border
- **Location:** `Deck.java:159-174`
- **Problem:** `drawDiscard` draws the top card and then always calls `drawCardSpace(x, y)`, which repaints the border in dark gray over the card. A selected waste card gets green text but a gray border, unlike selected tableau cards. It looks like `drawCardSpace` was meant to go only in the empty case.
- **Proposed fix:** call `drawCardSpace` only inside the `catch (NoSuchElementException)` branch (where `drawCardSpaceFilled` already does it), or replace the try/catch with an `isEmpty()` check.

### #5: `Deck.drawCardBack` leaves the graphics colors changed
- **Location:** `Deck.java:198-205`
- **Problem:** it sets the shared `TextGraphics` to a white background and black foreground and doesn't restore them. Anything drawn next without setting its own colors, such as `Main.drawText`, gets a white background. Nothing hits this today, because the next draw call always sets its colors, but it's an easy trap.
- **Proposed fix:** set the colors once before the loop and restore them after. Generally, every drawing helper should either set its colors up front or restore what it changed.

### #6: Two different card-back styles
- **Location:** `Deck.java:176-206`, `Card.java:205-232`
- **Problem:** the Solitaire stock is drawn by `Deck.drawCardBack` (white-background spaces) while face-down cards and the Spider stock use `Card.drawCardBack` (`█` characters). The two look different on some fonts and terminals.
- **Proposed fix:** have `Deck` call `Card.drawCardBack(x, y, color)` so there is one style.

### #7: `Foundation` only works with a single 52-card deck
- **Location:** `Foundation.java:33-40`, `Foundation.java:114-118`
- **Problem:** it looks up foundation cards by (rank, suit) with copy 0, and the four piles are fixed. That's fine for Klondike, but any future game with two decks (or foundations in Spider) can't reuse it.
- **Proposed fix:** store each pile as a `List<Card>` and read the top card from the list instead of looking it up from the deck. That removes the dependency on `Deck.getCard` too.

---

## Low: cleanup

- **Misleading names:**
  - `Deck.setAllCards` (`Deck.java:130-133`) replaces the cards *in the stock*, not `allCards`. Rename it `setCardsInDeck`.
  - `Foundation.clear()` (`Foundation.java:47-57`) only draws empty slots; it doesn't clear any state. Rename it `drawEmpty()` or similar.
  - `AutoPlay.java:188` has a comment working around `getCardsInDeck` ("despite the name").
- **Pointless recursion:** `Deck.drawCard` (`Deck.java:49-54`) calls itself if `remove` fails, but removing the first element of a non-empty list can't fail. Use `return cardsInDeck.isEmpty() ? null : cardsInDeck.removeFirst();`.
- **Unused code:** `Deck(long, int)`, `shuffle()`, `shuffleDiscard()`, `getNextShuffleIndex()`, `setNextShuffleIndex()` and the `random` field (only used once in the constructor) are never called.
- **Repeated drawing code:** the rounded-or-square corner logic is copied six times (`Card` ×3, `Deck` ×2, `Foundation`, plus `Main.drawRectangle`). A single `drawCardOutline(x, y, color)` helper would remove about 80 lines. `getOS()` reads a system property on every card drawn; cache it in a static field.
- **Cards hold the graphics object:** `Card`, `Deck` and `Foundation` copy `Main.g` into a field when they're created, so they can't be created before the screen exists. Reading `Main.g` when drawing (or passing it in) is safer and makes headless tests easier.
- **Copy index:** `Card(rank, suit, index)` uses the deck copy number as `index`. Name it `copy` or `deckNumber` so it isn't confused with a position.

## Verification

- **Tests:** a few plain JUnit tests would cover most of this once drawing no longer changes state:
  - `flip()` toggles `isFaceup()`,
  - `getCard` returns `null` for a missing card,
  - a fresh `Deck(seed)` has 52 distinct cards and the same order for the same seed,
  - `Foundation.addCard` / `removeCard` only accept the next and top card.
- **Visual check:** select the waste card in Solitaire and check the whole card turns green.
