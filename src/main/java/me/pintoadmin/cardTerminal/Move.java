package me.pintoadmin.cardTerminal;

import java.util.UUID;

public abstract class Move {
    public abstract boolean execute();

    public abstract void undo();

    public abstract UUID getUUID();
}
