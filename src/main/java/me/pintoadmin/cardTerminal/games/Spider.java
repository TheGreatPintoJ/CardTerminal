package me.pintoadmin.cardTerminal.games;

import static me.pintoadmin.cardTerminal.Main.*;
import static me.pintoadmin.cardTerminal.Main.g;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import java.io.*;
import java.util.*;
import java.util.function.*;
import me.pintoadmin.cardTerminal.*;

public class Spider extends Game {
    private final long SEED;
    private final Deck deck;
    private final List<List<Card>> stock =
            List.of(
                    new LinkedList<>(), new LinkedList<>(),
                    new LinkedList<>(), new LinkedList<>(),
                    new LinkedList<>(), new LinkedList<>(),
                    new LinkedList<>(), new LinkedList<>(),
                    new LinkedList<>(), new LinkedList<>()); // Ten piles

    private final Supplier<Integer> WIDTH = () -> g.getSize().getColumns();
    private final Supplier<Integer> HEIGHT = () -> g.getSize().getRows();

    private final Supplier<Integer> deckX = () -> 0;
    private final Supplier<Integer> deckY = () -> HEIGHT.get() - 14;

    enum GameType {
        SUIT1,
        SUIT2,
        SUIT3,
        SUIT4,
    }

    public Spider() {
        this(new Random().nextLong());
    }

    public Spider(long seed) {
        SEED = seed;
        deck = new Deck(SEED);

        for (int row = 0; row < 5; row++) {
            for (int column = 0; column < 10; column++) {
                stock.get(column).add(deck.drawCard());
            }
        }
        resize();
    }

    private void drawGame() throws IOException {
        screen.clear();
        Arrays.stream(SGR.values()).toList().forEach(g::disableModifiers);
        g.setBackgroundColor(BLACK);
        g.setForegroundColor(TextColor.ANSI.WHITE);
        drawTextRight(WIDTH.get() - 1, HEIGHT.get() - 1, "SEED: " + SEED);
        deck.drawDeck(deckX.get(), deckY.get());
        deck.drawDiscard(deckX.get() + 9, deckY.get());

        int startingX = g.getSize().getColumns() - 90;
        int startingY = 0;

        for (List<Card> column : stock) {
            for (Card card : column) {
                card.draw(startingX, startingY + (column.indexOf(card) * 3));
            }
            startingX += 9;
        }
        screen.refresh(Screen.RefreshType.AUTOMATIC);
    }

    @Override
    public Game newGame() {
        return new Spider();
    }

    @Override
    public Game restart() {
        return new Spider(SEED);
    }

    @Override
    public long getSeed() {
        return SEED;
    }

    @Override
    public void redraw() {}

    @Override
    public void drawCard(boolean logged) {}

    @Override
    public void resize() {
        try {
            drawGame();
        } catch (IOException e) {
            System.err.println("Could not draw spider game screen: " + e.getMessage());
        }
    }

    @Override
    public void handleInput(KeyStroke keyStroke) throws Exception {}
}
