package me.pintoadmin.cardTerminal.screens;

import static me.pintoadmin.cardTerminal.Main.BLACK;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;
import java.io.*;
import me.pintoadmin.cardTerminal.*;

public class Startup {
    private final Screen screen = Main.screen;
    private final Terminal terminal = Main.terminal;
    private final TextGraphics g = Main.g;

    private final int W = g.getSize().getColumns();
    private final int H = g.getSize().getRows();

    public Startup() {
        try {
            build();
        } catch (IOException e) {
            System.out.println("Could not draw startup screen: " + e.getMessage());
        }
    }

    private void build() throws IOException {
        g.setBackgroundColor(BLACK);
        g.setForegroundColor(TextColor.ANSI.WHITE);

        // @formatter:off
        Main.drawText(W/2, H/2-4, "  /$$$$$$                            /$$ /$$$$$$$$                                /$$                     /$$");
        Main.drawText(W/2, H/2-3, " /$$__  $$                          | $$|__  $$__/                               |__/                    | $$");
        Main.drawText(W/2, H/2-2, "| $$  \\__/  /$$$$$$   /$$$$$$   /$$$$$$$   | $$  /$$$$$$   /$$$$$$  /$$$$$$/$$$$  /$$ /$$$$$$$   /$$$$$$ | $$");
        Main.drawText(W/2, H/2-1, "| $$       |____  $$ /$$__  $$ /$$__  $$   | $$ /$$__  $$ /$$__  $$| $$_  $$_  $$| $$| $$__  $$ |____  $$| $$");
        Main.drawText(W/2, H/2,   "| $$        /$$$$$$$| $$  \\__/| $$  | $$   | $$| $$$$$$$$| $$  \\__/| $$ \\ $$ \\ $$| $$| $$  \\ $$  /$$$$$$$| $$");
        Main.drawText(W/2, H/2+1, "| $$    $$ /$$__  $$| $$      | $$  | $$   | $$| $$_____/| $$      | $$ | $$ | $$| $$| $$  | $$ /$$__  $$| $$");
        Main.drawText(W/2, H/2+2, "|  $$$$$$/|  $$$$$$$| $$      |  $$$$$$$   | $$|  $$$$$$$| $$      | $$ | $$ | $$| $$| $$  | $$|  $$$$$$$| $$");
        Main.drawText(W/2, H/2+3, " \\______/  \\_______/|__/       \\_______/   |__/ \\_______/|__/      |__/ |__/ |__/|__/|__/  |__/ \\_______/|__/");
        // @formatter:on

        screen.refresh(Screen.RefreshType.COMPLETE);
    }
}
