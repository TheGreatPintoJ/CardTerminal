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

        {
            List<Card> cards = engine.getCards(1);
            for (int i = 0; i < engine.getCards(1).size(); i++) {
                int startingY = (H / 2) - (cards.size() * 2) / 2 - (Card.height / 2);
                Card.drawCardBack(4, startingY + (i * 2), TextColor.ANSI.WHITE);
            }
        }
        {
            List<Card> cards = engine.getCards(2);
            for (int i = 0; i < engine.getCards(2).size(); i++) {
                int startingX = (W / 2) - (cards.size() * 3) / 2 - (Card.width / 2);
                Card.drawCardBack(startingX + (i * 3), 2, TextColor.ANSI.WHITE);
            }
        }
        {
            List<Card> cards = engine.getCards(3);
            for (int i = 0; i < engine.getCards(3).size(); i++) {
                int startingY = (H / 2) - (cards.size() * 2) / 2 - (Card.height / 2);
                Card.drawCardBack(W - 13, startingY + (i * 2), TextColor.ANSI.WHITE);
            }
        }

        {
            List<Card> cards = engine.getCards(0);
            Function<Card, Card.Rank> rank = Card::getRank;
            Function<Card.Rank, Integer> value = Card.Rank::toIntAceHigh;
            cards.sort(Comparator.comparing(Card::getSuit).thenComparing(rank.andThen(value)));
            int startingX = (W / 2) - (cards.size() * 3) / 2 - (Card.width / 2);
            for (Card card : cards) {
                card.setTextColor(TextColor.ANSI.WHITE);
                int index = cards.indexOf(card);

                int x = startingX + (index * 3);
                int y = H - 7;

                if (engine.getCurrentPlayerTurn() == 0) {
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
        if (engine.getState() == HeartsEngine.GameState.PASSING) {
            Main.drawText(W / 2, H / 2, "Select cards to pass");
        } else if (engine.getState() == HeartsEngine.GameState.TRICKS) {
            int turn = engine.getCurrentPlayerTurn();
            String turnString = turn != 0 ? "It is Player " + turn + "'s turn" : "It is your turn";
            Main.drawText(W / 2, H - Card.height - 3, turnString);
        }
        g.disableModifiers(SGR.BOLD);

        {
            List<Card> cards = engine.getPlayedCards();
            for (Card card : cards) {
                int index = cards.indexOf(card) + engine.getStartingPlayer();
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

    void dealEnd() throws IOException {
        screen.clear();
        int W = WIDTH.get(), H = HEIGHT.get();

        g.enableModifiers(SGR.BOLD);
        Main.drawText(W / 2, H / 2 - 7, "End of round scores");

        Map<Integer, Integer> scores = engine.getScores();
        Map<Integer, Integer> sorted =
                scores.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue())
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        (oldValue, newValue) -> oldValue,
                                        LinkedHashMap::new));
        ;
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
                if (selectedCard < engine.getCards(0).size() - 1) selectedCard++;
                break;
            case Character:
                if (keyStroke.getCharacter() == ' ') {
                    if (engine.getState() == HeartsEngine.GameState.PASSING) {
                        Card selected = engine.getCards(0).get(selectedCard);
                        if (Arrays.stream(passingCards).toList().contains(selected)) {
                            passingCards[Arrays.stream(passingCards).toList().indexOf(selected)] =
                                    null;
                        } else {
                            int firstNull = firstNull(passingCards);
                            if (firstNull != -1) {
                                passingCards[firstNull] = selected;
                            }
                        }
                    } else if (engine.getState() == HeartsEngine.GameState.TRICKS) {
                        int turn = engine.getCurrentPlayerTurn();
                        if (turn == 0) {
                            Card selected = engine.getCards(0).get(selectedCard);
                            engine.playCard(0, selected);
                        } else {
                            Card card = engine.getPlayableCards(turn).getFirst();
                            engine.playCard(turn, card);
                        }
                    }
                    if (selectedCard >= engine.getCards(0).size() - 1 && selectedCard >= 0)
                        selectedCard = engine.getCards(0).size() - 1;
                }
                break;
            case Enter:
                if (engine.getState() == HeartsEngine.GameState.PASSING) {
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
}
