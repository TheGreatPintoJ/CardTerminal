package me.pintoadmin.cardTerminal;

import java.util.*;

public class Deck {
    private List<Card> cardsInDeck = new ArrayList<>();

    public Deck() {
        for (Main.CardSuit suit : Main.CardSuit.values()) {
            for (Main.CardRank rank : Main.CardRank.values()) {
                cardsInDeck.add(new Card(rank, suit));
            }
        }
    }

    public Card drawCard() {
        Random random = new Random();
        Card randomCard = cardsInDeck.get(random.nextInt(cardsInDeck.size()));
        cardsInDeck.remove(randomCard);
        return randomCard;
    }
}
