package me.pintoadmin.cardTerminal.screens;

import com.googlecode.lanterna.input.*;
import java.util.*;
import me.pintoadmin.cardTerminal.*;

public abstract class EndScreen extends CustomScreen {
    final String seed;
    final String name;
    protected LinkedHashMap<String, String> gameStats = new LinkedHashMap<>();

    public EndScreen(String seed, String name, Map<String, String> stats) {
        this.seed = seed;
        this.name = name;

        gameStats.putAll(stats);
    }

    @Override
    public abstract void resize();

    @Override
    public abstract Action handleInput(KeyStroke keyStroke);

    public abstract void show();

    public void addStat(String k, String v) {
        gameStats.put(k, v);
    }
}
