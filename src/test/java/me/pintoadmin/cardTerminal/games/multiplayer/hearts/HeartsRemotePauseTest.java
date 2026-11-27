package me.pintoadmin.cardTerminal.games.multiplayer.hearts;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import me.pintoadmin.cardTerminal.Card;
import me.pintoadmin.cardTerminal.networking.HeartsProtocol;
import org.junit.jupiter.api.Test;

/**
 * Joins a game as a real remote player (seat 1) over the socket protocol and plays a full deal.
 * Checks that what a remote client receives matches what {@code HeartsLocal}/{@code
 * HeartsRemote} rely on to show the engine's pauses:
 *
 * <ul>
 *   <li>trick-end pause: a snapshot with 4 played cards arrives, and plays sent during it are
 *       rejected
 *   <li>score pause: "showScores" arrives, no snapshot for the next deal arrives until about
 *       {@link HeartsTiming#DEAL_SCORES} later, and then the deal number changes (which releases
 *       the client's score hold)
 * </ul>
 */
class HeartsRemotePauseTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void remoteClientSeesBothPauses() throws Exception {
        double oldSpeed = HeartsTiming.speed;
        HeartsTiming.speed = 0.1; // 100 ms trick pause, 400 ms score pause
        HeartsEngine engine = new HeartsEngine(777L, 100);
        try (Socket socket = new Socket("localhost", engine.getServer().getPort())) {
            BufferedWriter out =
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    socket.getOutputStream(), StandardCharsets.UTF_8));
            BlockingQueue<HeartsProtocol.Message> inbox = new LinkedBlockingQueue<>();
            Thread reader =
                    new Thread(
                            () -> {
                                try (BufferedReader in =
                                        new BufferedReader(
                                                new InputStreamReader(
                                                        socket.getInputStream(),
                                                        StandardCharsets.UTF_8))) {
                                    String line;
                                    while ((line = in.readLine()) != null)
                                        inbox.add(
                                                MAPPER.readValue(
                                                        line, HeartsProtocol.Message.class));
                                } catch (IOException ignored) {
                                }
                            });
            reader.setDaemon(true);
            reader.start();

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (!engine.getRemotePlayers()[1] && System.nanoTime() < deadline)
                Thread.sleep(10);
            assertTrue(engine.getRemotePlayers()[1], "remote player never joined seat 1");

            engine.start();
            int firstDeal = engine.getDeal();

            boolean sawFullTrick = false;
            boolean rejectedDuringTrickPause = false;
            boolean probeSent = false;
            String lastTurnPlayed = null;
            long scoresAt = -1;
            long nextDealAt = -1;
            deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);

            while (nextDealAt < 0 && System.nanoTime() < deadline) {
                // Seat 0 is the "local" seat: play it directly
                synchronized (engine) {
                    if (!engine.isPaused()
                            && engine.getState() == HeartsEngine.GameState.TRICKS
                            && engine.getCurrentPlayerTurn() == 0
                            && !engine.getCards(0).isEmpty())
                        engine.playCard(0, engine.getPlayableCards(0).getFirst());
                }

                HeartsProtocol.Message m = inbox.poll(10, TimeUnit.MILLISECONDS);
                if (m == null) continue;

                if ("error".equals(m.type)) {
                    // Only our probe can cause an error; real plays are made on our turn
                    assertTrue(probeSent, "unexpected server error: " + m.error);
                    rejectedDuringTrickPause = true;
                } else if ("showScores".equals(m.type) && scoresAt < 0) {
                    scoresAt = System.nanoTime();
                } else if ("gameState".equals(m.type)) {
                    if (m.deal < firstDeal) continue; // pregame snapshots from joining
                    if (m.deal > firstDeal) {
                        assertTrue(scoresAt > 0, "next deal arrived before the score screen");
                        nextDealAt = System.nanoTime();
                        break;
                    }
                    boolean tricks = "TRICKS".equals(m.gameState);
                    if (tricks && m.playedCards.size() == 4) {
                        sawFullTrick = true;
                        // What HeartsLocal shows as the winner must match the engine's rule
                        int winner =
                                HeartsEngine.getTrickWinner(
                                        m.playedCards.stream()
                                                .map(HeartsProtocol.CardData::toCard)
                                                .toList(),
                                        m.startingPlayer);
                        assertTrue(winner >= 0 && winner < 4);
                        // Try to play during the pause: the server must refuse
                        if (!probeSent && !m.hand.isEmpty()) {
                            send(out, playCard(0));
                            probeSent = true;
                        }
                    } else if (tricks
                            && m.currentPlayer == 1
                            && m.playedCards.size() < 4
                            && !m.hand.isEmpty()) {
                        // Several snapshots arrive per move; only answer each turn once
                        String turnKey = m.deal + "/" + m.trick + "/" + m.playedCards.size();
                        if (turnKey.equals(lastTurnPlayed)) continue;
                        int index;
                        synchronized (engine) {
                            if (engine.isPaused() || engine.getCurrentPlayerTurn() != 1)
                                continue;
                            List<Card> hand = engine.getCards(1);
                            index = hand.indexOf(engine.getPlayableCards(1).getFirst());
                        }
                        send(out, playCard(index));
                        lastTurnPlayed = turnKey;
                    }
                }
            }

            assertTrue(sawFullTrick, "remote never saw a full trick during the pause");
            assertTrue(rejectedDuringTrickPause, "never tested a play during the trick pause");
            assertTrue(scoresAt > 0, "remote never got showScores");
            assertTrue(nextDealAt > 0, "next deal never started");
            long heldMs = TimeUnit.NANOSECONDS.toMillis(nextDealAt - scoresAt);
            long expected = HeartsTiming.ms(HeartsTiming.DEAL_SCORES);
            assertTrue(
                    heldMs >= expected - 50,
                    "score screen held " + heldMs + "ms, expected ~" + expected + "ms");
        } finally {
            engine.endGame();
            HeartsTiming.speed = oldSpeed;
        }
    }

    private static HeartsProtocol.Message playCard(int index) {
        HeartsProtocol.Message m = new HeartsProtocol.Message();
        m.type = "playCard";
        m.cardIndex = index;
        return m;
    }

    private static void send(BufferedWriter out, HeartsProtocol.Message m) throws IOException {
        out.write(MAPPER.writeValueAsString(m));
        out.newLine();
        out.flush();
    }
}
