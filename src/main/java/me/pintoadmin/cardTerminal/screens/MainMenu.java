package me.pintoadmin.cardTerminal.screens;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;
import java.io.*;
import java.util.*;
import java.util.function.*;
import me.pintoadmin.cardTerminal.*;

public class MainMenu extends CustomScreen {
    private final Screen screen = Main.screen;
    private final TextGraphics g = Main.g;

    private final Supplier<Integer> W = () -> g.getSize().getColumns();
    private final Supplier<Integer> H = () -> g.getSize().getRows();

    private final Map<String, BiConsumer<Integer, Integer>> allGames = new LinkedHashMap<>();

    private final int gridW = 4;
    private final int gridH = 3;
    private final int totalSpaces = (gridW * gridH);

    private int selectedGame = 0;

    public MainMenu() {
        allGames.put(
                "Solitaire",
                (x, y) -> {
                    // Solitaire
                    int cw = 4, ch = 3; // card with/height
                    for (int column = 0; column < 20; column++) {
                        int sX = 1 + x + column * cw + column;
                        int eX = sX + cw;
                        if (eX >= x + W.get() / gridW) break;
                        for (int row = 0; row <= column; row++) {
                            int sY = 1 + y + row * ch - row * 2;
                            int eY = sY + ch;
                            g.fillRectangle(
                                    new TerminalPosition(sX, sY), new TerminalSize(cw, ch), ' ');
                            Main.drawRectangle(sX, sY, eX, eY);
                        }
                    }
                });
        allGames.put("Solitaire2", allGames.get("Solitaire"));
        allGames.put("Solitaire3", allGames.get("Solitaire"));
        allGames.put("Solitaire4", allGames.get("Solitaire"));
        allGames.put("Solitaire5", allGames.get("Solitaire"));
        allGames.put("Solitaire6", allGames.get("Solitaire"));
        allGames.put("Solitaire7", allGames.get("Solitaire"));
        allGames.put("Solitaire8", allGames.get("Solitaire"));
        allGames.put("Solitaire9", allGames.get("Solitaire"));
        allGames.put("Solitaire10", allGames.get("Solitaire"));
        allGames.put("Solitaire11", allGames.get("Solitaire"));
        allGames.put("Solitaire12", allGames.get("Solitaire"));

        resize();
    }

    private void build() throws IOException {
        screen.clear();
        g.setBackgroundColor(Main.BLACK);
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.enableModifiers(SGR.BOLD);
        g.disableModifiers(SGR.REVERSE);

        int W = this.W.get(), H = this.H.get();

        Main.drawRectangle(0, 0, W - 1, H - 1);
        Main.drawText(W / 2, 0, " Games ");

        if (allGames.size() > totalSpaces) Main.drawText(W / 2, H - 1, " < --- > ");

        for (Map.Entry<String, BiConsumer<Integer, Integer>> game : allGames.entrySet()) {
            g.disableModifiers(SGR.REVERSE);
            int i = allGames.keySet().stream().toList().indexOf(game.getKey());
            // padding + (index mod width * fullWidth / width)
            int x = 1 + (i % gridW * (W / gridW));
            int y = 1 + (i / gridW * H / gridH);
            game.getValue().accept(x, y);

            if (i == selectedGame) g.enableModifiers(SGR.REVERSE);

            Main.drawRectangle(x, y, x + (W - 2) / gridW - 1, y + (H - 2) / gridH - 1);
            Main.drawText(
                    (x + (W - 2) / gridW / 2 + 1),
                    y + (H - 2) / gridH - 1,
                    " " + game.getKey() + " ");
        }

        g.disableModifiers(SGR.BOLD);
        screen.refresh(Screen.RefreshType.COMPLETE);
    }

    @Override
    public void resize() {
        try {
            build();
        } catch (IOException e) {
            System.out.println("Could not draw main menu: " + e.getMessage());
        }
    }

    @Override
    public void handleInput(KeyStroke keyStroke) throws Exception {
        if (keyStroke.getKeyType().name().contains("Arrow")) {
            switch (keyStroke.getKeyType()) {
                case ArrowRight:
                    if (selectedGame + 1 < totalSpaces) selectedGame++;
                    break;
                case ArrowLeft:
                    if (selectedGame - 1 >= 0) selectedGame--;
                    break;
                case ArrowDown:
                    if (selectedGame + gridW < totalSpaces) selectedGame += gridW;
                    break;
                case ArrowUp:
                    if (selectedGame - gridW >= 0) selectedGame -= gridW;
                    break;
            }
            build();
        }
    }
}
