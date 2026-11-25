package me.pintoadmin.cardTerminal.games.multiplayer.hearts;

import static me.pintoadmin.cardTerminal.Main.g;
import static me.pintoadmin.cardTerminal.Main.screen;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;
import me.pintoadmin.cardTerminal.*;
import me.pintoadmin.cardTerminal.games.solitaire.*;
import me.pintoadmin.cardTerminal.screens.*;

public class HeartsLocal extends Game {
    private final long SEED;
    private final HeartsEngine engine;
    private final int endingPoints;

    private final long startTime = System.nanoTime();
    private final Supplier<Long> time =
            () -> TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

    private final Supplier<Integer> WIDTH = () -> g.getSize().getColumns();
    private final Supplier<Integer> HEIGHT = () -> g.getSize().getRows();

    protected Card[] passingCards = new Card[3];

    private int selectedCard = 0;

    protected List<Card> ourCards;
    protected int currentPlayerturn;
    protected int startingPlayer;
    protected boolean[] remotePlayers = new boolean[4];
    protected List<Card> playedCards;
    protected HeartsEngine.GameState gameState;
    protected Map<Integer, Integer> scores;
    protected boolean heartsBroken;
    protected Card.Suit activeSuit;
    protected int[] numCards = new int[4];
    protected int trick;
    protected Map<Integer, List<Card>> wonTricks = new HashMap<>();
    protected int localPlayer = 0;

    private boolean showingScores = false;
    private int escapeCount = 0;
    private boolean passedCards = false;

    public void updateLocals() {
        ourCards = engine.getCards(0);
        currentPlayerturn = engine.getCurrentPlayerTurn();
        startingPlayer = engine.getStartingPlayer();
        remotePlayers = engine.getRemotePlayers();
        playedCards = engine.getPlayedCards();
        gameState = engine.getState();
        scores = engine.getScores();
        heartsBroken = engine.isHeartsBroken();
        activeSuit = engine.getActiveSuit();
        trick = engine.getTrick();
        wonTricks = engine.getWonTricks();

        for (int i = 0; i < 4; i++) numCards[i] = engine.getNumCards(i);

        handleUpdate();
    }

    protected void handleUpdate() {
        if (gameState == HeartsEngine.GameState.PASSING) {
            passingCards = new Card[3];
        }
        if (gameState == HeartsEngine.GameState.TRICKS) {
            passedCards = false;
        }
    }

    public HeartsLocal() {
        this(new Random().nextLong(), 100);
    }

    public HeartsLocal(long SEED, int endingPoints) {
        this(SEED, endingPoints, new HeartsEngine(SEED, endingPoints));
    }

    public HeartsLocal(long SEED, int endingPoints, HeartsEngine engine) {
        this.SEED = SEED;
        this.endingPoints = endingPoints;

        this.engine = engine;
        engine.getServer().addLocalClient(this);
        updateLocals();

        resize();
    }

    protected HeartsLocal(boolean remote) {
        SEED = 0;
        endingPoints = 100;
        engine = null;
        ourCards = new ArrayList<>();
        playedCards = new ArrayList<>();
        scores = new HashMap<>(Map.of(0, 0, 1, 0, 2, 0, 3, 0));
        gameState = HeartsEngine.GameState.PREGAME;
    }

    protected void submitPlayCard(int player, Card card) {
        engine.playCard(player, card);
    }

    protected void submitPassCards(int player, Card[] cards) {
        if (engine.passCards(player, cards)) {
            passedCards = true;
        }
    }

    protected boolean isRemoteGame() {
        return false;
    }

