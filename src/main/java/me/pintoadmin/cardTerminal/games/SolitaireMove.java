package me.pintoadmin.cardTerminal.games;

import java.util.*;
import me.pintoadmin.cardTerminal.*;

public class SolitaireMove extends Move {
    private final UUID uuid = UUID.randomUUID();
    private final Solitaire solitaire;
    private final Card card;
    private final Solitaire.Location fromLocation;
    private final Solitaire.Location location;
    private final int index;

    public SolitaireMove(
            Solitaire solitaire,
            Card card,
            Solitaire.Location fromLocation,
            Solitaire.Location location,
            int index) {
        this.solitaire = solitaire;
        this.card = card;
        this.fromLocation = fromLocation;
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
        if (!res) return false;
        switch (fromLocation) {
            case STOCK:
                card.clear();
                solitaire.getStock().forEach(l -> l.remove(card));
                break;
            case DISCARD:
                card.clear();
                solitaire.getDeck().removeTopDiscard();
                break;
            case FOUNDATION:
                card.clear();
                solitaire.getFoundation().removeCard(card);
                break;
            case DECK:
                card.clear();
                solitaire.nullDrawnCard();
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
