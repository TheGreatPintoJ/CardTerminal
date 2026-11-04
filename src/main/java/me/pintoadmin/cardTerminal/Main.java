package me.pintoadmin.cardTerminal;

import static me.pintoadmin.cardTerminal.Card.Rank.*;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;
import com.googlecode.lanterna.terminal.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.function.*;
import me.pintoadmin.cardTerminal.games.*;
import me.pintoadmin.cardTerminal.screens.*;

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
    private static volatile Main instance;

    public Main() {}

    public static Terminal terminal;
    public static Screen screen;
    public static TextGraphics g;

    public static char SPADE = '♤';
    public static char CLUB = '♧';
    public static char HEART = '♥';
    public static char DIAMOND = '♦';

    public static TextColor BLACK;

    private CustomScreen currentScreen;
    private Supplier<Game> currentGame =
            () -> currentScreen instanceof Game ? (Game) currentScreen : null;

    public enum OS {
        WINDOWS,
        MAC,
        LINUX
    }

    void main(String[] args) {
        instance = this;
        try {
            terminal = new DefaultTerminalFactory().createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();

            if (terminal instanceof SwingTerminalFrame swingTerminalFrame) {
                swingTerminalFrame.setTitle("CardTerminal");
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                swingTerminalFrame.setSize(
                        (int) (screenSize.getWidth() / 2),
                        (int) (screenSize.getHeight() / 2)); // width and height in pixels
                swingTerminalFrame.setLocationRelativeTo(null); // center on screen
            }

            g = screen.newTextGraphics();
            BLACK = g.getBackgroundColor();
            g.setBackgroundColor(BLACK);
            g.setForegroundColor(TextColor.ANSI.WHITE);

            screen.setCursorPosition(null);

            screen.clear();
            g.fill(' ');

            screen.refresh(Screen.RefreshType.COMPLETE);
            currentScreen = new Startup();
            Thread resizeHandler = getResizeHandler();
            Thread keyHandlers = getKeyHandlers(resizeHandler);
            Thread.sleep(2000);

            currentScreen = new MainMenu();

            screen.refresh(Screen.RefreshType.COMPLETE);

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
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Thread getKeyHandlers(Thread resizeHandler) {
        Thread keyHandlers =
                new Thread(
                        () -> {
                            while (true) {
                                try {
                                    KeyStroke keystroke = screen.pollInput();
                                    if (keystroke == null) continue;
                                    try {
                                        currentScreen.handleInput(keystroke);
                                        if (keystroke.getKeyType() == KeyType.Escape) {
                                            screen.close();
                                            Thread.currentThread().interrupt();
                                            resizeHandler.interrupt();
                                            System.exit(0);
                                            break;
                                        }
                                    } catch (Exception e) {
                                        if (e.getMessage() == null) throw new RuntimeException(e);
                                        if (e.getMessage().equals("override")) {
                                        } else if (e.getMessage().equals("reset")) {
                                            screen.clear();
                                            if (currentGame.get() != null)
                                                currentScreen = currentGame.get().restart();
                                            screen.refresh(Screen.RefreshType.COMPLETE);
                                        } else if (e.getMessage().equals("newseed")) {
                                            screen.clear();
                                            if (currentGame.get() != null)
                                                currentScreen = currentGame.get().newGame();
                                            screen.refresh(Screen.RefreshType.COMPLETE);
                                        } else throw new RuntimeException(e);
                                    }
                                } catch (IOException e) {
                                    System.err.println(
                                            "Could not read terminal input: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        });
        keyHandlers.start();
        return keyHandlers;
    }

    private Thread getResizeHandler() {
        Thread resizeHandler =
                new Thread(
                        () -> {
                            while (true) {
                                TerminalSize newSize = screen.doResizeIfNecessary();
                                if (newSize != null) {
                                    currentScreen.resize();
                                    try {
                                        screen.refresh(Screen.RefreshType.COMPLETE);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        });
        resizeHandler.start();
        return resizeHandler;
    }

    public void setCurrentScreen(CustomScreen screen) {
        currentScreen = screen;
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

    public static void drawRectangle(int startX, int startY, int endX, int endY) {
        g.drawLine(startX, startY, endX, startY, '─');
        g.drawLine(startX, endY, endX, endY, '─');
        g.drawLine(startX, startY, startX, endY, '│');
        g.drawLine(endX, startY, endX, endY, '│');

        // Switch corner characters to square for windows (curved corner don't display correctly)
        if (getOS() != OS.WINDOWS) {
            g.setCharacter(startX, startY, '╭');
            g.setCharacter(endX, startY, '╮');
            g.setCharacter(startX, endY, '╰');
            g.setCharacter(endX, endY, '╯');
        } else {
            g.setCharacter(startX, startY, '┌');
            g.setCharacter(endX, startY, '┐');
            g.setCharacter(startX, endY, '└');
            g.setCharacter(endX, endY, '┘');
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

    public static Main getInstance() {
        // First check (no locking overhead)
        if (instance == null) {
            synchronized (Main.class) {
                // Second check (inside synchronized block)
                if (instance == null) {
                    instance = new Main();
                }
            }
        }
        return instance;
    }
}
