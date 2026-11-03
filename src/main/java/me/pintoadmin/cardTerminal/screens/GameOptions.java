package me.pintoadmin.cardTerminal.screens;

import me.pintoadmin.cardTerminal.CustomScreen;

public abstract class GameOptions extends CustomScreen {
    private final String name;

    public GameOptions(String name) {
        this.name = name;
    }

    public abstract void setSeed(long seed);

    public abstract void start();

    public String getName() {
        return name;
    }
}
