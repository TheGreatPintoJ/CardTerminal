package me.pintoadmin.cardTerminal;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;
import java.io.*;
import me.pintoadmin.cardTerminal.games.*;

// Unicode suits:♠ ♣ ♥ ♦
// Empty versions: ♤ ♧ ♡ ♢
// Cards: 🂡 https://unicode.org/Public/18.0.0/charts/PDF/U1F0A0.pdf
/*
Card mockups (Should render correctly in terminal)

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

    private Game currentGame;

    enum OS {
        WINDOWS,
        MAC,
        LINUX
    }

    void main(String[] args) {
        try {
            terminal = new DefaultTerminalFactory().createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();

            SimpleTerminalResizeListener resizeListener =
                    new SimpleTerminalResizeListener(terminal.getTerminalSize());
            terminal.addResizeListener(resizeListener);

            g = screen.newTextGraphics();
            g.setBackgroundColor(TextColor.ANSI.BLACK);
            g.setForegroundColor(TextColor.ANSI.WHITE);

            screen.setCursorPosition(null);

            screen.clear();
            g.fill(' ');

            currentGame = new Solitaire();

            screen.refresh(Screen.RefreshType.COMPLETE);

            Thread mainLoop =
                    new Thread(
                            () -> {
                                while (true) {
                                    TerminalSize newSize = screen.doResizeIfNecessary();
                                    if (newSize != null) {
                                        currentGame.redraw();
                                        try {
                                            screen.refresh(Screen.RefreshType.COMPLETE);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                    try {
                                        KeyStroke keystroke = terminal.readInput();
                                        if (keystroke.getKeyType() == KeyType.Escape) {
                                            screen.close();
                                            Thread.currentThread().interrupt();
                                            break;
                                        } else if (keystroke.getKeyType() == KeyType.Enter) {
                                            currentGame.redraw();
                                            screen.refresh(Screen.RefreshType.DELTA);
                                        }
                                    } catch (IOException e) {
                                        System.err.println(
                                                "Could not read terminal input: " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                            });
            mainLoop.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String rankToString(Card.CardRank rank) {
        return switch (rank) {
            case ACE -> "A";
            case TWO -> "2";
            case THREE -> "3";
            case FOUR -> "4";
            case FIVE -> "5";
            case SIX -> "6";
            case SEVEN -> "7";
            case EIGHT -> "8";
            case NINE -> "9";
            case TEN -> "10";
            case JACK -> "J";
            case QUEEN -> "Q";
            case KING -> "K";
        };
    }

    public static int rankToInt(Card.CardRank rank) {
        return switch (rank) {
            case ACE -> 1;
            case TWO -> 2;
            case THREE -> 3;
            case FOUR -> 4;
            case FIVE -> 5;
            case SIX -> 6;
            case SEVEN -> 7;
            case EIGHT -> 8;
            case NINE -> 9;
            case TEN -> 10;
            case JACK -> 11;
            case QUEEN -> 12;
            case KING -> 13;
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

    static OS getOS() {
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