    private synchronized void drawGame() throws IOException {
        if (showingScores) {
            showScores();
            return;
        }
        screen.clear();

        int W = WIDTH.get(), H = HEIGHT.get();

        if (gameState == HeartsEngine.GameState.PREGAME) {
            // draw second player's cards
            for (int i = 0; i < 13; i++) {
                int startingY = (H / 2) - (26) / 2 - (Card.height / 2);
                Card.drawCardBack(4, startingY + (i * 2), TextColor.ANSI.WHITE);
            }
            // draw third player's cards
            for (int i = 0; i < 13; i++) {
                int startingX = (W / 2) - (39) / 2 - (Card.width / 2);
                Card.drawCardBack(startingX + (i * 3), 2, TextColor.ANSI.WHITE);
            }
            // draw fourth player's cards
            for (int i = 0; i < 13; i++) {
                int startingY = (H / 2) - (26) / 2 - (Card.height / 2);
                Card.drawCardBack(W - 13, startingY + (i * 2), TextColor.ANSI.WHITE);
            }

            // Our cards
            {
                int startingX = (W / 2) - (39) / 2 - (Card.width / 2);
                for (int i = 0; i < 13; i++) {
                    int x = startingX + (i * 3);
                    int y = H - 7;
                    Card.drawCardBack(x, y, TextColor.ANSI.WHITE);
                }
            }

            Main.drawText(W / 2, H / 2 - 1, "Players:");
            for (int i = 0; i < 4; i++) {
                Main.drawText(
                        W / 2,
                        H / 2 + i,
                        "Player "
                                + i
                                + " - "
                                + (i == localPlayer
                                        ? "Local"
                                        : remotePlayers[i] ? "Remote" : "Bot"));
            }

            if (!isRemoteGame()) Main.drawText(W / 2, H / 2 + 5, "Press enter to start game");
        } else if (gameState == HeartsEngine.GameState.TRICKS
                || gameState == HeartsEngine.GameState.PASSING) {
            // draw second player's cards
            {
                int y = (H / 2) - (Card.height / 2);
                int startingY = y - (numCards[1] * 2) / 2;
                for (int i = 0; i < numCards[1]; i++) {
                    Card.drawCardBack(4, startingY + (i * 2), TextColor.ANSI.WHITE);
                }
                g.setForegroundColor(TextColor.ANSI.WHITE);
                int infoX = 4 + (Card.width / 2);
                int infoY = y - 16;
                Main.drawText(infoX, infoY, "Player " + ((localPlayer + 1) % 4));
                Main.drawText(
                        infoX,
                        infoY + 1,
                        "Score: " + HeartsEngine.getScore(wonTricks.get(((localPlayer + 1) % 4))));
            }
            // draw third player's cards
            {
                int x = (W / 2) - (Card.width / 2);
                int startingX = x - (numCards[2] * 3) / 2;
                for (int i = 0; i < numCards[2]; i++) {
                    Card.drawCardBack(startingX + (i * 3), 2, TextColor.ANSI.WHITE);
                }
                g.setForegroundColor(TextColor.ANSI.WHITE);
                int infoX = x - 7 - (39 / 2);
                int infoY = 2 + (Card.height / 2);
                Main.drawText(infoX, infoY, "Player " + ((localPlayer + 2) % 4));
                Main.drawText(
                        infoX,
                        infoY + 1,
                        "Score: " + HeartsEngine.getScore(wonTricks.get((localPlayer + 2) % 4)));
            }
            // draw fourth player's cards
            {
                int y = (H / 2) - (Card.height / 2);
                int startingY = y - (numCards[3] * 2) / 2;
                for (int i = 0; i < numCards[3]; i++) {
                    Card.drawCardBack(W - 13, startingY + (i * 2), TextColor.ANSI.WHITE);
                }
                g.setForegroundColor(TextColor.ANSI.WHITE);
                int infoX = W - (4 + (Card.width / 2));
                int infoY = y - 16;
                Main.drawText(infoX, infoY, "Player " + ((localPlayer + 3) % 4));
                Main.drawText(
                        infoX,
                        infoY + 1,
                        "Score: " + HeartsEngine.getScore(wonTricks.get(((localPlayer + 3) % 4))));
            }

            // Our cards
            {
                List<Card> cards = ourCards;
                if (selectedCard < 0 || selectedCard >= cards.size()) selectedCard = 0;
                int preX = (W / 2) - (Card.width / 2);
                int startingX = preX - (cards.size() * 3) / 2;
                for (Card card : cards) {
                    TextColor cardColor = TextColor.ANSI.WHITE;

                    int index = cards.indexOf(card);

                    int x = startingX + (index * 3);
                    int y = H - 7;

                    if (currentPlayerturn == localPlayer
                            || gameState == HeartsEngine.GameState.PASSING) {
                        // elevate cursor and selected cards
                        y -=
                                (index == selectedCard ? 2 : 0)
                                        + (Arrays.stream(passingCards).toList().contains(card)
                                                ? 1
                                                : 0);
                        if (Arrays.stream(passingCards).toList().contains(card))
                            cardColor = passedCards ? TextColor.ANSI.BLUE : TextColor.ANSI.GREEN;
                        if (index == selectedCard) cardColor = TextColor.ANSI.YELLOW;
                    }
                    if (!getPlayableCards(cards).contains(card)
                            && gameState == HeartsEngine.GameState.TRICKS)
                        cardColor = Main.dimColor(cardColor, 0.5f);

                    card.setTextColor(cardColor);
                    card.draw(x, y, true);
                }
                g.setForegroundColor(TextColor.ANSI.WHITE);
                Main.drawText(
                        W / 2,
                        H - Card.height - 4,
                        "Score: " + HeartsEngine.getScore(wonTricks.get(localPlayer)));
            }

            // Player turn text, or passing text
            g.setForegroundColor(TextColor.ANSI.WHITE);
            g.enableModifiers(SGR.BOLD);
            if (gameState == HeartsEngine.GameState.PASSING) {
                Main.drawText(W / 2, H / 2, "Select cards to pass");
            } else if (gameState == HeartsEngine.GameState.TRICKS) {
                int turn = currentPlayerturn;
                String turnString =
                        turn != localPlayer
                                ? "It is Player " + turn + "'s turn"
                                : "It is your turn";
                Main.drawText(W / 2, H - Card.height - 3, turnString);
                switch ((turn + (4 - localPlayer)) % 4) {
                    case 0 -> g.setCharacter(W / 2, H / 2, '▼');
                    case 1 -> g.setCharacter(W / 2, H / 2, '◀');
                    case 2 -> g.setCharacter(W / 2, H / 2, '▲');
                    case 3 -> g.setCharacter(W / 2, H / 2, '▶');
                }
            }
            g.disableModifiers(SGR.BOLD);

            // Hearts broken text
            if (heartsBroken) {
                g.setForegroundColor(TextColor.ANSI.RED);
                Main.drawText(W / 2, H / 2 - (Card.height * 15 / 10) - 1, "Hearts are broken");
                g.setForegroundColor(TextColor.ANSI.WHITE);
            }

            // Draw played cards
            {
                List<Card> cards = playedCards;
                for (Card card : cards) {
                    int index = cards.indexOf(card) + startingPlayer - localPlayer;
                    int x = (W / 2) - (Card.width / 2),
                            y = (H / 2) - (me.pintoadmin.cardTerminal.Card.height / 2);

                    card.setTextColor(TextColor.ANSI.WHITE);

                    switch ((index + 4) % 4) {
                        case 0 -> y += (Card.height / 2) + 2;
                        case 1 -> x -= Card.width * 15 / 10;
                        case 2 -> y -= (Card.height / 2) + 2;
                        case 3 -> x += Card.width + 2;
                    }

                    card.draw(x, y, true);
                }
            }
        }

        if (escapeCount > 0) {
            g.setForegroundColor(TextColor.ANSI.RED);
            int timesLeft = 2 - escapeCount + 1;
            Main.drawTextLeft(
                    0,
                    H - 1,
                    "Press escape "
                            + timesLeft
                            + " more time"
                            + (timesLeft == 1 ? "" : "s")
                            + " to leave");
            g.setForegroundColor(TextColor.ANSI.WHITE);
        }

        screen.refresh(Screen.RefreshType.DELTA);
    }

