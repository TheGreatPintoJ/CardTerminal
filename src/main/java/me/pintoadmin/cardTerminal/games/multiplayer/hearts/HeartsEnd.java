package me.pintoadmin.cardTerminal.games.multiplayer.hearts;

import static me.pintoadmin.cardTerminal.Main.*;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import java.io.*;
import java.util.*;
import me.pintoadmin.cardTerminal.*;
import me.pintoadmin.cardTerminal.screens.*;

public class HeartsEnd extends EndScreen {
    private final Screen screen = Main.screen;
    private final TextGraphics g = Main.g;
    private final HeartsLocal heartsLocal;
    private final HeartsLocal.Stats stats;

    public HeartsEnd(HeartsLocal local) {
        this.heartsLocal = local;
        super(String.valueOf(local.getSeed()), "Solitaire", new HashMap<>());
        stats = heartsLocal.newStats();

        addStat("Seed", String.valueOf(heartsLocal.getSeed()));
        addStat("Player", String.valueOf(stats.player));
        addStat("Winner", String.valueOf(stats.winner));
        addStat("Elapsed Time", String.format("%.3f", stats.elapsed / 1000.0) + "s");

        show();
        StatsManager.addGame("hearts", stats);
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
                    Main.getInstance().setCurrentScreen(heartsLocal.newGame());
                    break;
                case 'r':
                    Main.getInstance().setCurrentScreen(heartsLocal.restart());
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
            if (stats.winner == stats.player) {
                // @formatter:off
                // https://patorjk.com/software/taag (coder mini)
                Main.drawText(W/2, 3, "▄▄▄   ▄▄▄   ▄▄▄▄▄   ▄▄▄  ▄▄▄   ▄▄▄▄  ▄▄▄  ▄▄▄▄ ▄▄▄▄▄ ▄▄▄    ▄▄▄");
                Main.drawText(W/2, 4, "███   ███ ▄███████▄ ███  ███   ▀███  ███  ███▀  ███  ████▄  ███");
                Main.drawText(W/2, 5, "▀███▄███▀ ███   ███ ███  ███    ███  ███  ███   ███  ███▀██▄███");
                Main.drawText(W/2, 6, "  ▀███▀   ███▄▄▄███ ███▄▄███    ███▄▄███▄▄███   ███  ███  ▀████");
                Main.drawText(W/2, 7, "   ███     ▀█████▀  ▀██████▀     ▀████▀████▀   ▄███▄ ███    ███");
                // @formatter:on
            } else {
                // @formatter:off
                Main.drawText(W/2, 3, " ▄▄▄▄▄▄▄    ▄▄▄▄   ▄▄▄      ▄▄▄  ▄▄▄▄▄▄▄     ▄▄▄▄▄   ▄▄▄▄  ▄▄▄▄  ▄▄▄▄▄▄▄ ▄▄▄▄▄▄▄  ");
                Main.drawText(W/2, 4, "███▀▀▀▀▀  ▄██▀▀██▄ ████▄  ▄████ ███▀▀▀▀▀   ▄███████▄ ▀███  ███▀ ███▀▀▀▀▀ ███▀▀███▄");
                Main.drawText(W/2, 5, "███       ███  ███ ███▀████▀███ ███▄▄      ███   ███  ███  ███  ███▄▄    ███▄▄███▀");
                Main.drawText(W/2, 6, "███  ███▀ ███▀▀███ ███  ▀▀  ███ ███        ███▄▄▄███  ███▄▄███  ███      ███▀▀██▄ ");
                Main.drawText(W/2, 7, "▀██████▀  ███  ███ ███      ███ ▀███████    ▀█████▀    ▀████▀   ▀███████ ███  ▀███");
                // @formatter:on
            }
            terminal.bell();
            g.disableModifiers(SGR.BLINK);

            int startY = 15;
            Main.drawText(
                    W / 2, startY - 2, "Hearts" + (heartsLocal.isRemoteGame() ? " (remote)" : ""));
            Main.drawRectangle(W / 4 - 1, startY - 1, 3 * W / 4 + 1, startY + gameStats.size());
            for (Map.Entry<String, String> stat : gameStats.entrySet()) {
                String k = stat.getKey(), v = stat.getValue();

                Main.drawTextLeft(W / 4, startY, k);
                Main.drawTextRight(3 * W / 4, startY, v);

                startY++;
            }

            Main.drawRectangle(W / 4 - 1, startY + 1, 3 * W / 4 + 1, startY + 5);
            for (Map.Entry<Integer, Integer> score :
                    heartsLocal.scores.entrySet().stream().sorted().toList()) {
                Main.drawTextLeft(W / 4, startY, "Player " + score.getKey());
                Main.drawTextRight(3 * W / 4, startY, String.valueOf(score.getValue()));

                startY++;
            }

            Main.drawText(W / 2, H - 5, "Press N for a new game, or R to restart this seed");

            screen.refresh(Screen.RefreshType.COMPLETE);
        } catch (IOException e) {
            System.out.println("Ignored exception: " + e.getMessage());
        }
    }
}
