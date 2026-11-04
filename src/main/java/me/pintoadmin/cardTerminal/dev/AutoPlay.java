// @formatter:off
package me.pintoadmin.cardTerminal.dev;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;
import com.googlecode.lanterna.terminal.swing.*;
import java.awt.event.*;
import java.util.*;
import me.pintoadmin.cardTerminal.*;
import me.pintoadmin.cardTerminal.games.*;

/**
 * Starts Solitaire on a seed, solves it with Solver, and plays every move except the last one by
 * feeding keystrokes into Solitaire.handleInput. Afterwards you play normally (Esc to quit).
 *
 * <p>Run via autoplay.ps1 / autoplay.sh, or: java -cp build/autoplay-classes;lanterna.jar
 * AutoPlay.java [seed] [delayMs]
 */
public class AutoPlay {
    static Game game;

    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 4154672263252710606L;
        int delay = args.length > 1 ? Integer.parseInt(args[1]) : 60;

        System.out.println("Solving seed " + seed + "...");
        Solver.S sol = Solver.solve(Solver.deal(seed), 20000, 3.0);
        if (sol == null) sol = Solver.solve(Solver.deal(seed), 2000000, 1.5);
        if (sol == null) {
            System.out.println("No solution found for seed " + seed);
            return;
        }
        List<Solver.S> path = new ArrayList<>();
        for (Solver.S s = sol; s != null; s = s.parent) path.add(s);
        Collections.reverse(path);
        System.out.println("Solution has " + (path.size() - 1) + " moves; playing all but the last");

        Main.terminal = new DefaultTerminalFactory().createTerminal();
        Main.screen = new TerminalScreen(Main.terminal);
        Main.screen.startScreen();
        Main.g = Main.screen.newTextGraphics();
        Main.BLACK = Main.g.getBackgroundColor();
        Main.g.setBackgroundColor(Main.BLACK);
        Main.g.setForegroundColor(TextColor.ANSI.WHITE);
        Main.screen.setCursorPosition(null);
        Main.screen.clear();

        game = new Solitaire(Solitaire.GameType.KLONDIKE1, seed);
        Main.screen.refresh(Screen.RefreshType.COMPLETE);
        Thread.sleep(500);

        for (int i = 1; i < path.size() - 1; i++) {
            Solver.S from = path.get(i - 1), to = path.get(i);
            for (KeyStroke key : keysFor(from, to)) {
                game.handleInput(key);
                Thread.sleep(delay);
            }
            String err = diff(to);
            if (err != null) {
                System.out.println("Desync after move " + i + " (" + to.mv + "): " + err);
                break;
            }
        }
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
                if ("reset".equals(e.getMessage())) game = new Solitaire(Solitaire.GameType.KLONDIKE1, game.getSeed());
                else if ("newseed".equals(e.getMessage())) game = new Solitaire();
                else throw e;
                Main.screen.clear();
                Main.screen.refresh(Screen.RefreshType.COMPLETE);
            }
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
        // Model redeal = discard last drawn card (exhaust), then restart the deck
        if (mv.equals("redeal")) return List.of(ch('`'), ch('`'));

        String[] parts = mv.split(" ");
        String[] fromTo = parts[1].split("->");
        if (fromTo[0].equals("waste")) {
            int c = from.stock[from.pos - 1];
            Card top = ((Solitaire) game).getDeck().getTopDiscard();
            boolean onDiscard = top != null && matches(top, c);
            keys.add(onDiscard ? new KeyStroke(KeyType.Tab) : ch(' '));
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
        return null;
    }
}
// @formatter:on