    public synchronized void showScores() {
        try {
            screen.clear();
            int W = WIDTH.get(), H = HEIGHT.get();

            g.enableModifiers(SGR.BOLD);
            Main.drawText(W / 2, H / 2 - 7, "End of last round scores");

            Map<Integer, Integer> scores = this.scores;
            Map<Integer, Integer> sorted =
                    scores.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue())
                            .collect(
                                    Collectors.toMap(
                                            Map.Entry::getKey,
                                            Map.Entry::getValue,
                                            (oldValue, newValue) -> oldValue,
                                            LinkedHashMap::new));

            int i = 0;
            for (Map.Entry<Integer, Integer> entry : sorted.entrySet()) {
                int player = entry.getKey();
                int value = entry.getValue();

                String name = "Player " + player;
                if (i == localPlayer) name = "You";
                Main.drawTextLeft(W / 2 - 11, H / 2 - 4 + i, name);
                Main.drawTextRight(W / 2 + 5, H / 2 - 4 + i, String.valueOf(value));

                i++;
            }

            g.disableModifiers(SGR.BOLD);
            screen.refresh(Screen.RefreshType.COMPLETE);
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
    }

    @Override
    public Game newGame() {
        return new HeartsLocal();
    }

    @Override
    public Game restart() {
        return new HeartsLocal(SEED, endingPoints);
    }

