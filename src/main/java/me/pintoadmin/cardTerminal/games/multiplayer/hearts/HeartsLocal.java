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

public class HeartsLocal extends Game {
    private final long SEED;
    private final HeartsEngine engine;
    private final int endingPoints;

    private final Supplier<Integer> WIDTH = () -> g.getSize().getColumns();
    private final Supplier<Integer> HEIGHT = () -> g.getSize().getRows();

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

        {
            List<Card> cards = engine.getCards(3);
            for (int i = 0; i < engine.getCards(3).size(); i++) {
                int startingY = (HEIGHT.get() / 2) - (cards.size() * 2) / 2 - (Card.height / 2);
                Card.drawCardBack(4, startingY + (i * 2), TextColor.ANSI.WHITE);
            }
        }
        {
            List<Card> cards = engine.getCards(2);
            for (int i = 0; i < engine.getCards(2).size(); i++) {
                int startingX = (WIDTH.get() / 2) - (cards.size() * 3) / 2 - (Card.width / 2);
                Card.drawCardBack(startingX + (i * 3), 2, TextColor.ANSI.WHITE);
            }
        }
        {
            List<Card> cards = engine.getCards(3);
            for (int i = 0; i < engine.getCards(3).size(); i++) {
                int startingY = (HEIGHT.get() / 2) - (cards.size() * 2) / 2 - (Card.height / 2);
                Card.drawCardBack(WIDTH.get() - 13, startingY + (i * 2), TextColor.ANSI.WHITE);
            }
        }

        {
            List<Card> cards = engine.getCards(0);
            cards.sort(Comparator.comparing(Card::getSuit).thenComparing(Card::getRank));
            int startingX = (WIDTH.get() / 2) - (cards.size() * 3) / 2 - (Card.width / 2);
            for (Card card : cards) {
                int index = cards.indexOf(card);

                int x = startingX + (index * 3);
                int y = HEIGHT.get() - 7;

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
        return Action.NONE;
    }
}
