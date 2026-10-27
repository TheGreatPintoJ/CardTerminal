package me.pintoadmin.cardTerminal.games;

import java.util.*;
import java.util.function.*;
import me.pintoadmin.cardTerminal.*;

public class Solitaire extends Game {
    private final long SEED = new Random().nextLong();
    private final Deck deck = new Deck(SEED);
    private final Foundation foundation = new Foundation();
    private final List<List<Card>> cards = new ArrayList<>(); // 2d, column:row:card

    private final Supplier<Integer> WIDTH = () -> Main.g.getSize().getColumns();
    private final Supplier<Integer> HEIGHT = () -> Main.g.getSize().getRows();

    public Solitaire() {
        System.out.println("Solitaire game seed: " + SEED);
        for (int column = 0; column < 7; column++) {
            List<Card> thisColumn = new ArrayList<>();
            for (int row = 0; row <= column; row++) {
                Card card = deck.drawCard();
                if (card == null) continue;
                thisColumn.add(card);
            }
            cards.add(thisColumn);
            System.out.println("Column " + column + " is " + thisColumn);
        }
        redraw();
    }

    @Override
    public void redraw() {
        Main.screen.clear();
        drawGame();
    }

    private void drawGame() {
        deck.drawDeck(0, HEIGHT.get() - 14);
        deck.drawDiscard(9, HEIGHT.get() - 14);

        int startingX = Main.g.getSize().getColumns() - (9 * 7);
        int startingY = 0;

        for (int column = 0; column < 7; column++) {
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
        deck.flipTopCard();
    }
}
