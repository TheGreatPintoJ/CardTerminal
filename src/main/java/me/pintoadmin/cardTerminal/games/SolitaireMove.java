package me.pintoadmin.cardTerminal.games;

import java.util.UUID;
import me.pintoadmin.cardTerminal.Move;

public class SolitaireMove extends Move {
    private final UUID uuid = UUID.randomUUID();

    @Override
    public void execute() {}

    @Override
    public UUID getUUID() {
        return uuid;
    }
}
