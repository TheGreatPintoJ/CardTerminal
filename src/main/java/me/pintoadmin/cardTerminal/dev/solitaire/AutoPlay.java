// @formatter:off
package me.pintoadmin.cardTerminal.dev.solitaire;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;
import java.util.*;
import me.pintoadmin.cardTerminal.*;
import me.pintoadmin.cardTerminal.games.solitaire.*;

/**
 * Starts Solitaire on a seed, solves it with Solver, and plays every move except the last one by
 * feeding keystrokes into Solitaire.handleInput. Afterwards you play normally.
 *
 * <p>In the app, the AutoPlay box on the Solitaire options screen calls {@link #start}, which makes
 * the game Main's current screen so Main's own input loop takes over once the moves are played.
 * Standalone (autoplay.ps1): AutoPlay [seed] [delayMs] [gameType], with a minimal input loop.
 */
public class AutoPlay {
    static Solitaire game;
    // Mirror of the game's cursor (selectedColumn / selectedRow), which it doesn't expose. A new
    // game starts at 0/0 and only the arrow keys change it, so tracking our own presses is exact.
    static int curCol, curRow;

    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 4154672263252710606L;
        int delay = args.length > 1 ? Integer.parseInt(args[1]) : 60;
        Solitaire.GameType type = args.length > 2 ? Solitaire.GameType.valueOf(args[2]) : Solitaire.GameType.KLONDIKE1;

