package me.pintoadmin.cardTerminal.screens;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import java.io.*;
import java.util.*;
import java.util.function.*;
import me.pintoadmin.cardTerminal.*;
import me.pintoadmin.cardTerminal.games.multiplayer.hearts.*;
import me.pintoadmin.cardTerminal.games.solitaire.*;
import me.pintoadmin.cardTerminal.games.spider.*;

public class MainMenu extends CustomScreen {
    private final Screen screen = Main.screen;
    private final TextGraphics g = Main.g;

    private final Supplier<Integer> W = () -> g.getSize().getColumns();
    private final Supplier<Integer> H = () -> g.getSize().getRows();

    private final Map<GameOptions, QuadConsumer<Integer, Integer, Integer, Integer>> allGames =
            new LinkedHashMap<>();

    private final int gridW = 4;
    private final int gridH = 3;
    private final int totalSpaces = (gridW * gridH);

    private int selectedGame = 0;

    public MainMenu() {
        allGames.put(
                new SolitaireOptions("Solitaire"),
                (x, y, w, h) -> {
                    // Solitaire
                    int cw = 4, ch = 3; // card with/height
                    columnLoop:
                    for (int column = 0; column < 20; column++) {
                        int sX = 1 + x + column * cw + column;
                        int eX = sX + cw;
                        if (eX >= x + W.get() / gridW) break;
                        for (int row = 0; row <= column; row++) {
                            int sY = 1 + y + row * ch - row * 2;
                            int eY = sY + ch;
                            if (eY >= y + (H.get() - 2) / gridH - 2) break columnLoop;
                            g.fillRectangle(
                                    new TerminalPosition(sX, sY), new TerminalSize(cw, ch), ' ');
                            Main.drawRectangle(sX, sY, eX, eY);
                        }
                    }
                });
        allGames.put(
                new SpiderOptions("Spider"),
                (x, y, w, h) -> {
                    // Spider
                    int cw = 4, ch = 3; // card with/height
                    for (int column = 0; column < 10; column++) {
                        int sX = 1 + x + column * cw + column;
                        int eX = sX + cw;
                        if (eX >= x + W.get() / gridW) break;
                        for (int row = 0; row <= 5; row++) {
                            int sY = 1 + y + row * ch - row * 2;
                            int eY = sY + ch;
                            g.fillRectangle(
                                    new TerminalPosition(sX, sY), new TerminalSize(cw, ch), ' ');
                            Main.drawRectangle(sX, sY, eX, eY);
                        }
                    }
                });
        allGames.put(
                new HeartsOptions("Hearts"),
                (x, y, w, h) -> {
                    // Hearts
                    int cw = 4, ch = 3; // card with/height
                    for (int i = 0; i < 13; i++) {
                        int sX = x + w / 2 - (13 / 2) - (cw / 2) + i;
                        int sY = y + 1;
                        g.fillRectangle(
                                new TerminalPosition(sX, sY), new TerminalSize(cw, ch), ' ');
                        Main.drawRectangle(sX, sY, sX + cw, sY + ch);
                    }
                    for (int i = 0; i < 13; i++) {
                        int sX = x + w / 2 - (13 / 2) - (cw / 2) + i;
                        int sY = y + h - ch - 1;
                        g.fillRectangle(
                                new TerminalPosition(sX, sY), new TerminalSize(cw, ch), ' ');
                        Main.drawRectangle(sX, sY, sX + cw, sY + ch);
                    }
                    for (int i = 0; i < 8; i++) {
                        int sX = x + 1;
                        int sY = y + (h / 2) - (8 / 2) - (ch / 2) + i;
                        g.fillRectangle(
                                new TerminalPosition(sX, sY), new TerminalSize(cw, ch), ' ');
                        Main.drawRectangle(sX, sY, sX + cw, sY + ch);
                    }
                    for (int i = 0; i < 8; i++) {
                        int sX = x + w - cw - 1;
                        int sY = y + (h / 2) - (8 / 2) - (ch / 2) + i;
                        g.fillRectangle(
                                new TerminalPosition(sX, sY), new TerminalSize(cw, ch), ' ');
                        Main.drawRectangle(sX, sY, sX + cw, sY + ch);
                    }
                });

        resize();
    }

    private void build() throws IOException {
        screen.clear();
        Arrays.stream(SGR.values()).toList().forEach(g::disableModifiers);
        g.setBackgroundColor(Main.BLACK);
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.enableModifiers(SGR.BOLD);

        int W = this.W.get(), H = this.H.get();

        Main.drawRectangle(0, 0, W - 1, H - 1);
        Main.drawText(W / 2, 0, " Games ");

        if (allGames.size() > totalSpaces) Main.drawText(W / 2, H - 1, " < --- > ");

        for (Map.Entry<GameOptions, QuadConsumer<Integer, Integer, Integer, Integer>> game :
                allGames.entrySet()) {
            g.disableModifiers(SGR.REVERSE);
            int i = allGames.keySet().stream().toList().indexOf(game.getKey());
            // padding + (index mod width * fullWidth / width)
            int x = 1 + (i % gridW * (W / gridW));
            int y = 1 + (i / gridW * H / gridH);
            if (game.getValue() == null) continue;
            game.getValue().accept(x, y, (W - 2) / gridW - 1, (H - 2) / gridH - 1);

            if (i == selectedGame) g.enableModifiers(SGR.REVERSE);

            Main.drawRectangle(x, y, x + (W - 2) / gridW - 1, y + (H - 2) / gridH - 1);
            Main.drawText(
                    (x + (W - 2) / gridW / 2 + 1),
                    y + (H - 2) / gridH - 1,
                    " " + game.getKey().getName() + " ");
        }

        g.disableModifiers(SGR.BOLD);
        screen.refresh(Screen.RefreshType.COMPLETE);
    }

    @Override
    public void resize() {
        try {
            build();
        } catch (IOException e) {
            System.err.println("Could not draw main menu: " + e.getMessage());
        }
    }

    @Override
    public Action handleInput(KeyStroke keyStroke) {
        if (keyStroke.getKeyType().name().contains("Arrow")) {
            switch (keyStroke.getKeyType()) {
                case ArrowRight:
                    if (selectedGame + 1 < allGames.size()) selectedGame++;
                    break;
                case ArrowLeft:
                    if (selectedGame - 1 >= 0) selectedGame--;
                    break;
                case ArrowDown:
                    if (selectedGame + gridW < allGames.size()) selectedGame += gridW;
                    break;
                case ArrowUp:
                    if (selectedGame - gridW >= 0) selectedGame -= gridW;
                    break;
            }
            try {
                build();
            } catch (IOException e) {
                System.out.println("Ignored Exception: " + e.getMessage());
            }
        } else if (keyStroke.getKeyType() == KeyType.Enter) {
            GameOptions next = allGames.keySet().stream().toList().get(selectedGame);
            next.resize();
            Main.getInstance().setCurrentScreen(next);
        }
        return Action.NONE;
    }

    @FunctionalInterface
    public interface QuadConsumer<T, U, V, W> {
        void accept(T t, U u, V v, W w);

        default QuadConsumer<T, U, V, W> andThen(
                QuadConsumer<? super T, ? super U, ? super V, ? super W> after) {
            Objects.requireNonNull(after);
            return (t, u, v, w) -> {
                accept(t, u, v, w);
                after.accept(t, u, v, w);
            };
        }
    }
}
