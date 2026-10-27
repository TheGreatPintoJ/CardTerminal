package me.pintoadmin.cardTerminal.screens;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;
import me.pintoadmin.cardTerminal.*;

public class Startup {
    private final Screen screen = Main.screen;
    private final Terminal terminal = Main.terminal;
    private final TextGraphics g = Main.g;

    private final int W = g.getSize().getColumns();
    private final int H = g.getSize().getRows();

    public Startup() {
        build();
    }

    private void build() {
        g.setCharacter(1, 1, '�');
    }
}
