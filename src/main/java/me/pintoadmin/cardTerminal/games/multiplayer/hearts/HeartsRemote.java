package me.pintoadmin.cardTerminal.games.multiplayer.hearts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.lanterna.input.KeyStroke;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.pintoadmin.cardTerminal.Card;
import me.pintoadmin.cardTerminal.Game;
import me.pintoadmin.cardTerminal.networking.HeartsProtocol;

public class HeartsRemote extends HeartsLocal {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String host;
    private final int port;
    private final Socket socket;
    private final BufferedWriter out;

    public HeartsRemote(String host, int port) {
        super(true);
        this.host = host;
        this.port = port;
        try {
            socket = new Socket(host, port);
            out =
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    socket.getOutputStream(), StandardCharsets.UTF_8));
            Thread reader = new Thread(() -> readMessages(socket), "Hearts server connection");
            reader.setDaemon(true);
            reader.start();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not connect to Hearts server " + host + ":" + port, e);
        }
    }

    public HeartsRemote(String host, int port, long seed, int endingPoints) {
        this(host, port);
    }

    @Override
    protected boolean isRemoteGame() {
        return true;
    }

    @Override
    protected void submitPlayCard(int player, Card card) {
        HeartsProtocol.Message message = new HeartsProtocol.Message();
        message.type = "playCard";
        message.cardIndex = ourCards.indexOf(card);
        send(message);
    }

    @Override
    protected void submitPassCards(int player, Card[] cards) {
        HeartsProtocol.Message message = new HeartsProtocol.Message();
        message.type = "passCards";
        message.cardIndices = new int[cards.length];
        for (int i = 0; i < cards.length; i++) {
            message.cardIndices[i] = ourCards.indexOf(cards[i]);
        }
        send(message);
        passingCards = new Card[3];
    }

    @Override
    public Action handleInput(KeyStroke keyStroke) {
        Action result = super.handleInput(keyStroke);
        if (result == Action.BACK_TO_MENU) closeConnection();
        return result;
    }

    @Override
    public Game newGame() {
        return new HeartsRemote(host, port);
    }

    @Override
    public Game restart() {
        return new HeartsRemote(host, port);
    }

    private void send(HeartsProtocol.Message message) {
        try {
            synchronized (out) {
                out.write(MAPPER.writeValueAsString(message));
                out.newLine();
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Could not send action to Hearts server: " + e.getMessage());
        }
    }

    private void readMessages(Socket connection) {
        try (BufferedReader in =
                new BufferedReader(
                        new InputStreamReader(
                                connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(line);
            }
        } catch (IOException e) {
            if (!connection.isClosed()) {
                System.err.println("Lost connection to Hearts server: " + e.getMessage());
            }
        }
    }

    private void handleMessage(String line) {
        HeartsProtocol.Message message;
        try {
            message = MAPPER.readValue(line, HeartsProtocol.Message.class);
        } catch (JsonProcessingException e) {
            System.err.println("Received invalid JSON from Hearts server: " + e.getMessage());
            return;
        }
        if (message.version != HeartsProtocol.VERSION) {
            System.err.println("Unsupported Hearts protocol version: " + message.version);
            return;
        }
        if ("gameState".equals(message.type)) {
            applySnapshot(message);
        } else if ("resizeScreen".equals(message.type)) {
            resize();
        } else if ("showScores".equals(message.type)) {
            showScores();
        } else if ("error".equals(message.type)) {
            System.err.println("Hearts server rejected request: " + message.error);
        } else {
            System.err.println("Unknown Hearts server message type: " + message.type);
        }
    }

    private void applySnapshot(HeartsProtocol.Message message) {
        try {
            if (message.playerIndex < 0
                    || message.playerIndex > 3
                    || message.hand == null
                    || message.playedCards == null
                    || message.scores == null
                    || message.scores.length != 4
                    || message.cardCounts == null
                    || message.cardCounts.length != 4) {
                throw new IllegalArgumentException("Invalid game-state fields");
            }

            localPlayer = message.playerIndex;
            ourCards = decodeCards(message.hand);
            playedCards = decodeCards(message.playedCards);
            currentPlayerturn = message.currentPlayer;
            startingPlayer = message.startingPlayer;
            remotePlayers = message.remotePlayers;
            gameState = HeartsEngine.GameState.valueOf(message.gameState);
            scores = new HashMap<>();
            for (int i = 0; i < 4; i++) {
                scores.put(i, message.scores[i]);
            }
            heartsBroken = message.heartsBroken;
            activeSuit = message.activeSuit == null ? null : Card.Suit.valueOf(message.activeSuit);
            trick = message.trick;
            for (int i = 0; i < 4; i++) {
                wonTricks.put(i, decodeCards(message.wonTricks.get(i)));
            }
            numCards = message.cardCounts.clone();

            for (int i = 0; i < passingCards.length; i++) {
                Card old = passingCards[i];
                if (old == null) continue;
                passingCards[i] = ourCards.stream()
                        .filter(c -> c.getRank() == old.getRank() && c.getSuit() == old.getSuit())
                        .findFirst()
                        .orElse(null);
            }

            handleUpdate();
            resize();
        } catch (IllegalArgumentException | NullPointerException e) {
            System.err.println("Received invalid game state from Hearts server: " + e.getMessage());
        }
    }

    private List<Card> decodeCards(List<HeartsProtocol.CardData> cards) {
        List<Card> decoded = new ArrayList<>();
        for (HeartsProtocol.CardData card : cards) {
            if (card == null || card.rank == null || card.suit == null) {
                throw new IllegalArgumentException("Card data must include rank and suit");
            }
            decoded.add(card.toCard());
        }
        return decoded;
    }

    private void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Could not close Hearts server connection: " + e.getMessage());
        }
    }
}
