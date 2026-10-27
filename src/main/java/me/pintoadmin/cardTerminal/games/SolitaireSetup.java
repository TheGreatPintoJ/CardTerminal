package me.pintoadmin.cardTerminal.games;

import java.util.*;
import me.pintoadmin.cardTerminal.*;

public class SolitaireSetup {
    private final Deck deck = new Deck();
    private final List<Card> cards = new ArrayList<>();

    public SolitaireSetup() {
        drawGame();
    }

    private void drawGame() {
        int startingX = 15;
        int startingY = 2;

        for (int i = 0; i < 7; i++) {
            for (int j = 0; j + i < 7; j++) {
                Card card = deck.drawCard();
                cards.add(card);

                card.draw(startingX + (i + j) * 9, startingY + i * 2, false);
            }
        }
    }
}
