package me.pintoadmin.cardTerminal.games.multiplayer.hearts;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import me.pintoadmin.cardTerminal.Card;
import org.junit.jupiter.api.Test;

/**
 * Plays as seat 0 (the local seat) against the built-in bots and checks that the end-of-trick
 * pause no longer blocks whoever plays the fourth card, and that plays are rejected during it.
 */
class HeartsEnginePauseTest {

    @Test
    void trickPauseDoesNotBlockAndRejectsPlays() throws Exception {
        HeartsEngine engine = new HeartsEngine(12345L, 100);
        try {
            engine.start();
            assertEquals(HeartsEngine.GameState.TRICKS, engine.getState());

            boolean sawPause = false;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
            // Play until we've seen at least two tricks complete
            while (engine.getTrick() < 2 && System.nanoTime() < deadline) {
                if (engine.isPaused()) {
                    // Holding the engine lock means the pause can't end mid-check
                    synchronized (engine) {
                        if (engine.isPaused()) {
                            sawPause = true;
                            // Whoever's "turn" it appears to be, nobody may play during it
                            int turn = engine.getCurrentPlayerTurn();
                            List<Card> hand = engine.getCards(turn);
                            if (!hand.isEmpty())
                                assertFalse(engine.playCard(turn, hand.getFirst()));
                        }
                    }
                } else if (engine.getCurrentPlayerTurn() == 0) {
                    Card card;
                    synchronized (engine) {
                        if (engine.isPaused() || engine.getCurrentPlayerTurn() != 0) continue;
                        card = engine.getPlayableCards(0).getFirst();
                    }
                    long start = System.nanoTime();
                    assertTrue(engine.playCard(0, card));
                    long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                    // Previously this blocked for TRICK_END (1000 ms) when it was the 4th card
                    assertTrue(ms < 250, "playCard blocked for " + ms + "ms");
                }
                Thread.sleep(10);
            }
            assertTrue(engine.getTrick() >= 2, "game did not progress");
            assertTrue(sawPause, "never observed the end-of-trick pause");
        } finally {
            engine.endGame();
        }
    }
}
