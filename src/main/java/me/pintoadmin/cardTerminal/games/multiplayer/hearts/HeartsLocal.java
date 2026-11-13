package me.pintoadmin.cardTerminal.games.multiplayer.hearts;

import static me.pintoadmin.cardTerminal.Main.g;
import static me.pintoadmin.cardTerminal.Main.screen;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import java.io.*;
import java.util.*;
import java.util.function.*;
import me.pintoadmin.cardTerminal.*;
import me.pintoadmin.cardTerminal.screens.*;

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

        engine = new HeartsEngine(SEED, endingPoints);
        engine.start();

        resize();
    }

    private void drawGame() throws IOException {
        screen.clear();

        int W = WIDTH.get(), H = HEIGHT.get();

        {
            List<Card> cards = engine.getCards(3);
            for (int i = 0; i < engine.getCards(3).size(); i++) {
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
                int y =
                        H
                                - 7
                                - (index == selectedCard ? 2 : 0)
                                - (Arrays.stream(passingCards).toList().contains(card) ? 1 : 0);

                if (Arrays.stream(passingCards).toList().contains(card))
                    card.setTextColor(TextColor.ANSI.GREEN);
                if (index == selectedCard) card.setTextColor(TextColor.ANSI.YELLOW);
                card.draw(x, y, true);
            }
        }

        if (engine.getState() == HeartsEngine.GameState.PASSING) {
            Main.drawText(W / 2, H / 2, "Select cards to pass");
        }

        {
            List<Card> cards = engine.getPlayedCards();
            for (Card card : cards) {
                int index = cards.indexOf(card);
                int x = (W / 2) - (Card.width / 2), y = (H / 2) - (Card.height / 2);

                switch (index) {
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
                                // engine.getCards(0).remove(selected);
                            }
                        }
                    } else if (engine.getState() == HeartsEngine.GameState.TRICKS) {
                        Card selected = engine.getCards(0).get(selectedCard);
                        if (engine.playCard(0, selected)) {
                            // testing
                            Card selected1 = engine.getPlayableCards(1).getFirst();
                            engine.playCard(1, selected1);
                            Card selected2 = engine.getPlayableCards(2).getFirst();
                            engine.playCard(2, selected2);
                            Card selected3 = engine.getPlayableCards(3).getFirst();
                            engine.playCard(3, selected3);
                        }
                    }
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
