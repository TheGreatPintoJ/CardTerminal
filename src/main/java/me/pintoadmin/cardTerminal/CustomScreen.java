package me.pintoadmin.cardTerminal;

import com.googlecode.lanterna.input.*;

public abstract class CustomScreen {
    public enum Action {
        NONE,
        BACK_TO_MENU,
        RESTART,
        NEW_GAME
    }

    public abstract void resize();

    public abstract Action handleInput(KeyStroke keyStroke);
}
