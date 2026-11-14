package me.pintoadmin.cardTerminal.games.multiplayer.hearts;

import static me.pintoadmin.cardTerminal.Main.g;
import static me.pintoadmin.cardTerminal.Main.screen;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import me.pintoadmin.cardTerminal.*;
import me.pintoadmin.cardTerminal.screens.*;

/*
TODO: add hearts broken indicator
 */

public class HeartsLocal extends Game {
    private final long SEED;
    private final HeartsEngine engine;
    private final int endingPoints;

    private final Supplier<Integer> WIDTH = () -> g.getSize().getColumns();
    private final Supplier<Integer> HEIGHT = () -> g.getSize().getRows();

    private Card[] passingCards = new Card[3];

    private int selectedCard = 0;

    private List<Card> ourCards;
    private int currentPlayerturn;
    private int startingPlayer;
    private List<Card> playedCards;
    private HeartsEngine.GameState gameState;
    private Map<Integer, Integer> scores;
    private boolean heartsBroken;
    private Card.Suit activeSuit;
    private int[] numCards = new int[4];

    public void updateLocals() {
        ourCards = engine.getCards(0);
        currentPlayerturn = engine.getCurrentPlayerTurn();
        startingPlayer = engine.getStartingPlayer();
        playedCards = engine.getPlayedCards();
        gameState = engine.getState();
        scores = engine.getScores();
        heartsBroken = engine.isHeartsBroken();
        activeSuit = engine.activeSuit;

        for (int i = 0; i < 4; i++) numCards[i] = engine.getNumCards(i);
    }

    public HeartsLocal() {
        this(new Random().nextLong(), 100);
    }

    public HeartsLocal(long SEED, int endingPoints) {
        this.SEED = SEED;
        this.endingPoints = endingPoints;

        engine = new HeartsEngine(SEED, endingPoints, this);
        engine.start();

        resize();
    }

    private void drawGame() throws IOException {
        screen.clear();

        int W = WIDTH.get(), H = HEIGHT.get();

        // draw second player's cards
        for (int i = 0; i < numCards[1]; i++) {
            int startingY = (H / 2) - (numCards[1] * 2) / 2 - (Card.height / 2);
            Card.drawCardBack(4, startingY + (i * 2), TextColor.ANSI.WHITE);
        }
        // draw third player's cards
        for (int i = 0; i < numCards[2]; i++) {
            int startingX = (W / 2) - (numCards[2] * 3) / 2 - (Card.width / 2);
            Card.drawCardBack(startingX + (i * 3), 2, TextColor.ANSI.WHITE);
        }
        // draw fourth player's cards
        for (int i = 0; i < numCards[3]; i++) {
            int startingY = (H / 2) - (numCards[3] * 2) / 2 - (Card.height / 2);
            Card.drawCardBack(W - 13, startingY + (i * 2), TextColor.ANSI.WHITE);
        }

        {
            List<Card> cards = ourCards;
            int startingX = (W / 2) - (cards.size() * 3) / 2 - (Card.width / 2);
            for (Card card : cards) {
                card.setTextColor(TextColor.ANSI.WHITE);
                int index = cards.indexOf(card);

                int x = startingX + (index * 3);
                int y = H - 7;

                if (currentPlayerturn == 0) {
                    // elevate cursor and selected cards
                    y -=
                            (index == selectedCard ? 2 : 0)
                                    + (Arrays.stream(passingCards).toList().contains(card) ? 1 : 0);
                    if (Arrays.stream(passingCards).toList().contains(card))
                        card.setTextColor(TextColor.ANSI.GREEN);
                    if (index == selectedCard) card.setTextColor(TextColor.ANSI.YELLOW);
                }
                card.draw(x, y, true);
            }
        }

        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.enableModifiers(SGR.BOLD);
        if (gameState == HeartsEngine.GameState.PASSING) {
            Main.drawText(W / 2, H / 2, "Select cards to pass");
        } else if (gameState == HeartsEngine.GameState.TRICKS) {
            int turn = currentPlayerturn;
            String turnString = turn != 0 ? "It is Player " + turn + "'s turn" : "It is your turn";
            Main.drawText(W / 2, H - Card.height - 3, turnString);
        }
        g.disableModifiers(SGR.BOLD);

        {
            List<Card> cards = playedCards;
            for (Card card : cards) {
                int index = cards.indexOf(card) + startingPlayer;
                int x = (W / 2) - (Card.width / 2), y = (H / 2) - (Card.height / 2);

                card.setTextColor(TextColor.ANSI.WHITE);

                switch (index % 4) {
                    case 0 -> y += (Card.height / 2) + 2;
                    case 1 -> x -= Card.width * 15 / 10;
                    case 2 -> y -= (Card.height / 2) + 2;
                    case 3 -> x += Card.width + 2;
                }

                card.draw(x, y, true);
            }
        }

        screen.refresh(Screen.RefreshType.COMPLETE);
    }

    void showScores() {
        try {
            screen.clear();
            int W = WIDTH.get(), H = HEIGHT.get();

            g.enableModifiers(SGR.BOLD);
            Main.drawText(W / 2, H / 2 - 7, "End of round scores");

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
    public void drawCard(boolean logged) {}

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
        switch (keyStroke.getKeyType()) {
            case Escape:
                Main.getInstance().setCurrentScreen(new MainMenu());
                return Action.BACK_TO_MENU;
            case ArrowLeft:
                if (selectedCard > 0) selectedCard--;
                break;
            case ArrowRight:
                if (selectedCard < ourCards.size() - 1) selectedCard++;
                break;
            case Character:
                if (keyStroke.getCharacter() == ' ') {
                    if (gameState == HeartsEngine.GameState.PASSING) {
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
                        if (turn == 0) {
                            Card selected = ourCards.get(selectedCard);
                            engine.playCard(0, selected);
                        } else {
                            Card card = getPlayableCards(engine.getCards(turn)).getFirst();
                            engine.playCard(turn, card);
                        }
                    }
                    if (selectedCard >= ourCards.size() - 1 && selectedCard >= 0)
                        selectedCard = ourCards.size() - 1;
                }
                break;
            case Enter:
                if (gameState == HeartsEngine.GameState.PASSING) {
                    engine.passCards(0, passingCards);
                    // testing
                    engine.passCards(1, engine.getCards(1).subList(0, 3).toArray(new Card[0]));
                    engine.passCards(2, engine.getCards(2).subList(0, 3).toArray(new Card[0]));
                    engine.passCards(3, engine.getCards(3).subList(0, 3).toArray(new Card[0]));
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
            return heartsBroken
                    ? cards.stream().toList()
                    : cards.stream().filter(c -> c.getSuit() != Card.Suit.HEART).toList();
        }
    }
}