    @Override
    public long getSeed() {
        return SEED;
    }

    @Override
    public void resize() {
        try {
            drawGame();
        } catch (IOException e) {
            System.out.println("Ignored exception: " + e.getMessage());
        }
    }

    @Override
    public Action handleInput(KeyStroke keyStroke) {
        if (keyStroke.getKeyType() != KeyType.Escape) escapeCount = 0;
        switch (keyStroke.getKeyType()) {
            case Escape:
                if (escapeCount < 2) {
                    if (gameState != HeartsEngine.GameState.PREGAME) escapeCount++;
                    resize();
                    return Action.NULL;
                } else {
                    if (!isRemoteGame()) engine.endGame();
                    return Action.BACK_TO_MENU;
                }
            case ArrowLeft:
                if (selectedCard > 0) selectedCard--;
                break;
            case ArrowRight:
                if (selectedCard < ourCards.size() - 1) selectedCard++;
                break;
            case Character:
                if (keyStroke.getCharacter() == ' ') {
                    if (gameState == HeartsEngine.GameState.PASSING && !passedCards) {
                        Card selected = ourCards.get(selectedCard);
                        if (Arrays.stream(passingCards).toList().contains(selected)) {
                            passingCards[Arrays.stream(passingCards).toList().indexOf(selected)] =
                                    null;
                        } else {
                            int firstNull = firstNull(passingCards);
                            if (firstNull != -1) {
                                passingCards[firstNull] = selected;
                            }
                        }
                    } else if (gameState == HeartsEngine.GameState.TRICKS) {
                        int turn = currentPlayerturn;
                        if (turn == localPlayer) {
                            Card selected = ourCards.get(selectedCard);
                            submitPlayCard(localPlayer, selected);
                        }
                    }
                    if (gameState != HeartsEngine.GameState.PREGAME) {
                        if (selectedCard >= ourCards.size() - 1 && selectedCard >= 0)
                            selectedCard = ourCards.size() - 1;
                    }
                }
                break;
            case Tab:
                showingScores = !showingScores;
                resize();
                return Action.NONE;
            case Enter:
                if (gameState == HeartsEngine.GameState.PREGAME) {
                    if (!isRemoteGame()) engine.start();
                } else if (gameState == HeartsEngine.GameState.PASSING) {
                    submitPassCards(localPlayer, passingCards);
                }
                break;
        }
        resize();
        return Action.NONE;
    }

    private int firstNull(Card[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) return i;
        }
        return -1;
    }

    private List<Card> getPlayableCards(List<Card> cards) {
        if (activeSuit != null) {
            List<Card> sameSuit = cards.stream().filter(c -> c.getSuit() == activeSuit).toList();
            List<Card> notSuit = cards.stream().filter(c -> c.getSuit() != activeSuit).toList();
            return sameSuit.isEmpty() ? notSuit : sameSuit;
        } else {
            List<Card> playable = new ArrayList<>(cards);
            if (trick == 0)
                playable.removeIf(
                        c ->
                                c.getSuit() != Card.Suit.CLUB
                                        || c.getRank() != me.pintoadmin.cardTerminal.Card.Rank.TWO);
            if (!heartsBroken) playable.removeIf(c -> c.getSuit() == Card.Suit.HEART);
            return playable;
        }
    }

    public Stats newStats() {
        return new Stats();
    }

    public class Stats {
        public final String seed = String.valueOf(HeartsLocal.this.SEED);
        public final int player = HeartsLocal.this.localPlayer;
        public final int winner =
                Collections.max(scores.entrySet(), Map.Entry.comparingByValue()).getKey();
        public final long elapsed = HeartsLocal.this.time.get();
        public final String time =
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy, hh:mm:ss a"));
    }
}
