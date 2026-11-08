package me.pintoadmin.cardTerminal.games.spider;

import java.util.*;
import me.pintoadmin.cardTerminal.*;

public class SpiderDeck extends Deck {
    public SpiderDeck(long SEED) {
        super(SEED, List.of(Card.Suit.SPADE), 8);
    }

    SpiderDeck(long SEED, Spider.GameType type) {
        super(
                SEED,
                type == Spider.GameType.SUIT1
                        ? List.of(Card.Suit.SPADE)
                        : type == Spider.GameType.SUIT2
                                ? List.of(Card.Suit.SPADE, Card.Suit.HEART)
                                : type == Spider.GameType.SUIT3
                                        ? List.of(Card.Suit.SPADE, Card.Suit.HEART, Card.Suit.CLUB)
                                        : List.of(Card.Suit.DIAMOND),
                type == Spider.GameType.SUIT1 ? 8 : type == Spider.GameType.SUIT2 ? 4 : 2);
    }

    public List<Card> getCards(Card.Rank rank, Card.Suit suit) {
        return allCards.stream().filter(c -> c.getRank() == rank && c.getSuit() == suit).toList();
    }
}
