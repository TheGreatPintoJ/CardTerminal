package me.pintoadmin.cardTerminal.games;

import java.util.*;
import me.pintoadmin.cardTerminal.*;

public class SolitaireMove {
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

    public boolean execute() {
        boolean res = false;
        switch (location) {
            case FOUNDATION:
                res = solitaire.getFoundation().addCard(card);
                if (res) handleRemove();
                solitaire.getFoundation().redraw();
                break;
            case DISCARD:
                solitaire.getDeck().discardCard(card);
                handleRemove();
                solitaire.getDeck().redrawDiscard();
                res = true;
                break;
            case STOCK:
                res = solitaire.getStock().get(index).add(card);
                handleRemove();
                solitaire.drawGame();
                break;
        }
        return res;
    }

    private void handleRemove() {
        switch (fromLocation) {
            case STOCK:
                card.clear();
                solitaire.getStock().forEach(l -> l.remove(card));
                solitaire.drawGame();
                break;
            case DISCARD:
                card.clear();
                solitaire.getDeck().removeTopDiscard();
                solitaire.getDeck().redrawDiscard();
                break;
            case FOUNDATION:
                card.clear();
                solitaire.getFoundation().removeCard(card);
                solitaire.getFoundation().redraw();
                break;
            case DECK:
                card.clear();
                solitaire.nullDrawnCard();
                solitaire.getDeck().redrawDeck();
                break;
        }
    }

    public void undo() {}

    public UUID getUUID() {
        return uuid;
    }
}
