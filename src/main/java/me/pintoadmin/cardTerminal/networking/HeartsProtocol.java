package me.pintoadmin.cardTerminal.networking;

import java.util.List;
import me.pintoadmin.cardTerminal.Card;

public final class HeartsProtocol {
    public static final int VERSION = 1;

    private HeartsProtocol() {}

    public static class Message {
        public String type;
        public int version = VERSION;
        public String error;
        public int playerIndex;
        public List<CardData> hand;
        public List<CardData> playedCards;
        public int currentPlayer;
        public int startingPlayer;
        public boolean[] remotePlayers;
        public String gameState;
        public int[] scores;
        public boolean heartsBroken;
        public String activeSuit;
        public int trick;
        public int[] cardCounts;
        public Integer cardIndex;
        public int[] cardIndices;

        public Message() {}
    }

    public static class CardData {
        public String suit;
        public String rank;

        public CardData() {}

        public CardData(Card card) {
            suit = card.getSuit().name();
            rank = card.getRank().name();
        }

        public Card toCard() {
            return new Card(Card.Rank.valueOf(rank), Card.Suit.valueOf(suit));
        }
    }
}
