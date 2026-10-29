package me.pintoadmin.cardTerminal;

import static me.pintoadmin.cardTerminal.Card.Rank.*;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;
import com.googlecode.lanterna.terminal.swing.*;
import java.awt.event.*;
import java.io.*;
import me.pintoadmin.cardTerminal.games.*;

// Unicode suits:♠ ♣ ♥ ♦
// Empty versions: ♤ ♧ ♡ ♢
// Cards: 🂡 https://unicode.org/Public/18.0.0/charts/PDF/U1F0A0.pdf
// Other unicodes: █ ▌▐ ▀▄
// Box characters: ┤├ (https://gist.github.com/flaviut/0db1aec4cadf2ef06455)
/*
Card mockups (Should render correctly in terminal)
w = 9, h = 7
  ╭───────╮
  │A      │
  │♠      │
  │   ♠   │
  │      ♠│
  │      A│
  ╰───────╯
  ╭───────╮
  │7      │
  │♥      │
  │   ♥   │
  │      ♥│
  │      7│
  ╰───────╯
  ╭───────╮
  │10     │
  │♦      │
  │   ♦   │
  │      ♦│
  │     10│
  ╰───────╯

  ╭───────╮  ╭───────╮  ╭───────╮
  │K      │  │Q      │  │J      │
  │♠      │  │♥      │  │♣      │
  │   ♚   │  │   ♛   │  │   ♞   │
  │      ♠│  │      ♥│  │      ♣│
  │      K│  │      Q│  │      J│
  ╰───────╯  ╰───────╯  ╰───────╯
  ╭───────╮  ╭───────╮
  │★      │  │☆      │
  │       │  │       │
  │ JOKER │  │ JOKER │
  │       │  │       │
  │      ★│  │      ☆│
  ╰───────╯  ╰───────╯
  ╭───────╮  ╭───────╮  ╭───────╮
  │╳╳╳╳╳╳╳│  │░▒░▒░▒░│  │◆◇◆◇◆◇◆│
  │╳╳╳╳╳╳╳│  │▒░▒░▒░▒│  │◇◆◇◆◇◆◇│
  │╳╳╳╳╳╳╳│  │░▒░▒░▒░│  │◆◇◆◇◆◇◆│
  │╳╳╳╳╳╳╳│  │▒░▒░▒░▒│  │◇◆◇◆◇◆◇│
  │╳╳╳╳╳╳╳│  │░▒░▒░▒░│  │◆◇◆◇◆◇◆│
  ╰───────╯  ╰───────╯  ╰───────╯
 */
public class Main {
    public static Terminal terminal;
    public static Screen screen;
    public static TextGraphics g;

    public static char SPADE = '♤';
    public static char CLUB = '♧';
    public static char HEART = '♥';
    public static char DIAMOND = '♦';

    public static TextColor BLACK;

    private Game currentGame;

    public enum OS {
        WINDOWS,
        MAC,
        LINUX
    }

    void main(String[] args) {
        try {
            terminal = new DefaultTerminalFactory().createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();

            g = screen.newTextGraphics();
            BLACK = g.getBackgroundColor();
            g.setBackgroundColor(BLACK);
            g.setForegroundColor(TextColor.ANSI.WHITE);

            screen.setCursorPosition(null);

            screen.clear();
            g.fill(' ');

            currentGame = new Solitaire();
            currentGame.redraw();
            screen.refresh(Screen.RefreshType.COMPLETE);

            Thread resizeHandler =
                    new Thread(
                            () -> {
                                while (true) {
                                    TerminalSize newSize = screen.doResizeIfNecessary();
                                    if (newSize != null) {
                                        currentGame.resize();
                                        try {
                                            screen.refresh(Screen.RefreshType.DELTA);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }
                            });
            resizeHandler.start();

            Thread keyHandlers =
                    new Thread(
                            () -> {
                                while (true) {
                                    try {
                                        KeyStroke keystroke = screen.pollInput();
                                        if (keystroke == null) continue;
                                        if (keystroke.getKeyType() == KeyType.Escape) {
                                            screen.close();
                                            Thread.currentThread().interrupt();
                                            resizeHandler.interrupt();
                                            System.exit(0);
                                            break;
                                        } else {
                                            if (keystroke.getKeyType() == KeyType.Character
                                                    && keystroke.getCharacter() == 'r') {
                                                screen.clear();
                                                currentGame = new Solitaire(currentGame.getSeed());
                                                continue;
                                            }
                                            currentGame.handleInput(keystroke);
                                        }
                                    } catch (IOException e) {
                                        System.err.println(
                                                "Could not read terminal input: " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                            });
            keyHandlers.start();

            if (terminal instanceof SwingTerminalFrame swingTerminalFrame) {
                swingTerminalFrame.addWindowListener(
                        new WindowAdapter() {
                            @Override
                            public void windowClosing(WindowEvent e) {
                                try {
                                    keyHandlers.interrupt();
                                    resizeHandler.interrupt();
                                } catch (RuntimeException _) {
                                }
                                System.exit(0);
                            }
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Card.Rank intToRank(Integer i) {
        return switch (i) {
            case 1 -> ACE;
            case 2 -> TWO;
            case 3 -> THREE;
            case 4 -> FOUR;
            case 5 -> FIVE;
            case 6 -> SIX;
            case 7 -> SEVEN;
            case 8 -> EIGHT;
            case 9 -> NINE;
            case 10 -> TEN;
            case 11 -> JACK;
            case 12 -> QUEEN;
            case 13 -> KING;
            case 14 -> JOKER;
            default -> null;
        };
    }

    public static void drawText(int x, int y, String text) {
        x -= (text.length() / 2);
        for (char c : text.toCharArray()) {
            g.setCharacter(x++, y, c);
        }
    }

    public static void drawTextLeft(int x, int y, String text) {
        for (char c : text.toCharArray()) {
            g.setCharacter(x++, y, c);
        }
    }

    public static void drawTextRight(int x, int y, String text) {
        x -= text.length();
        for (char c : text.toCharArray()) {
            g.setCharacter(x++, y, c);
        }
    }

    public static OS getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        OS type = OS.LINUX;

        if (os.contains("win")) {
            type = OS.WINDOWS;
        } else if (os.contains("mac")) {
            type = OS.MAC;
        }

        return type;
    }
}
