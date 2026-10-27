package me.pintoadmin.cardTerminal.games;

import java.util.*;
import me.pintoadmin.cardTerminal.*;

public class Solitaire extends Game {
    private final Deck deck = new Deck();
    private final Foundation foundation = new Foundation();
    private final List<List<Card>> cards = new ArrayList<>(); // 2d, column:row:card

    public Solitaire() {
        for (int row = 0; row < 7; row++) {
            for (int column = 0; column + row < 7; column++) {
                Card card = deck.drawCard();
                if (card == null) continue;
                cards.add(card);
            }
        }
        drawGame();
    }

    @Override
    public void redraw() {
        Main.screen.clear();
        drawGame();
    }

    private void drawGame() {
        deck.drawDeck(0, 0);

        int startingX = Main.g.getSize().getColumns() - (9 * 7);
        int startingY = 0;

        int i = 0;
        for (int row = 0; row < 7; row++) {
            for (int column = 0; column + row < 7; column++) {
                Card card = cards.get(i);
                card.draw(startingX + (row + column) * 9, startingY + row * 3, column == 0);
                i++;
            }
        }

        int foundationX = 0;
        int foundationY = Main.g.getSize().getRows() - 7;

        foundation.draw(foundationX, foundationY);
    }
}
