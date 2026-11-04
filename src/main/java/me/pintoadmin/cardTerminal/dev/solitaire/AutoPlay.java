// @formatter:off
package me.pintoadmin.cardTerminal.dev.solitaire;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;
import com.googlecode.lanterna.terminal.swing.*;
import java.awt.event.*;
import java.util.*;
import me.pintoadmin.cardTerminal.*;
import me.pintoadmin.cardTerminal.games.solitaire.*;

/**
 * Starts Solitaire on a seed, solves it with Solver, and plays every move except the last one by
 * feeding keystrokes into Solitaire.handleInput. Afterwards you play normally (Esc to quit).
 *
 * <p>Run via autoplay.ps1 / autoplay.sh, or: java -cp build/autoplay-classes;lanterna.jar
 * AutoPlay.java [seed] [delayMs] [gameType]
 */
public class AutoPlay {
    static Game game;

    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 4154672263252710606L;
        int delay = args.length > 1 ? Integer.parseInt(args[1]) : 60;
        Solitaire.GameType type = args.length > 2 ? Solitaire.GameType.valueOf(args[2]) : Solitaire.GameType.KLONDIKE1;

        if(Main.terminal == null) Main.terminal = new DefaultTerminalFactory().createTerminal();
        if(Main.screen == null) {
            Main.screen = new TerminalScreen(Main.terminal);
            Main.screen.startScreen();
        }
        if(Main.g == null) Main.g = Main.screen.newTextGraphics();
        if(Main.BLACK == null) Main.BLACK = Main.g.getBackgroundColor();
        Main.g.setBackgroundColor(Main.BLACK);
        Main.g.setForegroundColor(TextColor.ANSI.WHITE);
        Main.screen.setCursorPosition(null);
        Main.screen.clear();

        Main.drawRectangle(0, 0, Main.g.getSize().getColumns() - 1, Main.g.getSize().getRows() - 1);
        Main.drawText(Main.g.getSize().getColumns()/2, 3, "Solving seed: "+seed+"...");
        System.out.println("Solving seed " + seed + "...");
        Main.screen.refresh(Screen.RefreshType.AUTOMATIC);
        Solver.S sol = Solver.solve(Solver.deal(seed, type), 20000, 3.0);
        if (sol == null) sol = Solver.solve(Solver.deal(seed, type), 2000000, 1.5);
        if (sol == null) {
            Main.drawText(Main.g.getSize().getColumns()/2, 3, "No solution found for seed: "+seed);
            System.out.println("No solution found for seed " + seed);
            Main.screen.refresh(Screen.RefreshType.AUTOMATIC);
            Thread.sleep(1000);
            Main.getInstance().setCurrentScreen(new SolitaireOptions("Solitaire"));
            return;
        }
        List<Solver.S> path = new ArrayList<>();
        for (Solver.S s = sol; s != null; s = s.parent) path.add(s);
        Collections.reverse(path);
        System.out.println("Solution has " + (path.size() - 1) + " moves; playing all but the last");


        game = new Solitaire(type, seed);
        Main.screen.refresh(Screen.RefreshType.COMPLETE);
        Thread.sleep(500);

        String err = playAllButLast(path, delay);
        if (err != null) System.out.println(err);
        System.out.println("Last move: " + sol.mv);

        if (Main.terminal instanceof SwingTerminalFrame frame) {
            frame.addWindowListener(
                    new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            System.exit(0);
                        }
                    });
        }
        while (true) {
            if (Main.screen.doResizeIfNecessary() != null) {
                game.resize();
                Main.screen.refresh(Screen.RefreshType.DELTA);
            }
            KeyStroke key = Main.screen.pollInput();
            if (key == null) {
                Thread.sleep(10);
                continue;
            }
            if (key.getKeyType() == KeyType.Escape) {
                Main.screen.close();
                System.exit(0);
            }
            try {
                game.handleInput(key);
            } catch (Exception e) {
                if ("reset".equals(e.getMessage())) game = new Solitaire(type, game.getSeed());
                else if ("newseed".equals(e.getMessage())) game = new Solitaire(type, new Random().nextLong());
                else throw e;
                Main.screen.clear();
                Main.screen.refresh(Screen.RefreshType.COMPLETE);
            }
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
        Deck deck = ((Solitaire) game).getDeck();
        for (int press = 0; press < 2 && deck.getTopDiscard() != null; press++) {
            game.handleInput(ch('`'));
            Thread.sleep(delay);
        }
    }

    static KeyStroke ch(char c) {
        return new KeyStroke(c, false, false);
    }

    static KeyStroke digit(int n) {
        return ch((char) ('0' + n));
    }

    static List<KeyStroke> keysFor(Solver.S from, Solver.S to) {
        String mv = to.mv;
        List<KeyStroke> keys = new ArrayList<>();
        if (mv.equals("draw")) return List.of(ch('`'));

        String[] parts = mv.split(" ");
        String[] fromTo = parts[1].split("->");
        if (fromTo[0].equals("waste")) {
            keys.add(ch(' '));
        } else {
            int col = Integer.parseInt(fromTo[0].substring(3)) - 1;
            int k = -1;
            for (int idx = 0; idx < from.col[col].length; idx++)
                if (Solver.n(from.col[col][idx]).equals(parts[0])) k = idx;
            int faceup = from.col[col].length - from.down[col];
            int row = k - from.down[col] + 1;
            keys.add(digit(col + 1));
            if (faceup > 1) {
                if (row == faceup) keys.add(ch('0'));
                else if (row <= 9) keys.add(digit(row));
                else throw new IllegalStateException("Can't select row " + row + " for " + mv);
            }
        }
        if (fromTo[1].equals("F")) keys.add(ch('q'));
        else keys.add(digit(Integer.parseInt(fromTo[1].substring(3))));
        return keys;
    }

    static boolean matches(Card card, int c) {
        return card.getSuit().ordinal() == Solver.suit(c) && card.getRank().ordinal() == Solver.rank(c);
    }

    static String diff(Solver.S s) {
        Solitaire sol = (Solitaire) game;
        for (int i = 0; i < 7; i++) {
            List<Card> col = sol.getStock().get(i);
            if (col.size() != s.col[i].length) return "column " + (i + 1) + " size " + col.size() + " != " + s.col[i].length;
            for (int k = 0; k < col.size(); k++)
                if (!matches(col.get(k), s.col[i][k])) return "column " + (i + 1) + " card " + k + " differs";
        }
        for (Card.Suit suit : Card.Suit.values()) {
            Card top = sol.getFoundation().getTopCard(suit);
            int have = top == null ? 0 : top.getRank().ordinal() + 1;
            if (have != s.fnd[suit.ordinal()]) return "foundation " + suit + " at " + have + " != " + s.fnd[suit.ordinal()];
        }
        Card top = sol.getDeck().getTopDiscard();
        if (s.pos == 0 ? top != null : top == null || !matches(top, s.stock[s.pos - 1])) return "waste top differs";
        List<Card> left = sol.getDeck().getCardsInDeck(); // cards still in the deck, despite the name
        if (left.size() != s.stock.length - s.pos) return "deck size " + left.size() + " != " + (s.stock.length - s.pos);
        for (int k = 0; k < left.size(); k++)
            if (!matches(left.get(k), s.stock[s.pos + k])) return "deck card " + k + " differs";
        return null;
    }
}
// @formatter:on
