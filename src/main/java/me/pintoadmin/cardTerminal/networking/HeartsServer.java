package me.pintoadmin.cardTerminal.networking;

import java.io.*;
import java.net.*;
import java.util.*;
import me.pintoadmin.cardTerminal.*;
import me.pintoadmin.cardTerminal.games.multiplayer.hearts.*;

public class HeartsServer {
    private final int PORT;
    private final HeartsEngine engine;

    private HeartsLocal local;
    private final LinkedHashMap<Socket, PrintWriter> clients = new LinkedHashMap<>();

    private Thread socketThread;
    private ServerSocket socket;

    private int playerCounter = 1;

    /*
    MESSAGE FORMAT: "<command>/[args]"
     */

    public HeartsServer(int PORT, HeartsEngine engine) {
        this.PORT = PORT;
        this.engine = engine;

        try {
            socket = new ServerSocket(PORT);

            socketThread =
                    new Thread(
                            () -> {
                                try {
                                    socket = new ServerSocket(PORT);
                                    while (!Thread.currentThread().isInterrupted()) {
                                        Socket clientSocket = socket.accept();
                                        int player = playerCounter++;
                                        new Thread(new ClientHandler(clientSocket, player)).start();
                                    }
                                    socket.close();
                                } catch (BindException ignored) {
                                    throw new RuntimeException("There's another instance running!");
                                } catch (SocketException ignored) {
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
            socketThread.start();
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
    }

    public void deinit() {
        socketThread.interrupt();
    }

    public void updateClients() {
        local.updateLocals();
        clients.forEach(
                (s, w) -> {
                    w.println("UPDATECACHE");
                });
    }

    public void resizeClients() {
        local.resize();
        clients.forEach(
                (s, w) -> {
                    w.println("RESIZESCREEN");
                });
    }

    public void showScoresClients() {
        local.showScores();
        clients.forEach(
                (s, w) -> {
                    w.println("SHOWSCORES");
                });
    }

    public void addLocalClient(HeartsLocal local) {
        this.local = local;
    }

    public int getPort() {
        return PORT;
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final int playerId;
        private PrintWriter out;

        public ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
        }

        @Override
        public void run() {
            try (BufferedReader in =
                    new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);
                clients.put(socket, out);

                // receive messages
                String inputLine;
                // Continuously listen for player moves/actions
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Received from Player " + playerId + ": " + inputLine);
                    String[] messageSplit = inputLine.split("/");
                    int player = clients.keySet().stream().toList().indexOf(socket);
                    switch (messageSplit[0].toLowerCase()) {
                        case "pass":
                            {
                                String[] strs = messageSplit[1].split(":");
                                Integer[] ints =
                                        Arrays.stream(strs)
                                                .map(Integer::parseInt)
                                                .toArray(Integer[]::new);
                                Card[] cards =
                                        new Card[] {
                                            engine.getCards(player).get(ints[0]),
                                            engine.getCards(player).get(ints[1]),
                                            engine.getCards(player).get(ints[2])
                                        };
                                engine.passCards(player, cards);
                            }
                            break;
                        case "play":
                            {
                                int card = Integer.parseInt(messageSplit[2]);
                                engine.playCard(player, engine.getCards(player).get(card));
                            }
                            break;
                        case "getinfo":
                            {
                                Map<String, Object> info = new HashMap<>();
                                info.put("ourCards", engine.getCards(0));
                                info.put("currentPlayerturn", engine.getCurrentPlayerTurn());
                                info.put("startingPlayer", engine.getStartingPlayer());
                                info.put("playedCards", engine.getPlayedCards());
                                info.put("gameState", engine.getState());
                                info.put("scores", engine.getScores());
                                info.put("heartsBroken", engine.isHeartsBroken());
                                info.put("activeSuit", engine.getActiveSuit());
                                info.put("trick", engine.getTrick());
                                info.put("numcards0", engine.getNumCards(0));
                                info.put("numcards1", engine.getNumCards(1));
                                info.put("numcards2", engine.getNumCards(2));
                                info.put("numcards3", engine.getNumCards(3));

                                out.println("UPDATEDINFO:" + info);
                                break;
                            }
                    }
                }
            } catch (IOException e) {
                System.out.println("Player " + playerId + " disconnected.");
            } finally {
                if (out != null) {
                    clients.remove(socket);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
