package me.pintoadmin.cardTerminal;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
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

    public enum CardSuit {
        SPADE,
        CLUB,
        HEART,
        DIAMOND
    }

    public enum CardNumber {
        ACE,
        TWO,
        THREE,
        FOUR,
        FIVE,
        SIX,
        SEVEN,
        EIGHT,
        NINE,
        TEN,
        JACK,
        QUEEN,
        KING
    }

    private enum OS {
        WINDOWS,
        MAC,
        LINUX
    }

    static void main(String[] args) {
        try {
            terminal = new DefaultTerminalFactory().createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();

            g = screen.newTextGraphics();
            g.setBackgroundColor(TextColor.ANSI.BLACK);
            g.setForegroundColor(TextColor.ANSI.WHITE);

            screen.setCursorPosition(null);

            screen.clear();
            g.fill(' ');

            new SolitaireSetup();

            //            new Card(CardNumber.ACE, CardSuit.SPADE).draw(3, 3, true);
            //            new Card(CardNumber.JACK, CardSuit.DIAMOND).draw(13, 3, false);

            screen.refresh(Screen.RefreshType.COMPLETE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void drawCard(int x, int y, CardNumber number, CardSuit suit) throws IOException {
        g.setBackgroundColor(TextColor.ANSI.BLACK);
        g.setForegroundColor(TextColor.ANSI.WHITE);

        g.drawLine(x + 1, y, x + 7, y, '─');
        g.drawLine(x + 1, y + 6, x + 7, y + 6, '─');
        g.drawLine(x, y + 1, x, y + 5, '│');
        g.drawLine(x + 8, y + 1, x + 8, y + 5, '│');

        // Switch corner characters to square for windows (curved corner don't display correctly)
        if (getOS() != OS.WINDOWS) {
            g.setCharacter(x, y, '╭');
            g.setCharacter(x + 8, y, '╮');
            g.setCharacter(x, y + 6, '╰');
            g.setCharacter(x + 8, y + 6, '╯');
        } else {
            g.setCharacter(x, y, '┌');
            g.setCharacter(x + 8, y, '┐');
            g.setCharacter(x, y + 6, '└');
            g.setCharacter(x + 8, y + 6, '┘');
        }

        // Draw card number
        if (numToString(number).length() == 1) {
            g.setCharacter(x + 1, y + 1, numToString(number).charAt(0));
            g.setCharacter(x + 7, y + 5, numToString(number).charAt(0));
        } else {
            g.setCharacter(x + 1, y + 1, numToString(number).charAt(0));
            g.setCharacter(x + 2, y + 1, numToString(number).charAt(1));
            g.setCharacter(x + 6, y + 5, numToString(number).charAt(0));
            g.setCharacter(x + 7, y + 5, numToString(number).charAt(1));
        }

        // Draw card suit
        char cardSuit =
                switch (suit) {
                    case SPADE -> SPADE;
                    case CLUB -> CLUB;
                    case HEART -> HEART;
                    case DIAMOND -> DIAMOND;
                };
        g.setCharacter(x + 1, y + 2, cardSuit);
        g.setCharacter(x + 4, y + 3, cardSuit);
        g.setCharacter(x + 7, y + 4, cardSuit);
    }

    public static void drawCardBack(int x, int y) throws IOException {
        g.setBackgroundColor(TextColor.ANSI.BLACK);
        g.setForegroundColor(TextColor.ANSI.WHITE);

        g.drawLine(x + 1, y, x + 7, y, '─');
        g.drawLine(x + 1, y + 6, x + 7, y + 6, '─');
        g.drawLine(x, y + 1, x, y + 5, '│');
        g.drawLine(x + 8, y + 1, x + 8, y + 5, '│');

        // Switch corner characters to square for windows (curved corner don't display correctly)
        if (getOS() != OS.WINDOWS) {
            g.setCharacter(x, y, '╭');
            g.setCharacter(x + 8, y, '╮');
            g.setCharacter(x, y + 6, '╰');
            g.setCharacter(x + 8, y + 6, '╯');
        } else {
            g.setCharacter(x, y, '┌');
            g.setCharacter(x + 8, y, '┐');
            g.setCharacter(x, y + 6, '└');
            g.setCharacter(x + 8, y + 6, '┘');
        }

        for (int xi = 1; xi < 8; xi++) {
            for (int yi = 1; yi < 6; yi++) {
                g.setBackgroundColor(TextColor.ANSI.WHITE);
                g.setForegroundColor(TextColor.ANSI.BLACK);
                g.setCharacter(x + xi, y + yi, ' ');
                //                g.setCharacter(x + xi, y + yi, xi + yi % 2 == 0 ? '░' : '▒');
            }
        }
    }

    public static String numToString(CardNumber num) {
        return switch (num) {
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

    private static OS getOS() {
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
