package me.pintoadmin.cardTerminal.networking;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import me.pintoadmin.cardTerminal.Card;
import me.pintoadmin.cardTerminal.games.multiplayer.hearts.HeartsEngine;
import me.pintoadmin.cardTerminal.games.multiplayer.hearts.HeartsLocal;

public class HeartsServer {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final int port;
    private final HeartsEngine engine;
    private final ServerSocket serverSocket;
    private final ConcurrentHashMap<ClientHandler, Integer> clients = new ConcurrentHashMap<>();

    private HeartsLocal local;
    private Thread socketThread;

    public HeartsServer(int port, HeartsEngine engine) {
        this.port = port;
        this.engine = engine;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new IllegalStateException("Could not start Hearts server on port " + port, e);
        }

        socketThread =
                new Thread(
                        () -> {
                            while (!serverSocket.isClosed()) {
                                try {
                                    Socket clientSocket = serverSocket.accept();
                                    int player = allocatePlayer();
                                    if (player < 0) {
                                        clientSocket.close();
                                        continue;
                                    }
                                    ClientHandler handler = new ClientHandler(clientSocket, player);
                                    clients.put(handler, player);
                                    new Thread(handler, "Hearts client " + player).start();
                                    updateClients();
                                    resizeClients();
                                } catch (SocketException e) {
                                    if (!serverSocket.isClosed()) {
                                        System.err.println(
                                                "Hearts server socket error: " + e.getMessage());
                                    }
                                    break;
                                } catch (IOException e) {
                                    System.err.println(
                                            "Could not accept Hearts client: " + e.getMessage());
                                }
                            }
                        },
                        "Hearts server");
        socketThread.setDaemon(true);
        socketThread.start();
    }

    public void deinit() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Could not close Hearts server: " + e.getMessage());
        }
        clients.keySet().forEach(ClientHandler::close);
        socketThread.interrupt();
    }

    public void updateClients() {
        if (local != null) local.updateLocals();
        clients.keySet().forEach(client -> sendSnapshot(client, client.playerId));
    }

    public void resizeClients() {
        if (local != null) local.resize();
        broadcast(event("resizeScreen"));
    }

    public void showScoresClients() {
        if (local != null) local.showScores();
        broadcast(event("showScores"));
    }

    public void addLocalClient(HeartsLocal local) {
        this.local = local;
    }

    public int getPort() {
        return port;
    }

    public boolean isRemotePlayerConnected(int player) {
        return clients.containsValue(player) || player == 0;
    }

    private synchronized int allocatePlayer() {
        for (int player = 1; player < 4; player++) {
            if (!clients.containsValue(player)) return player;
        }
        return -1;
    }

    private HeartsProtocol.Message event(String type) {
        HeartsProtocol.Message message = new HeartsProtocol.Message();
        message.type = type;
        return message;
    }

    private void broadcast(HeartsProtocol.Message message) {
        clients.keySet().forEach(client -> send(client, message));
    }

    private void sendSnapshot(ClientHandler client, int player) {
        HeartsProtocol.Message message = new HeartsProtocol.Message();
        message.type = "gameState";
        synchronized (engine) {
            message.playerIndex = player;
            message.hand =
                    engine.getCards(player).stream().map(HeartsProtocol.CardData::new).toList();
            message.playedCards =
                    engine.getPlayedCards().stream().map(HeartsProtocol.CardData::new).toList();
            message.currentPlayer = engine.getCurrentPlayerTurn();
            message.startingPlayer = engine.getStartingPlayer();
            message.remotePlayers = engine.getRemotePlayers();
            message.gameState = engine.getState().name();
            message.scores = new int[4];
            message.cardCounts = new int[4];
            for (int i = 0; i < 4; i++) {
                message.scores[i] = engine.getScores().get(i);
                message.cardCounts[i] = engine.getNumCards(i);
            }
            message.heartsBroken = engine.isHeartsBroken();
            message.activeSuit =
                    engine.getActiveSuit() == null ? null : engine.getActiveSuit().name();
            message.trick = engine.getTrick();
            for (int i = 0; i < 4; i++) {
                message.wonTricks.put(
                        i,
                        engine.getWonTricks().get(i).stream()
                                .map(HeartsProtocol.CardData::new)
                                .toList());
            }
        }
        send(client, message);
    }

    private void send(ClientHandler client, HeartsProtocol.Message message) {
        try {
            client.send(message);
        } catch (IOException e) {
            System.err.println(
                    "Could not send game data to player "
                            + client.playerId
                            + ": "
                            + e.getMessage());
            client.close();
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final int playerId;
        private volatile BufferedWriter out;

        private ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
        }

        @Override
        public void run() {
            try (BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream(), StandardCharsets.UTF_8))) {
                out =
                        new BufferedWriter(
                                new OutputStreamWriter(
                                        socket.getOutputStream(), StandardCharsets.UTF_8));
                sendSnapshot(this, playerId);

                String line;
                while ((line = in.readLine()) != null) {
                    handleMessage(line);
                }
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.out.println("Player " + playerId + " disconnected: " + e.getMessage());
                }
            } finally {
                clients.remove(this);
                close();
            }
        }

        private void handleMessage(String line) {
            HeartsProtocol.Message message;
            try {
                message = MAPPER.readValue(line, HeartsProtocol.Message.class);
            } catch (IOException e) {
                sendError("Invalid JSON message");
                return;
            }
            if (message.version != HeartsProtocol.VERSION) {
                sendError("Unsupported protocol version: " + message.version);
                return;
            }
            if (message.type == null) {
                sendError("Message type is required");
                return;
            }

            switch (message.type) {
                case "playCard" -> playCard(message);
                case "passCards" -> passCards(message);
                default -> sendError("Unknown message type: " + message.type);
            }
        }

        private void playCard(HeartsProtocol.Message message) {
            List<Card> hand = engine.getCards(playerId);
            if (message.cardIndex == null
                    || message.cardIndex < 0
                    || message.cardIndex >= hand.size()) {
                sendError("Card index is out of range");
                return;
            }
            if (!engine.playCard(playerId, hand.get(message.cardIndex))) {
                sendError("Card play was not accepted");
            }
        }

        private void passCards(HeartsProtocol.Message message) {
            if (message.cardIndices == null || message.cardIndices.length != 3) {
                sendError("Passing requires exactly three card indices");
                return;
            }
            List<Card> hand = engine.getCards(playerId);
            List<Card> cards = new ArrayList<>();
            for (int index : message.cardIndices) {
                if (index < 0 || index >= hand.size()) {
                    sendError("Card index is out of range");
                    return;
                }
                cards.add(hand.get(index));
            }
            if (cards.stream().distinct().count() != 3) {
                sendError("Passed card indices must be distinct");
                return;
            }
            if (!engine.passCards(playerId, cards.toArray(Card[]::new))) {
                sendError("Card pass was not accepted");
            }
        }

        private void sendError(String error) {
            HeartsProtocol.Message message = event("error");
            message.error = error;
            HeartsServer.this.send(this, message);
        }

        private synchronized void send(HeartsProtocol.Message message) throws IOException {
            if (out == null) return;
            out.write(MAPPER.writeValueAsString(message));
            out.newLine();
            out.flush();
        }

        private void close() {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println(
                        "Could not close player " + playerId + " connection: " + e.getMessage());
            }
        }
    }
}
