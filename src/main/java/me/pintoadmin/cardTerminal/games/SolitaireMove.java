package me.pintoadmin.cardTerminal.games;

import java.util.UUID;
import me.pintoadmin.cardTerminal.*;

public class SolitaireMove extends Move {
    private final UUID uuid = UUID.randomUUID();
    private final Solitaire solitaire;
    private final Card card;
    private final Solitaire.Location location;
    private final int index;

    public SolitaireMove(Solitaire solitaire, Card card, Solitaire.Location location, int index) {
        this.solitaire = solitaire;
        this.card = card;
        this.location = location;
        this.index = index;
    }

    @Override
    public boolean execute() {
        boolean res = false;
        switch (location) {
            case FOUNDATION:
                res = solitaire.getFoundation().addCard(card);
                solitaire.getFoundation().redraw();
                break;
            case DISCARD:
                solitaire.getDeck().discardCard(card);
                solitaire.getDeck().redrawDiscard();
                res = true;
                break;
            case STOCK:
                res = solitaire.getStock().get(index % solitaire.getStockWidth()).add(card);
                solitaire.drawGame();
                break;
        }
        return res;
    }

    @Override
    public void undo() {}

    @Override
    public UUID getUUID() {
        return uuid;
    }
}
