package me.pintoadmin.cardTerminal.games.solitaire;

import static me.pintoadmin.cardTerminal.Main.*;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import java.io.*;
import java.util.*;
import java.util.function.*;
import me.pintoadmin.cardTerminal.*;
import me.pintoadmin.cardTerminal.screens.MainMenu;

public class Solitaire extends Game {
    private final GameType type;
    private final long SEED;
    private final Deck deck;
    private final Foundation foundation;
    private final LinkedList<LinkedList<Card>> stock = new LinkedList<>(); // 2d, column:row:card
    private final LinkedList<SolitaireMove> moves = new LinkedList<>();
    private final LinkedList<SolitaireMove> movesUndone = new LinkedList<>();

    private final Supplier<Integer> WIDTH = () -> g.getSize().getColumns();
    private final Supplier<Integer> HEIGHT = () -> g.getSize().getRows();

    private final int stockWidth = 7; // Seven for default solitaire, hopefully configurable later

    private final Supplier<Integer> deckX = () -> 0;
    private final Supplier<Integer> deckY = () -> HEIGHT.get() - 14;

    private int selectedColumn = -1;

    private Card selectedCard;
    private final Supplier<Location> selectedCardLocation;

    private int shuffleCounter = 0;

    public enum GameType {
        KLONDIKE1,
        KLONDIKE3,
        VEGAS1,
        VEGAS3,
    }

    enum Location {
        DECK,
        DISCARD,
        STOCK,
        FOUNDATION
    }

    public Solitaire() {
        this(GameType.KLONDIKE1, new Random().nextLong());
    }

    public Solitaire(GameType type, long SEED) {
        this.type = type;
        this.SEED = SEED;
        this.deck = new Deck(SEED);
        this.foundation = new Foundation(deck);
        this.selectedCardLocation =
                () ->
                        stock.stream().map(l -> l.contains(selectedCard)).toList().contains(true)
                                ? Location.STOCK
                                : selectedCard == deck.getTopDiscard()
                                        ? Location.DISCARD
                                        : Location.FOUNDATION;

        screen.clear();

        System.out.println("Solitaire game seed: " + SEED);
        for (int column = 0; column < stockWidth; column++) {
            LinkedList<Card> thisColumn = new LinkedList<>();
            for (int row = 0; row <= column; row++) {
                Card card = deck.drawCard();
                if (card == null) continue;
                thisColumn.add(card);
            }
            stock.add(thisColumn);
        }

        drawGame();
        try {
            screen.refresh(Screen.RefreshType.AUTOMATIC);
        } catch (IOException e) {
            System.out.println("Ignored exception: " + e.getMessage());
        }
    }

