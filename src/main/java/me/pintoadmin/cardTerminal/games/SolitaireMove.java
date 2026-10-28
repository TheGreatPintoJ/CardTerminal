package me.pintoadmin.cardTerminal.games;

import me.pintoadmin.cardTerminal.Move;

import java.util.UUID;

public class SolitaireMove extends Move {
    private final UUID uuid = UUID.randomUUID();

    @Override
    public void execute() {

    }

    @Override
    public UUID getUUID() {
        return uuid;
    }
}
