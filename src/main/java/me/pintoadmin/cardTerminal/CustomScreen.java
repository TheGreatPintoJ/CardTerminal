package me.pintoadmin.cardTerminal;

import com.googlecode.lanterna.input.*;

public abstract class CustomScreen {
    public abstract void resize();

    public abstract void handleInput(KeyStroke keyStroke) throws Exception;
}
