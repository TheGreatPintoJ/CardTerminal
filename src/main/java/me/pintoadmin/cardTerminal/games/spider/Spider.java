package me.pintoadmin.cardTerminal.games.spider;

import static me.pintoadmin.cardTerminal.Main.*;
import static me.pintoadmin.cardTerminal.Main.g;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import java.io.*;
import java.util.*;
import java.util.function.*;
import me.pintoadmin.cardTerminal.*;
import me.pintoadmin.cardTerminal.games.solitaire.*;
import me.pintoadmin.cardTerminal.screens.*;

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
    private final LinkedList<SolitaireMove> moves = new LinkedList<>();
    private final LinkedList<SolitaireMove> movesUndone = new LinkedList<>();

    private final Supplier<Integer> WIDTH = () -> g.getSize().getColumns();
    private final Supplier<Integer> HEIGHT = () -> g.getSize().getRows();

    private final Supplier<Integer> deckX = () -> 0;
    private final Supplier<Integer> deckY = () -> HEIGHT.get() - 7;

    private int selectedColumn = -1;
    private Card selectedCard;

    private int drawCount = 0;

    enum GameType {
        SUIT1,
        SUIT2,
        SUIT3,
        SUIT4,
    }

    enum Location {
        DISCARD,
        STOCK,
    }

    public Spider() {
        this(new Random().nextLong());
    }

    public Spider(long seed) {
        SEED = seed;
        deck = new SpiderDeck(SEED);

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
        g.setCharacter(deckX.get() + 4, deckY.get() + 3, String.valueOf(8 - drawCount).charAt(0));

        int startingX = g.getSize().getColumns() - 90;
        int startingY = 0;

        for (List<Card> column : stock) {
            for (Card card : column) {
                card.setTextColor(TextColor.ANSI.WHITE);
                if (stock.indexOf(column) == selectedColumn) {
                    if (selectedCard == null) {
                        card.setTextColor(
                                card.isFaceup()
                                        ? new TextColor.RGB(255, 200, 0)
                                        : TextColor.ANSI.RED);
                    }
                    if (card == selectedCard) {
                        card.setTextColor(TextColor.ANSI.GREEN);
                    }
                }

                card.draw(startingX, startingY + (column.indexOf(card) * 3));
                if (column.getLast() == card) card.flip(true);
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
    public void redraw() {
        Main.screen.clear();
        deck.redrawDeck();
        g.setCharacter(deckX.get() + 4, deckY.get() + 3, String.valueOf(8 - drawCount).charAt(0));
        deck.redrawDiscard();

        for (List<Card> list : stock) {
            for (Card card : list) {
                card.redraw();
            }
        }

        for (List<Card> list : stock) {
            if (list.isEmpty()) continue;
            list.getLast().flip(true);
        }
    }

    @Override
    public void drawCard(boolean logged) {
        for (int column = 0; column < 10; column++) {
            Card card = deck.drawCard();
            if (card == null) break;
            stock.get(column).add(card);
        }
        drawCount++;

        resize();
    }

    @Override
    public void resize() {
        try {
            drawGame();
        } catch (IOException e) {
            System.err.println("Could not draw spider game screen: " + e.getMessage());
        }
    }

    @Override
    public void handleInput(KeyStroke keyStroke) throws Exception {
        if (keyStroke.getKeyType() == KeyType.Escape) {
            Main.getInstance().setCurrentScreen(new MainMenu());
            throw new Exception("override");
        } else if (keyStroke.getKeyType() == KeyType.Character) {
            Character c = keyStroke.getCharacter();
            if (Character.isDigit(c)) {
                int i = Integer.parseInt(c.toString()) - 1;
                if (i == -1) i = 10;
                // move corresponding stock piles
                if (selectedCard == null) {
                    if (selectedColumn == -1) {
                        selectedColumn = i;
                    } else {
                        if (stock.size() < i) selectCard(Location.STOCK, i);
                    }
                } else {
                    moveSelectedCard(i);
                }
            } else {
                switch (c) {
                    case '`':
                        selectCard(null, -1);
                        drawCard(true);
                        break;
                    case ' ':
                        // select discard card
                        selectCard(Location.DISCARD, 0);
                        break;
                    case '-':
                        selectCard(null, -1);
                        break;
                }
            }
        }
        drawGame();
        screen.refresh(Screen.RefreshType.DELTA);
    }

    private void selectCard(Spider.Location loc, int index) {
        if (loc == null || index == -1) {
            selectedCard = null;
            selectedColumn = -1;
            return;
        }
        if (selectedCard != null) selectedCard.setTextColor(TextColor.ANSI.WHITE);
        switch (loc) {
            case DISCARD:
                selectedCard = deck.getTopDiscard();
                break;
            case STOCK:
                if (selectedColumn == -1) return;
                selectedCard = stock.get(selectedColumn).get(index);
                break;
        }
        if (selectedCard != null)
            selectedCard.setTextColor(
                    selectedCard.isFaceup() ? TextColor.ANSI.GREEN : TextColor.ANSI.RED);
    }

    private void moveSelectedCard(int index) {
        selectedCard.setTextColor(TextColor.ANSI.WHITE);
        //        SpiderMove move =
        //                new SpiderMove(
        //                        this,
        //                        selectedCard,
        //                        selectedCardLocation.get(),
        //                        toLocation,
        //                        index,
        //                        SpiderMove.Type.MOVE);
        //        if (move.execute()) {
        //            moves.add(move);
        //            movesUndone.clear();
        //        }

        selectCard(null, -1);
    }
}
