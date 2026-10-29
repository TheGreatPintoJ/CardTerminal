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
                // Check if possible to move to stock
                LinkedList<Card> stockPile = solitaire.getStock().get(index);
                if (stockPile.isEmpty() && card.getRank() == Card.Rank.KING) {
                    res = stockPile.add(card);
                } else {
                    // Check if colors are opposites
                    if (stockPile.getLast().getSuit().isOppositeColor(card.getSuit())
                            // Check if ranks line up
                            && stockPile.getLast().getRank().toInt()
                                    == card.getRank().toInt() + 1) {
                        res = stockPile.add(card);
                    }
                }
                if (res) handleRemove();
                solitaire.drawGame();
                break;
        }
        return res;
    }

    private void handleRemove() {
        switch (fromLocation) {
            case STOCK:
                card.clear();
                LinkedList<Card> stockPile =
                        solitaire
                                .getStock()
                                .get(
                                        solitaire
                                                .getStock()
                                                .indexOf(
                                                        solitaire.getStock().stream()
                                                                .filter(l -> l.contains(card))
                                                                .toList()
                                                                .getFirst()));
                LinkedList<Card> oldStockPile =
                        solitaire.getStock().stream()
                                .filter(
                                        l ->
                                                l.contains(card)
                                                        && solitaire.getStock().indexOf(l) != index)
                                .toList()
                                .getFirst();
                oldStockPile.forEach(Card::clear);

                // Move rest of stack
                stockPile.addAll(
                        oldStockPile.subList(oldStockPile.indexOf(card) + 1, oldStockPile.size()));
                // Remove rest of stack from old pile
                oldStockPile.removeAll(
                        oldStockPile.subList(oldStockPile.indexOf(card) + 1, oldStockPile.size()));

                solitaire.getStock().stream()
                        .filter(l -> solitaire.getStock().indexOf(l) != index)
                        .forEach(l -> l.remove(card));
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
