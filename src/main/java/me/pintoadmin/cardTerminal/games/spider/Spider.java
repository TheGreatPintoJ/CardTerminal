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
    private final LinkedList<LinkedList<Card>> stock = new LinkedList<>(); // Ten piles
    private final LinkedList<SpiderMove> moves = new LinkedList<>();
    private final LinkedList<SpiderMove> movesUndone = new LinkedList<>();

    private final Supplier<Integer> WIDTH = () -> g.getSize().getColumns();
    private final Supplier<Integer> HEIGHT = () -> g.getSize().getRows();

    private final Supplier<Integer> deckX = () -> 0;
    private final Supplier<Integer> deckY = () -> HEIGHT.get() - 7;

    private int selectedColumn = -1;
    private Card selectedCard;

    private int drawCount = 0;

    private int finishedRuns = 0;

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
                try {
                    stock.get(column).add(deck.drawCard());
                } catch (IndexOutOfBoundsException e) {
                    stock.add(new LinkedList<>());
                    stock.get(column).add(deck.drawCard());
                }
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

        for (LinkedList<Card> column : stock) {
            if (!column.isEmpty()) column.getLast().flip(true);
            else Card.drawCardSpace(startingX, startingY);
            LinkedList<Card> lastRun =
                    getLastRun(new LinkedList<>(column.stream().filter(Card::isFaceup).toList()));
            for (Card card : column) {
                card.setTextColor(
                        lastRun.contains(card)
                                ? TextColor.ANSI.WHITE
                                : new TextColor.RGB(75, 75, 75));
                if (stock.indexOf(column) == selectedColumn) {
                    if (selectedCard == null) {
                        if (card.isFaceup() && lastRun.contains(card))
                            card.setTextColor(new TextColor.RGB(255, 200, 0));
                    }
                    if (card == selectedCard) card.setTextColor(TextColor.ANSI.GREEN);
                }
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
            SpiderMove move = new SpiderMove(this, null, null, SpiderMove.Type.DEAL);
            moves.add(move);
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
                if (i == -1) i = 9;
                // move corresponding stock piles
                if (selectedCard == null) {
                    if (selectedColumn == -1) {
                        LinkedList<Card> column = stock.get(i);
                        if (!column.isEmpty()) {
                            selectedColumn = i;
                            if (column.stream().filter(Card::isFaceup).count() == 1)
                                selectCard(Location.STOCK, 0);
                        }
                    } else {
                        if (stock.get(selectedColumn).stream().filter(Card::isFaceup).count() > i)
                            selectCard(Location.STOCK, i);
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
                    case '[':
                        undo();
                        break;
                    case ']':
                        redo();
                        break;
                }
            }
        } else if (keyStroke.getKeyType() == KeyType.F12) {
            // Reset game
            throw new Exception("reset");
        } else if (keyStroke.getKeyType() == KeyType.F10) {
            // New game
            throw new Exception("newseed");
        } else if (keyStroke.getKeyType().name().contains("Arrow")) {
            switch (keyStroke.getKeyType()) {
                case ArrowRight:
                    if (selectedColumn < 10) selectedColumn++;
                    selectCard(Location.STOCK, 0);
                    break;
                case ArrowLeft:
                    if (selectedColumn >= 0) selectedColumn--;
                    selectCard(Location.STOCK, 0);
                    break;
                case ArrowUp:
                    if (selectedColumn == -1) break;
                    {
                        LinkedList<Card> column = stock.get(selectedColumn);
                        LinkedList<Card> thisRun = getLastRun(column);
                        int cardIndex = thisRun.indexOf(selectedCard);
                        if (cardIndex >= 0) selectCard(Location.STOCK, cardIndex - 1);
                    }
                    break;
                case ArrowDown:
                    if (selectedColumn == -1) break;
                    {
                        LinkedList<Card> column = stock.get(selectedColumn);
                        LinkedList<Card> thisRun = getLastRun(column);
                        int cardIndex = thisRun.indexOf(selectedCard);
                        if (thisRun.size() - 1 > cardIndex)
                            selectCard(Location.STOCK, cardIndex + 1);
                    }
                    break;
            }
        }

        drawGame();
        screen.refresh(Screen.RefreshType.DELTA);

        LinkedList<LinkedList<Card>> stockSnap = new LinkedList<>(stock);
        for (LinkedList<Card> pile : stock) {
            if (pile.size() >= 13) {
                LinkedList<Card> lastRun = getLastRun(pile);
                if (lastRun.getFirst().getRank() == Card.Rank.KING
                        && lastRun.getLast().getRank() == Card.Rank.ACE) {
                    pile.removeAll(lastRun);
                    finishedRuns++;
                }
            }
        }
        if (!stock.equals(stockSnap)) {
            drawGame();
            screen.refresh(Screen.RefreshType.DELTA);
        }
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
                selectedCard =
                        getLastRun(stock.get(selectedColumn)).stream()
                                .filter(Card::isFaceup)
                                .toList()
                                .get(index);
                break;
        }
        if (selectedCard != null)
            selectedCard.setTextColor(
                    selectedCard.isFaceup() ? TextColor.ANSI.GREEN : TextColor.ANSI.RED);
    }

    private void moveSelectedCard(int index) {
        selectedCard.setTextColor(TextColor.ANSI.WHITE);
        SpiderMove move = new SpiderMove(this, selectedCard, index, SpiderMove.Type.MOVE);
        if (move.execute()) {
            moves.add(move);
            movesUndone.clear();
        }

        selectCard(null, -1);
    }

    private void undo() {
        if (moves.isEmpty()) return;
        SpiderMove move = moves.getLast();
        move.undo();
        movesUndone.add(move);
        moves.remove(move);
    }

    private void redo() {
        if (movesUndone.isEmpty()) return;
        movesUndone.getLast().execute();
        moves.add(movesUndone.getLast());
        movesUndone.removeLast();
    }

    public LinkedList<Card> getRun(LinkedList<Card> input, int afterIndex) {
        LinkedList<Card> subList = new LinkedList<>(input.subList(afterIndex, input.size()));
        if (subList.isEmpty()) return new LinkedList<>();
        LinkedList<Card> currentList = new LinkedList<>();
        Card.Rank lastRank = subList.getFirst().getRank();

        currentList.add(subList.getFirst());
        for (Card card : subList) {
            if (card.getRank().toInt() != lastRank.toInt() - 1) break;
            lastRank = card.getRank();
            currentList.add(card);
        }

        return currentList;
    }

    public LinkedList<Card> getLastRun(LinkedList<Card> input) {
        if (input.isEmpty()) return new LinkedList<>();
        LinkedList<Card> currentList = new LinkedList<>();
        Card.Rank lastRank = input.getFirst().getRank();

        currentList.add(input.getFirst());
        for (Card card : input.subList(1, input.size())) {
            if (card.getRank().toInt() != lastRank.toInt() - 1) {
                currentList.clear();
            }
            lastRank = card.getRank();
            currentList.add(card);
        }

        return currentList;
    }

    public Deck getDeck() {
        return deck;
    }

    public LinkedList<LinkedList<Card>> getStock() {
        return stock;
    }
}
