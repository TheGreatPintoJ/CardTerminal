package me.pintoadmin.cardTerminal.games;

import java.util.*;
import me.pintoadmin.cardTerminal.*;

public class SolitaireMove {
    private final UUID uuid = UUID.randomUUID();
    private final Solitaire solitaire;
    private final Card card;
    private final Solitaire.Location fromLocation;
    private final Solitaire.Location location;
    private final int toIndex;
    private int fromIndex;
    private final Type type;
    private final List<Card> oldDiscard = new LinkedList<>();
    private final List<Card> shuffled = new LinkedList<>();
    private long shuffleIndex;

    enum Type {
        DRAW,
        SHUFFLE,
        EXHAUST,
        MOVE
    }

    public SolitaireMove(
            Solitaire solitaire,
            Card card,
            Solitaire.Location fromLocation,
            Solitaire.Location location,
            int toIndex,
            Type type) {
        this.solitaire = solitaire;
        this.card = card;
        this.fromLocation = fromLocation;
        this.location = location;
        this.toIndex = toIndex;
        this.type = type;
    }

    public boolean execute() {
        boolean res = false;
        if (type == Type.DRAW) {
            solitaire.drawCard(false);
            return true;
        } else if (type == Type.SHUFFLE) {
            solitaire.getDeck().clearDiscard();
            solitaire.getDeck().setAllCards(shuffled);
            solitaire.getDeck().setNextShuffleIndex(shuffleIndex + 1);
            solitaire.setNextShuffle(false);
            return true;
        } else if (type == Type.EXHAUST) {
            solitaire.getDeck().discardCard(card);
            solitaire.setDrawnCard(null);
            solitaire.setNextShuffle(true);
        } else {
            switch (location) {
                case FOUNDATION:
                    res = solitaire.getFoundation().addCard(card);
                    if (res) handleRemove();
                    solitaire.getFoundation().redraw();
                    break;
                case STOCK:
                    // Check if possible to move to stock
                    LinkedList<Card> stockPile = solitaire.getStock().get(toIndex);
                    if (stockPile.isEmpty() && card.getRank() == Card.Rank.KING) {
                        res = stockPile.add(card);
                    } else {
                        if (!stockPile.isEmpty()) {
                            // Check if colors are opposites
                            if (stockPile.getLast().getSuit().isOppositeColor(card.getSuit())
                                    // Check if ranks line up
                                    && stockPile.getLast().getRank().toInt()
                                            == card.getRank().toInt() + 1) {
                                res = stockPile.add(card);
                            }
                        }
                    }
                    if (res) {

                        handleRemove();
                    }
                    solitaire.drawGame();
                    break;
            }
        }
        return res;
    }

    private void handleRemove() {
        card.clear();
        solitaire.getFoundation().clear();
        switch (fromLocation) {
            case STOCK:
                fromIndex =
                        solitaire
                                .getStock()
                                .indexOf(
                                        solitaire.getStock().stream()
                                                .filter(
                                                        l ->
                                                                l.contains(card)
                                                                        && solitaire
                                                                                        .getStock()
                                                                                        .indexOf(l)
                                                                                != toIndex)
                                                .toList()
                                                .getFirst());
                LinkedList<Card> stockPile = solitaire.getStock().get(toIndex);
                LinkedList<Card> oldStockPile =
                        solitaire.getStock().stream()
                                .filter(
                                        l ->
                                                l.contains(card)
                                                        && solitaire.getStock().indexOf(l)
                                                                != toIndex)
                                .toList()
                                .getFirst();
                oldStockPile.forEach(Card::clear);

                if (location == Solitaire.Location.STOCK) {
                    // Move rest of stack
                    stockPile.addAll(
                            oldStockPile.subList(
                                    oldStockPile.indexOf(card) + 1, oldStockPile.size()));
                    // Remove rest of stack from old pile
                    oldStockPile.removeAll(
                            oldStockPile
                                    .subList(oldStockPile.indexOf(card) + 1, oldStockPile.size())
                                    .stream()
                                    .toList());
                }

                solitaire.getStock().stream()
                        .filter(l -> solitaire.getStock().indexOf(l) != toIndex)
                        .forEach(l -> l.remove(card));
                solitaire.drawGame();
                break;
            case DISCARD:
                fromIndex = 0;
                solitaire.getDeck().removeTopDiscard();
                solitaire.getDeck().redrawDiscard();
                break;
            case FOUNDATION:
                fromIndex = 0;
                solitaire.getFoundation().clear();
                solitaire.getFoundation().removeCard(card);
                solitaire.getFoundation().redraw();
                break;
            case DECK:
                fromIndex = 0;
                solitaire.nullDrawnCard();
                solitaire.getDeck().redrawDeck();
                break;
        }
    }

    public void undo() {
        if (type == Type.MOVE) {
            // Clear
            card.clear();
            solitaire.getFoundation().clear();
            // Move to fromLocation
            switch (fromLocation) {
                case STOCK:
                    if (!solitaire.getStock().get(fromIndex).isEmpty())
                        solitaire.getStock().get(fromIndex).getLast().flip(false);
                    solitaire.getStock().get(fromIndex).add(card);
                    break;
            }
            switch (location) {
                case STOCK:
                    solitaire.getStock().get(toIndex).remove(card);
                    break;
                case FOUNDATION:
                    solitaire.getFoundation().removeCard(card);
                    break;
            }
        } else if (type == Type.DRAW) {
            // Add to top of deck
            solitaire.getDeck().addTopDeck(card);
            solitaire.setDrawnCard(solitaire.getDeck().getTopDiscard());
            solitaire.getDeck().removeTopDiscard();
            solitaire.setNextShuffle(false);
        } else if (type == Type.SHUFFLE) {
            // Set deck to old shuffle
            oldDiscard.forEach(c -> solitaire.getDeck().discardCard(c));
            solitaire.getDeck().setNextShuffleIndex(shuffleIndex);
            solitaire.setNextShuffle(true);
        } else if (type == Type.EXHAUST) {
            if (solitaire.getDeck().getTopDiscard() != card) return;
            solitaire.getDeck().removeTopDiscard();
            solitaire.setDrawnCard(card);
            solitaire.setNextShuffle(false);
        }
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setOldDiscard(List<Card> oldDiscard) {
        this.oldDiscard.clear();
        this.oldDiscard.addAll(oldDiscard);
    }

    public void setShuffled(List<Card> shuffled) {
        this.shuffled.clear();
        this.shuffled.addAll(shuffled);
    }

    public void setShuffleIndex(long index) {
        shuffleIndex = index;
    }
}