        boolean standalone = Main.screen == null;
        if (standalone) {
            Main.terminal = new DefaultTerminalFactory().createTerminal();
            Main.screen = new TerminalScreen(Main.terminal);
            Main.screen.startScreen();
            Main.g = Main.screen.newTextGraphics();
            Main.BLACK = Main.g.getBackgroundColor();
            Main.screen.setCursorPosition(null);
        }
        if (!start(seed, delay, type)) {
            if (standalone) System.exit(1);
            return;
        }
        if (standalone) standaloneLoop();
    }

    /**
     * Solves and plays the seed as Main's current screen. Runs on the caller's thread (Main's key
     * thread when called from the options screen), so keys pressed meanwhile wait until it returns.
     *
     * @return false if no solution was found; the caller's screen is left as it was.
     */
    public static boolean start(long seed, int delay, Solitaire.GameType type) throws Exception {
        TextGraphics g = Main.g;
        int W = g.getSize().getColumns();
        Main.screen.clear();
        g.setBackgroundColor(Main.BLACK);
        g.setForegroundColor(TextColor.ANSI.WHITE);
        Main.drawRectangle(0, 0, W - 1, g.getSize().getRows() - 1);
        Main.drawText(W / 2, 3, "Solving seed: " + seed + "...");
        Main.screen.refresh(Screen.RefreshType.AUTOMATIC);
        System.out.println("Solving seed " + seed + " (" + type + ")...");

        List<Solver.S> path = solve(seed, type);
        if (path == null) {
            String msg = "No solution found for seed: " + seed;
            Main.drawText(W / 2, 3, " ".repeat(msg.length() + 20));
            Main.drawText(W / 2, 3, msg);
            System.out.println(msg);
            Main.screen.refresh(Screen.RefreshType.AUTOMATIC);
            Thread.sleep(1000);
            return false;
        }
        System.out.println("Solution has " + (path.size() - 1) + " moves; playing all but the last");

        game = new Solitaire(type, seed);
        curCol = 0; curRow = 0;
        Main.getInstance().setCurrentScreen(game); // so resizes redraw the game, not the options
        Main.screen.refresh(Screen.RefreshType.COMPLETE);
        Thread.sleep(500);

        try {
            String err = playAllButLast(path, delay);
            if (err != null) System.out.println(err);
            else System.out.println("Last move: " + path.getLast().mv);
        } catch (Exception e) {
            // An exception escaping into Main's key thread would stop all input; leave the game playable.
            System.out.println("AutoPlay stopped: " + e);
            e.printStackTrace();
        }
        return true;
    }

    static List<Solver.S> solve(long seed, Solitaire.GameType type) {
        Solver.S sol = Solver.solve(Solver.deal(seed, type), 20000, 3.0);
        if (sol == null) sol = Solver.solve(Solver.deal(seed, type), 2000000, 1.5);
        if (sol == null) return null;
        List<Solver.S> path = new ArrayList<>();
        for (Solver.S s = sol; s != null; s = s.parent) path.add(s);
        Collections.reverse(path);
        return path;
    }

    private static void standaloneLoop() throws Exception {
        while (true) {
            if (Main.screen.doResizeIfNecessary() != null) {
                game.resize();
                Main.screen.refresh(Screen.RefreshType.DELTA);
            }
            KeyStroke key = Main.screen.pollInput();
            if (key == null) { Thread.sleep(10); continue; }
            switch (game.handleInput(key)) {
                case BACK_TO_MENU -> { Main.screen.close(); System.exit(0); }
                case RESTART -> game = (Solitaire) game.restart();
                case NEW_GAME -> game = (Solitaire) game.newGame();
                case NONE -> { continue; }
            }
            Main.screen.refresh(Screen.RefreshType.COMPLETE);
        }
    }

    /** Returns null on success, or a description of the first desync between model and game. */
    static String playAllButLast(List<Solver.S> path, int delay) throws Exception {
        for (int i = 1; i < path.size() - 1; i++) {
            Solver.S from = path.get(i - 1), to = path.get(i);
            if (to.mv.equals("redeal")) {
                redeal(delay);
            } else {
                for (KeyStroke key : keysFor(from, to)) {
                    game.handleInput(key);
                    Thread.sleep(delay);
                }
            }
            String err = diff(to);
            if (err != null) return "Desync after move " + i + " (" + to.mv + "): " + err;
        }
        return null;
    }

    // Once the deck is empty, the game restarts it on the next press if the last draw came up short
    // of a full deal (only possible in 3-deal), or the press after that if it was a full one (exhaust,
    // then restart). Earlier waste plays decide which, so watch the discard rather than predict it.
    static void redeal(int delay) throws Exception {
        Deck deck = game.getDeck();
        for (int press = 0; press < 2 && deck.getTopDiscard() != null; press++) {
            game.handleInput(ch('`'));
            Thread.sleep(delay);
        }
    }

    static KeyStroke ch(char c) {
        return new KeyStroke(c, false, false);
    }

    static KeyStroke key(KeyType t) {
        return new KeyStroke(t);
    }

    /** Arrow presses that put the cursor on (col, row), where row indexes the column's face-up cards. */
    static void moveCursor(List<KeyStroke> keys, int col, int row) {
        if (col != curCol) {
            KeyType dir = col > curCol ? KeyType.ArrowRight : KeyType.ArrowLeft;
            for (int n = Math.abs(col - curCol); n > 0; n--) keys.add(key(dir));
            curCol = col;
            curRow = 0; // left/right reset the row
        }
        if (row < 0) return; // only the column matters (destination)
        KeyType dir = row > curRow ? KeyType.ArrowDown : KeyType.ArrowUp;
        for (int n = Math.abs(row - curRow); n > 0; n--) keys.add(key(dir));
        curRow = row;
    }

    /** Keys for one solver move. Updates the cursor mirror, so every returned key must be pressed. */
    static List<KeyStroke> keysFor(Solver.S from, Solver.S to) {
        String mv = to.mv;
        List<KeyStroke> keys = new ArrayList<>();
        if (mv.equals("draw")) return List.of(ch('`'));

        String[] parts = mv.split(" ");
        String[] fromTo = parts[1].split("->");
        if (fromTo[0].equals("waste")) {
            keys.add(key(KeyType.Tab)); // select the top discard
        } else {
            int col = Integer.parseInt(fromTo[0].substring(3)) - 1;
            int k = -1;
            for (int idx = 0; idx < from.col[col].length; idx++)
                if (Solver.n(from.col[col][idx]).equals(parts[0])) k = idx;
            moveCursor(keys, col, k - from.down[col]);
            keys.add(ch(' ')); // select the card under the cursor (and everything below it)
        }
        if (fromTo[1].equals("F")) keys.add(ch('q')); // any foundation key moves the selection there
        else {
            moveCursor(keys, Integer.parseInt(fromTo[1].substring(3)) - 1, -1);
            keys.add(ch(' ')); // drop onto the cursor's column
        }
        return keys;
    }

    static boolean matches(Card card, int c) {
        return card.getSuit().ordinal() == Solver.suit(c) && card.getRank().ordinal() == Solver.rank(c);
    }

    static String diff(Solver.S s) {
        for (int i = 0; i < 7; i++) {
            List<Card> col = game.getStock().get(i);
            if (col.size() != s.col[i].length) return "column " + (i + 1) + " size " + col.size() + " != " + s.col[i].length;
            for (int k = 0; k < col.size(); k++)
                if (!matches(col.get(k), s.col[i][k])) return "column " + (i + 1) + " card " + k + " differs";
        }
        for (Card.Suit suit : Card.Suit.values()) {
            Card top = game.getFoundation().getTopCard(suit);
            int have = top == null ? 0 : top.getRank().ordinal() + 1;
            if (have != s.fnd[suit.ordinal()]) return "foundation " + suit + " at " + have + " != " + s.fnd[suit.ordinal()];
        }
        Card top = game.getDeck().getTopDiscard();
        if (s.pos == 0 ? top != null : top == null || !matches(top, s.stock[s.pos - 1])) return "waste top differs";
        List<Card> left = game.getDeck().getCardsInDeck();
        if (left.size() != s.stock.length - s.pos) return "deck size " + left.size() + " != " + (s.stock.length - s.pos);
        for (int k = 0; k < left.size(); k++)
            if (!matches(left.get(k), s.stock[s.pos + k])) return "deck card " + k + " differs";
        return null;
    }
}
// @formatter:on
