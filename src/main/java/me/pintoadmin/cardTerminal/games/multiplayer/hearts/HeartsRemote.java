package me.pintoadmin.cardTerminal.games.multiplayer.hearts;

import java.util.*;
import java.util.stream.*;

public class HeartsRemote extends HeartsLocal {
    private final String host;
    private final int port;

    public HeartsRemote(String host, int port) {
        super();
        this.host = host;
        this.port = port;
    }

    public HeartsRemote(String host, int port, long SEED, int endingPoints) {
        super(SEED, endingPoints);
        this.host = host;
        this.port = port;
    }

    @Override
    public void updateLocals() {
        String message = "";
        message.replace("UPDATECACHE:", "");
        String trimmed = message.substring(0, message.length()-1);
        Map<String, String> values = Arrays.stream(trimmed.split(", "))
                .map(entry -> entry.split("=", 2))
                .collect(Collectors.toMap(entry -> entry[0].trim(), entry -> entry.length > 1 ? entry[1].trim() : ""));

        ourCards = engine.getCards(0);
        currentPlayerturn = engine.getCurrentPlayerTurn();
        startingPlayer = engine.getStartingPlayer();
        playedCards = engine.getPlayedCards();
        gameState = engine.getState();
        scores = engine.getScores();
        heartsBroken = engine.isHeartsBroken();
        activeSuit = engine.getActiveSuit();
        trick = engine.getTrick();

        for (int i = 0; i < 4; i++) numCards[i] = engine.getNumCards(i);
    }
}
