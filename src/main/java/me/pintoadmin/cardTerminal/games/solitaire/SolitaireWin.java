package me.pintoadmin.cardTerminal.games.solitaire;

import static me.pintoadmin.cardTerminal.Main.*;
import static me.pintoadmin.cardTerminal.Main.terminal;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import java.io.*;
import java.util.*;
import me.pintoadmin.cardTerminal.*;
import me.pintoadmin.cardTerminal.screens.*;

public class SolitaireWin extends EndScreen {
    private final Screen screen = Main.screen;
    private final TextGraphics g = Main.g;
    private final Solitaire solitaire;
    private final Solitaire.Stats stats;

    public SolitaireWin(Solitaire solitaire) {
        this.solitaire = solitaire;
        super(String.valueOf(solitaire.getSeed()), "Solitaire", new HashMap<>());
        stats = solitaire.newStats();

        addStat("Seed", String.valueOf(solitaire.getSeed()));
        addStat("Moves", String.valueOf(stats.moves));
        addStat("Time", String.format("%.3f", stats.time / 1000.0) + "s");

        show();
    }

    @Override
    public void resize() {
        show();
    }

    @Override
    public Action handleInput(KeyStroke keyStroke) {
        if (keyStroke.getKeyType() == KeyType.Escape) {
            return Action.BACK_TO_MENU;
        } else if (keyStroke.getKeyType() == KeyType.Character) {
            char key = keyStroke.getCharacter();
            switch (key) {
                case 'n':
                    Main.getInstance().setCurrentScreen(solitaire.newGame());
                    break;
                case 'r':
                    Main.getInstance().setCurrentScreen(solitaire.restart());
                    break;
            }
        }
        return Action.NONE;
    }

    @Override
    public void show() {
        try {
            int W = g.getSize().getColumns();
            int H = g.getSize().getRows();

            screen.clear();
            g.enableModifiers(SGR.BLINK);
            g.setBackgroundColor(BLACK);
            g.setForegroundColor(TextColor.ANSI.WHITE);
            // @formatter:off
            Main.drawText(W/2, 3, "▄▄▄   ▄▄▄   ▄▄▄▄▄   ▄▄▄  ▄▄▄   ▄▄▄▄  ▄▄▄  ▄▄▄▄ ▄▄▄▄▄ ▄▄▄    ▄▄▄");
            Main.drawText(W/2, 4, "███   ███ ▄███████▄ ███  ███   ▀███  ███  ███▀  ███  ████▄  ███");
            Main.drawText(W/2, 5, "▀███▄███▀ ███   ███ ███  ███    ███  ███  ███   ███  ███▀██▄███");
            Main.drawText(W/2, 6, "  ▀███▀   ███▄▄▄███ ███▄▄███    ███▄▄███▄▄███   ███  ███  ▀████");
            Main.drawText(W/2, 7, "   ███     ▀█████▀  ▀██████▀     ▀████▀████▀   ▄███▄ ███    ███");
            // @formatter:on
            terminal.bell();
            g.disableModifiers(SGR.BLINK);

            int startY = 15;
            Main.drawText(W/2, startY-2, "Solitaire");
            Main.drawRectangle(W / 4 - 1, startY - 1, 3 * W / 4 + 1, startY + gameStats.size());
            for (Map.Entry<String, String> stat : gameStats.entrySet()) {
                String k = stat.getKey(), v = stat.getValue();

                Main.drawTextLeft(W / 4, startY, k);
                Main.drawTextRight(3 * W / 4, startY, v);

                startY++;
            }

            Main.drawText(W / 2, H - 5, "Press N for a new game, or R to restart this seed");

            screen.refresh(Screen.RefreshType.COMPLETE);
        } catch (IOException e) {
            System.out.println("Ignored exception: " + e.getMessage());
        }
    }
}
