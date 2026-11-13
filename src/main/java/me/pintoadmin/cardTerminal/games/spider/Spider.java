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
import me.pintoadmin.cardTerminal.screens.*;

public class Spider extends Game {
    private final long SEED;
    private final GameType type;
    private final Deck deck;
    private final LinkedList<LinkedList<Card>> stock = new LinkedList<>(); // Ten piles
    private final LinkedList<SpiderMove> moves = new LinkedList<>();
    private final LinkedList<SpiderMove> movesUndone = new LinkedList<>();

    private final Supplier<Integer> WIDTH = () -> g.getSize().getColumns();
    private final Supplier<Integer> HEIGHT = () -> g.getSize().getRows();

    private final Supplier<Integer> deckX = () -> 0;
    private final Supplier<Integer> deckY = () -> HEIGHT.get() - 7;

    private int selectedColumn = -1;
    private int selectedRow = -1;
    private Card selectedCard;

    int dealCount = 0;

    int finishedRuns = 0;

    public enum GameType {
        SUIT1,
        SUIT2,
        SUIT4,
    }

    public Spider() {
        this(new Random().nextLong(), GameType.SUIT1);
    }

    public Spider(long seed, GameType type) {
        SEED = seed;
        this.type = type;
        deck = new SpiderDeck(SEED, type);

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

    private Spider(long SEED, GameType type, LinkedList<SpiderMove> moves) {
        this(SEED, type);
        try {
            screen.refresh(Screen.RefreshType.COMPLETE);
        } catch (IOException e) {
            System.out.println("Ignored exception: " + e.getMessage());
        }

        LinkedList<SpiderMove> newMoves = new LinkedList<>();
        for (SpiderMove move : moves) {
            Card card =
                    deck.getCard(
                            move.getCard().getRank(),
                            move.getCard().getSuit(),
                            move.getCard().getDeckNumber());
            SpiderMove newMove = new SpiderMove(this, card, move.getToIndex(), move.getType());
            newMoves.add(newMove);
        }

        for (SpiderMove move : new LinkedList<>(newMoves)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                System.out.println("Ignored exception: " + e.getMessage());
            }
            move.execute();
            try {
                drawGame();
                screen.refresh(Screen.RefreshType.DELTA);
            } catch (IOException e) {
                System.out.println("Ignored exception: " + e.getMessage());
            }
        }
    }

    private void drawGame() throws IOException {
        screen.clear();
        Arrays.stream(SGR.values()).toList().forEach(g::disableModifiers);
        g.setBackgroundColor(BLACK);
        g.setForegroundColor(TextColor.ANSI.WHITE);
        drawTextRight(WIDTH.get() - 1, HEIGHT.get() - 1, "SEED: " + SEED);
        Card.drawCardBack(deckX.get(), deckY.get(), TextColor.ANSI.WHITE);
        g.setCharacter(
                deckX.get() + 4,
                deckY.get() + 3,
                TextCharacter.fromCharacter(
                        String.valueOf(6 - dealCount).charAt(0), null, null, SGR.REVERSE)[0]);

        for (int i = 0; i < finishedRuns; i++) {
            Card.drawCardBack(2 * i, 0, TextColor.ANSI.WHITE);
            g.setCharacter(
                    2 * i + 4,
                    3,
                    TextCharacter.fromCharacter(
                            Integer.valueOf(i + 1).toString().charAt(0),
                            TextColor.ANSI.WHITE,
                            BLACK,
                            SGR.REVERSE)[0]);
        }

        int startingX = g.getSize().getColumns() - 90;
        int startingY = 0;

        int index = 0;
        for (LinkedList<Card> column : stock) {
            if (!column.isEmpty()) column.getLast().setFlipped(true);
            else Card.drawCardSpace(startingX, startingY);
            LinkedList<Card> lastRun =
                    getLastRun(new LinkedList<>(column.stream().filter(Card::isFaceup).toList()));
            for (Card card : column) {
                card.setTextColor(
                        lastRun.contains(card)
                                ? TextColor.ANSI.WHITE
                                : new TextColor.RGB(75, 75, 75));
                if (stock.indexOf(column) == selectedColumn && selectedCard == null) {
                    if (card.isFaceup()
                            && lastRun.contains(card)
                            && selectedRow == lastRun.indexOf(card))
                        card.setTextColor(new TextColor.RGB(255, 200, 0));
                }
                if (card == selectedCard) card.setTextColor(TextColor.ANSI.GREEN);
                card.draw(startingX, startingY + (column.indexOf(card) * 3));
            }

            if (selectedColumn == index) {
                g.setCharacter(
                        startingX + 4,
                        column.size() * 3 + 5,
                        TextCharacter.fromCharacter(
                                '▲', TextColor.ANSI.WHITE_BRIGHT, BLACK, SGR.BOLD)[0]);
            }

            startingX += 9;
            index++;
        }
        screen.refresh(Screen.RefreshType.AUTOMATIC);
    }

    @Override
    public Game newGame() {
        return new Spider();
    }

    @Override
    public Game restart() {
        return new Spider(SEED, type);
    }

    @Override
    public long getSeed() {
        return SEED;
    }

    //    @Override
    //    public void redraw() {
    //        Main.screen.clear();
    //        deck.redrawDeck();
    //        g.setCharacter(deckX.get() + 4, deckY.get() + 3, String.valueOf(8 -
    // dealCount).charAt(0));
    //        deck.redrawDiscard();
    //
    //        for (List<Card> list : stock) {
    //            for (Card card : list) {
    //                card.redraw();
    //            }
    //        }
    //
    //        for (List<Card> list : stock) {
    //            if (list.isEmpty()) continue;
    //            list.getLast().flip(true);
    //        }
    //    }

