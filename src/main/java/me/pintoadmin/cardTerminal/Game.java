package me.pintoadmin.cardTerminal;

import com.googlecode.lanterna.input.KeyStroke;
import java.io.*;

public abstract class Game {
    public abstract long getSeed();

    public abstract void resize();

    public abstract void redraw();

    public abstract void drawCard(boolean logged);

    public abstract void handleInput(KeyStroke keyStroke) throws IOException;
}
