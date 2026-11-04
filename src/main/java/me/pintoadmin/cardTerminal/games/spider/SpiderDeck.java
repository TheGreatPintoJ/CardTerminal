package me.pintoadmin.cardTerminal.games.spider;

import java.util.*;
import me.pintoadmin.cardTerminal.*;

public class SpiderDeck extends Deck {
    public SpiderDeck(long SEED) {
        super(SEED, List.of(Card.Suit.SPADE), 8);
    }

    public List<Card> getCards(Card.Rank rank, Card.Suit suit) {
        return allCards.stream()
                .filter(c -> c.getRank() == rank && c.getSuit() == suit)
                .toList();
    }
}
