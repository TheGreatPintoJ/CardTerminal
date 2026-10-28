package me.pintoadmin.cardTerminal;

import java.util.UUID;

public abstract class Move {
    public abstract void execute();

    public abstract UUID getUUID();
}
