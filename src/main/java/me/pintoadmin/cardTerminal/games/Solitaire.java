package me.pintoadmin.cardTerminal.games;

import java.util.*;
import java.util.function.*;
import me.pintoadmin.cardTerminal.*;

public class Solitaire extends Game {
    private final long SEED = new Random().nextLong();
    private final Deck deck = new Deck(SEED);
    private final Foundation foundation = new Foundation();
    private final List<List<Card>> cards = new ArrayList<>(); // 2d, column:row:card
    private Card drawnCard;

    private final Supplier<Integer> WIDTH = () -> Main.g.getSize().getColumns();
    private final Supplier<Integer> HEIGHT = () -> Main.g.getSize().getRows();

    private final int stockWidth = 7; // Seven for default solitaire, hopefully configurable later

    private final Supplier<Integer> deckX = () -> 0;
    private final Supplier<Integer> deckY = () -> HEIGHT.get() - 14;

    public Solitaire() {
        System.out.println("Solitaire game seed: " + SEED);
        for (int column = 0; column < stockWidth; column++) {
            List<Card> thisColumn = new ArrayList<>();
            for (int row = 0; row <= column; row++) {
                Card card = deck.drawCard();
                if (card == null) continue;
                thisColumn.add(card);
            }
            cards.add(thisColumn);
        }
        drawnCard = deck.drawCard();

        drawGame();
    }

    @Override
    public void resize() {
        Main.screen.clear();
        drawGame();
    }

    @Override
    public void redraw() {
        Main.screen.clear();
        deck.redrawDeck();
        deck.redrawDiscard();

        for (List<Card> list : cards) {
            for (Card card : list) {
                card.redraw();
            }
        }

        for (List<Card> list : cards) {
            list.getLast().flip(true);
        }

        foundation.redraw();
        drawnCard.redraw();
    }

    private boolean nextDrawShuffles = false;

    @Override
    public void drawCard() {
        if (nextDrawShuffles) {
            deck.shuffle();
            deck.redrawDiscard();
            deck.redrawDeck();

            drawnCard = deck.drawCard();
            drawnCard.draw(deckX.get(), deckY.get(), false);

            nextDrawShuffles = false;
            return;
        }
        if (!drawnCard.isFaceup()) {
            drawnCard.draw(deckX.get(), deckY.get(), true);
            return;
        }
        deck.discardCard(drawnCard);
        deck.redrawDiscard();
        drawnCard = deck.drawCard();
        if (drawnCard == null) {
            // Deck has run out
            deck.redrawDeck();
            nextDrawShuffles = true;
            return;
        }
        drawnCard.draw(deckX.get(), deckY.get(), true);
    }

    private void drawGame() {
        deck.drawDeck(deckX.get(), deckY.get());
        deck.drawDiscard(deckX.get() + 9, deckY.get());

        int startingX = Main.g.getSize().getColumns() - (9 * stockWidth);
        int startingY = 0;

        for (int column = 0; column < stockWidth; column++) {
            int ci = 0;
            for (Card card : cards.get(column)) {
                card.draw(startingX + (column) * 9, startingY + ci * 3);
                ci++;
            }
        }

        for (List<Card> list : cards) {
            list.getLast().flip(true);
        }

        int foundationX = 0;
        int foundationY = HEIGHT.get() - 7;

        foundation.draw(foundationX, foundationY);

        if (drawnCard != null) drawnCard.draw(deckX.get(), deckY.get());
        else deck.redrawDeck();
    }
}
