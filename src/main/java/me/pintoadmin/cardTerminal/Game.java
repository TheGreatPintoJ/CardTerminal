package me.pintoadmin.cardTerminal;

import static me.pintoadmin.cardTerminal.Main.*;
import static me.pintoadmin.cardTerminal.Main.g;
import static me.pintoadmin.cardTerminal.Main.screen;
import static me.pintoadmin.cardTerminal.Main.terminal;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.screen.*;
import java.io.*;

public abstract class Game extends CustomScreen {
    public abstract Game newGame();

    public abstract Game restart();

    public abstract long getSeed();

    public abstract void redraw();

    public abstract void drawCard(boolean logged);

    protected void win() throws IOException {
        int W = g.getSize().getColumns();
        int H = g.getSize().getRows();

        screen.clear();
        g.enableModifiers(SGR.BLINK);
        g.setBackgroundColor(BLACK);
        g.setForegroundColor(TextColor.ANSI.WHITE);
        // @formatter:off
        Main.drawText(W/2, H/2-2, "▄▄▄   ▄▄▄   ▄▄▄▄▄   ▄▄▄  ▄▄▄   ▄▄▄▄  ▄▄▄  ▄▄▄▄ ▄▄▄▄▄ ▄▄▄    ▄▄▄");
        Main.drawText(W/2, H/2-1, "███   ███ ▄███████▄ ███  ███   ▀███  ███  ███▀  ███  ████▄  ███");
        Main.drawText(W/2, H/2,   "▀███▄███▀ ███   ███ ███  ███    ███  ███  ███   ███  ███▀██▄███");
        Main.drawText(W/2, H/2+1, "  ▀███▀   ███▄▄▄███ ███▄▄███    ███▄▄███▄▄███   ███  ███  ▀████");
        Main.drawText(W/2, H/2+2, "   ███     ▀█████▀  ▀██████▀     ▀████▀████▀   ▄███▄ ███    ███");
        // @formatter:on
        terminal.bell();
        g.disableModifiers(SGR.BLINK);
        screen.refresh(Screen.RefreshType.COMPLETE);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            System.out.println("Ignored exception: " + e.getMessage());
        }

        screen.clear();
    }
}