    private Solitaire(GameType type, long SEED, LinkedList<SolitaireMove> moves) {
        this(type, SEED);
        try {
            screen.refresh(Screen.RefreshType.COMPLETE);
        } catch (IOException e) {
            System.out.println("Ignored exception: " + e.getMessage());
        }

        LinkedList<SolitaireMove> newMoves = new LinkedList<>();
        for (SolitaireMove move : moves) {
            SolitaireMove newMove =
                    new SolitaireMove(
                            this,
                            move.getCard() != null
                                    ? deck.getCard(
                                            move.getCard().getRank(), move.getCard().getSuit())
                                    : null,
                            move.getFromLocation(),
                            move.getToLocation(),
                            move.getToIndex(),
                            move.getType());
            newMove.setOldDiscard(
                    move.getOldDiscard().stream()
                            .map(c -> deck.getCard(c.getRank(), c.getSuit()))
                            .toList());
            newMove.setShuffled(
                    move.getShuffled().stream()
                            .map(c -> deck.getCard(c.getRank(), c.getSuit()))
                            .toList());
            newMove.setShuffleIndex(move.getShuffleIndex());
            newMoves.add(newMove);
        }

        for (SolitaireMove move : new LinkedList<>(newMoves)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                System.out.println("Ignored exception: " + e.getMessage());
            }
            move.execute();
            try {
                screen.refresh(Screen.RefreshType.DELTA);
            } catch (IOException e) {
                System.out.println("Ignored exception: " + e.getMessage());
            }
        }
    }

    @Override
    public Game newGame() {
        return new Solitaire();
    }

    @Override
    public Game restart() {
        return new Solitaire(type, SEED);
    }

    @Override
    public long getSeed() {
        return SEED;
    }

    @Override
    public void resize() {
        Main.screen.clear();
        drawGame();
    }

    //    @Override
    //    public void redraw() {
    //        Main.screen.clear();
    //        deck.redrawDeck();
    //        deck.redrawDiscard();
    //        foundation.redraw();
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

    private boolean nextDrawShuffles = false;

    @Override
    public void drawCard(boolean logged) {
        int drawCt = type.name().contains("3") ? 3 : 1;
        if (nextDrawShuffles) {
            if (type.name().contains("VEGAS") && shuffleCounter >= drawCt - 1) return;
            SolitaireMove move =
                    new SolitaireMove(
                            this,
                            deck.getTopDiscard(),
                            Location.DISCARD,
                            Location.DECK,
                            -1,
                            SolitaireMove.Type.SHUFFLE);
            move.setOldDiscard(deck.restartDiscard());
            move.setShuffled(deck.getCardsInDeck());
            moves.add(move);

            deck.redrawDiscard();
            deck.redrawDeck();

            nextDrawShuffles = false;
            shuffleCounter++;
            return;
        }

        for (int i = 0; i < (drawCt); i++) {
            Card drawnCard = deck.drawCard();
            if (drawnCard == null) {
                // Deck has run out
                if (logged && i == 0) {
                    SolitaireMove move =
                            new SolitaireMove(
                                    this,
                                    deck.getTopDiscard(),
                                    Location.DECK,
                                    Location.DISCARD,
                                    -1,
                                    SolitaireMove.Type.EXHAUST);
                    moves.add(move);
                }
                deck.redrawDeck();
                nextDrawShuffles = true;
                return;
            } else if (drawnCard != null) {
                deck.discardCard(drawnCard);
                if (logged && i == 0) {
                    SolitaireMove move =
                            new SolitaireMove(
                                    this,
                                    drawnCard,
                                    Location.DECK,
                                    Location.DISCARD,
                                    -1,
                                    SolitaireMove.Type.DRAW);
                    moves.add(move);
                }
            }
        }
        deck.redrawDiscard();
    }

    void drawGame() {
        Arrays.stream(SGR.values()).toList().forEach(g::disableModifiers);
        g.setBackgroundColor(BLACK);
        g.setForegroundColor(TextColor.ANSI.WHITE);
        drawTextRight(WIDTH.get() - 1, HEIGHT.get() - 1, "SEED: " + SEED);
        deck.drawDeck(deckX.get(), deckY.get());
        deck.drawDiscard(deckX.get() + 9, deckY.get());

        int startingX = g.getSize().getColumns() - (9 * stockWidth);
        int startingY = 0;

        for (int column = 0; column < stockWidth; column++) {
            List<Card> thisColumn = stock.get(column);
            if (thisColumn.isEmpty()) {
                try {
                    Card.drawCardSpace(startingX + (column) * 9, startingY);
                } catch (IOException _) {
                }
                continue;
            }
            for (Card card : stock.get(column)) {
                card.setTextColor(TextColor.ANSI.WHITE);
                if (column == selectedColumn) {
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
                card.draw(startingX + (column) * 9, startingY + thisColumn.indexOf(card) * 3);
            }
        }

        for (List<Card> list : stock) {
            if (list.isEmpty())
                try {
                    Card.drawCardSpace(startingX + (stock.indexOf(list)) * 9, startingY);
                } catch (IOException _) {
                }
            else list.getLast().flip(true);
        }

        int foundationX = 0;
        int foundationY = HEIGHT.get() - 7;

        foundation.draw(foundationX, foundationY);
    }

    /*
    Escape: close
    Backtick: draw card
    Space: select drawn card
    Tab: select discarded card
    1,2,3,4,5,6,7: select column
        1-9,0: select row (card)
    qwer: select foundation
    -: deselect all
    [/]: undo/redo
     */
    @Override
    public Action handleInput(KeyStroke keyStroke) {
        // if (checkHandleWin()) return;
        if (keyStroke.getKeyType() == KeyType.Escape) {
            Main.getInstance().setCurrentScreen(new MainMenu());
            return Action.BACK_TO_MENU;
        } else if (keyStroke.getKeyType() == KeyType.Character) {
            char key = keyStroke.getCharacter();
            try {
                if (key == '-') {
                    clearSelected();
                    resize();
                }
                int intKey = Integer.parseInt(String.valueOf(key)) - 1;
                if (selectedCard != null) {
                    moveSelected(Location.STOCK, intKey);
                } else {
                    if (intKey < stockWidth && selectedColumn == -1) {
                        selectedColumn = intKey;
                        List<Card> faceupCards =
                                stock.get(selectedColumn).stream().filter(Card::isFaceup).toList();
                        if (faceupCards.size() == 1) {
                            selectedCard = faceupCards.getFirst();
                        }
                    } else if (intKey <= stock.get(selectedColumn).size()) {
                        List<Card> faceupCards =
                                stock.get(selectedColumn).stream().filter(Card::isFaceup).toList();
                        if (intKey == -1) {
                            if (!faceupCards.isEmpty()) selectedCard = faceupCards.getLast();
                        } else selectedCard = faceupCards.get(intKey);
                    }
                }
            } catch (NumberFormatException | IndexOutOfBoundsException _) {
            }
            switch (key) {
                case '`':
                    clearSelected();
                    drawCard(true);
                    movesUndone.clear();
                    break;
                case ' ':
                    // Select drawn card
                    if (selectedCard == deck.getTopDiscard()) clearSelected();
                    else selectCard(Location.DECK, 0);
                    break;
                case '[':
                    // Undo
                    undo();
                    break;
                case ']':
                    // Redo
                    redo();
                    break;
                case 'q':
                    // Foundation 1
                    if (selectedCard == null) selectCard(Location.FOUNDATION, 0);
                    else moveSelected(Location.FOUNDATION, -1);
                    break;
                case 'w':
                    // Foundation 2
                    if (selectedCard == null) selectCard(Location.FOUNDATION, 1);
                    else moveSelected(Location.FOUNDATION, -1);
                    break;
                case 'e':
                    // Foundation 3
                    if (selectedCard == null) selectCard(Location.FOUNDATION, 2);
                    else moveSelected(Location.FOUNDATION, -1);
                    break;
                case 'r':
                    // Foundation 4
                    if (selectedCard == null) selectCard(Location.FOUNDATION, 3);
                    else moveSelected(Location.FOUNDATION, -1);
                    break;
            }
        } else if (keyStroke.getKeyType() == KeyType.F12) {
            // Reset game
            return Action.RESTART;
        } else if (keyStroke.getKeyType() == KeyType.F10) {
            // New game
            return Action.NEW_GAME;
        }
        drawGame();
        try {
            screen.refresh(Screen.RefreshType.DELTA);
            checkHandleWin();
        } catch (IOException e) {
            System.out.println("Ignored exception: " + e.getMessage());
        }
        return Action.NONE;
    }

    private void selectCard(Location loc, int index) {
        if (loc == null || index == -1) {
            selectedCard = null;
            selectedColumn = -1;
            return;
        }
        if (selectedCard != null) selectedCard.setTextColor(TextColor.ANSI.WHITE);
        switch (loc) {
            case DECK, DISCARD:
                selectedCard = deck.getTopDiscard();
                break;
            case FOUNDATION:
                Card.Suit suit = Card.Suit.HEART;
                if (index == 1) suit = Card.Suit.SPADE;
                else if (index == 2) suit = Card.Suit.DIAMOND;
                else if (index == 3) suit = Card.Suit.CLUB;
                selectedCard = foundation.getTopCard(suit);
                break;
        }
        if (selectedCard != null)
            selectedCard.setTextColor(
                    selectedCard.isFaceup() ? TextColor.ANSI.GREEN : TextColor.ANSI.RED);
    }

    private boolean checkHandleWin() throws IOException {
        if (foundation.checkWin()) { // || true for testing
            // Player won
            clearSelected();
            win();
            new Solitaire(type, SEED, moves);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Ignored exception: " + e.getMessage());
            }
            Main.getInstance().setCurrentScreen(new MainMenu());

            return true;
        }
        return false;
    }

    private void moveSelected(Location toLocation, int index) {
        selectedCard.setTextColor(TextColor.ANSI.WHITE);
        SolitaireMove move =
                new SolitaireMove(
                        this,
                        selectedCard,
                        selectedCardLocation.get(),
                        toLocation,
                        index,
                        SolitaireMove.Type.MOVE);
        if (move.execute()) {
            moves.add(move);
            movesUndone.clear();
        }

        clearSelected();
    }

    private void undo() {
        if (moves.isEmpty()) return;
        SolitaireMove move = moves.getLast();
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

    private void clearSelected() {
        selectedColumn = -1;
        if (selectedCard != null) selectedCard.setTextColor(TextColor.ANSI.WHITE);
        selectedCard = null;
    }

    void setNextShuffle(boolean state) {
        nextDrawShuffles = state;
    }

    public Foundation getFoundation() {
        return foundation;
    }

    public Deck getDeck() {
        return deck;
    }

    public LinkedList<LinkedList<Card>> getStock() {
        return stock;
    }
}
