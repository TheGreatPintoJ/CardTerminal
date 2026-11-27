package me.pintoadmin.cardTerminal.games.multiplayer.hearts;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import me.pintoadmin.cardTerminal.Card;
import me.pintoadmin.cardTerminal.Card.Rank;
import me.pintoadmin.cardTerminal.Card.Suit;
import org.junit.jupiter.api.Test;

class HeartsTrickWinnerTest {
    private static Card c(Rank r, Suit s) {
        return new Card(r, s);
    }

    @Test
    void highestOfLedSuitWins() {
        List<Card> played =
                List.of(
                        c(Rank.FIVE, Suit.CLUB),
                        c(Rank.KING, Suit.CLUB),
                        c(Rank.ACE, Suit.HEART), // off-suit, can't win
                        c(Rank.TEN, Suit.CLUB));
        // Starting player 2: seats play 2, 3, 0, 1 -> KING by seat 3
        assertEquals(3, HeartsEngine.getTrickWinner(played, 2));
    }

    @Test
    void aceIsHigh() {
        List<Card> played =
                List.of(
                        c(Rank.KING, Suit.SPADE),
                        c(Rank.ACE, Suit.SPADE),
                        c(Rank.TWO, Suit.SPADE),
                        c(Rank.QUEEN, Suit.SPADE));
        assertEquals(1, HeartsEngine.getTrickWinner(played, 0));
    }

    @Test
    void leaderWinsWhenNobodyFollows() {
        List<Card> played =
                List.of(
                        c(Rank.TWO, Suit.DIAMOND),
                        c(Rank.ACE, Suit.HEART),
                        c(Rank.ACE, Suit.CLUB),
                        c(Rank.ACE, Suit.SPADE));
        assertEquals(3, HeartsEngine.getTrickWinner(played, 3));
    }

    @Test
    void emptyTrickHasNoWinner() {
        assertEquals(-1, HeartsEngine.getTrickWinner(List.of(), 0));
    }
}