    @Override
    public void drawCard(boolean logged) {
        SpiderMove move = new SpiderMove(this, null, null, SpiderMove.Type.DEAL);
        if (move.execute()) moves.add(move);
        dealCount++;

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
    public Action handleInput(KeyStroke keyStroke) {
        if (keyStroke.getKeyType() == KeyType.Escape) {
            Main.getInstance().setCurrentScreen(new MainMenu());
            return Action.BACK_TO_MENU;
        } else if (keyStroke.getKeyType() == KeyType.Character) {
            Character c = keyStroke.getCharacter();
            if (Character.isDigit(c)) {
                int i = Integer.parseInt(c.toString()) - 1;
                if (i == -1) i = 9;
                selectedColumn = i;
            } else {
                switch (c) {
                    case '`':
                        selectedCard = null;
                        drawCard(true);
                        break;
                    case ' ':
                        if (selectedCard != null) {
                            moveSelectedCard(selectedColumn);
                            movesUndone.clear();
                        } else selectCard();
                        break;
                    case '-':
                        selectedCard = null;
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
            return Action.RESTART;
        } else if (keyStroke.getKeyType() == KeyType.F10) {
            // New game
            return Action.NEW_GAME;
        } else if (keyStroke.getKeyType().name().contains("Arrow")) {
            switch (keyStroke.getKeyType()) {
                case ArrowRight:
                    if (selectedColumn < 9) selectedColumn++;
                    selectedRow = 0;
                    break;
                case ArrowLeft:
                    if (selectedColumn > 0) selectedColumn--;
                    selectedRow = 0;
                    break;
                case ArrowUp:
                    if (selectedCard != null) break;
                    if (selectedColumn == -1) break;
                    if (selectedRow <= 0) break;
                    selectedRow--;
                    //                    {
                    //                        LinkedList<Card> column = stock.get(selectedColumn);
                    //                        LinkedList<Card> thisRun = getLastRun(column);
                    //                        int cardIndex = thisRun.indexOf(selectedCard);
                    //                        if (cardIndex >= 0) selectCard(Location.STOCK,
                    // cardIndex - 1);
                    //                    }
                    break;
                case ArrowDown:
                    if (selectedCard != null) break;
                    if (selectedColumn == -1) break;
                    LinkedList<Card> column = getLastRun(stock.get(selectedColumn));
                    if (column.size() - 1 <= selectedRow) break;
                    selectedRow++;
                    //                    {
                    //                        LinkedList<Card> column = stock.get(selectedColumn);
                    //                        LinkedList<Card> thisRun = getLastRun(column);
                    //                        int cardIndex = thisRun.indexOf(selectedCard);
                    //                        if (thisRun.size() - 1 > cardIndex)
                    //                            selectCard(Location.STOCK, cardIndex + 1);
                    //                    }
                    break;
            }
        }

        try {

            drawGame();
            screen.refresh(Screen.RefreshType.DELTA);

            {
                boolean didSomething = false;
                for (LinkedList<Card> pile : stock) {
                    if (pile.size() >= 13) {
                        LinkedList<Card> lastRun = getLastRun(pile);
                        if (lastRun.getFirst().getRank() == Card.Rank.KING
                                && lastRun.getLast().getRank() == Card.Rank.ACE) {
                            SpiderMove move =
                                    new SpiderMove(
                                            this, null, stock.indexOf(pile), SpiderMove.Type.RUN);
                            move.setRunCards(lastRun);
                            if (move.execute()) {
                                moves.add(move);
                                finishedRuns++;
                                didSomething = true;
                            }
                        }
                    }
                }
                if (didSomething) {
                    Thread.sleep(250);
                    drawGame();
                    screen.refresh(Screen.RefreshType.DELTA);
                }
            }

            if (finishedRuns == 8) {
                win();
                new Spider(SEED, type, moves);
                Thread.sleep(1000);
                Main.getInstance().setCurrentScreen(new MainMenu());
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Ignored exception: " + e.getMessage());
        }

        return Action.NONE;
    }

    private void selectCard() {
        if (selectedCard != null) selectedCard.setTextColor(TextColor.ANSI.WHITE);
        if (selectedColumn <= -1 || selectedRow <= -1) return;
        LinkedList<Card> thisRun =
                new LinkedList<>(
                        getLastRun(stock.get(selectedColumn)).stream()
                                .filter(Card::isFaceup)
                                .toList());
        if (selectedRow >= thisRun.size()) return;
        selectedCard = thisRun.get(selectedRow);
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

        selectedCard = null;
    }

    private void undo() {
        selectedCard = null;
        if (moves.isEmpty()) return;
        SpiderMove move = moves.getLast();
        move.undo();
        movesUndone.add(move);
        moves.remove(move);
        if (move.getType() == SpiderMove.Type.DEAL) dealCount--;
        if (move.getType() == SpiderMove.Type.RUN) undo();
    }

    private void redo() {
        selectedCard = null;
        if (movesUndone.isEmpty()) return;
        SpiderMove move = movesUndone.removeLast();
        if (!move.execute()) {
            movesUndone.clear();
            return;
        }
        moves.add(move);
        if (!movesUndone.isEmpty() && movesUndone.getLast().getType() == SpiderMove.Type.RUN)
            redo();
    }

    public LinkedList<Card> getRun(LinkedList<Card> input, int afterIndex) {
        LinkedList<Card> subList = new LinkedList<>(input.subList(afterIndex, input.size()));
        if (subList.isEmpty()) return new LinkedList<>();
        LinkedList<Card> currentList = new LinkedList<>();
        int lastRank = subList.getFirst().getRank().toInt() + 1;

        for (Card card : subList) {
            if (card.getRank().toInt() != lastRank - 1) break;
            lastRank = card.getRank().toInt();
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
