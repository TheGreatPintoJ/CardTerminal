package me.pintoadmin.cardTerminal.games;

import static me.pintoadmin.cardTerminal.Main.*;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import java.io.*;
import java.util.*;
import java.util.function.*;
import me.pintoadmin.cardTerminal.*;

public class Solitaire extends Game {
    private final long SEED;
    private final Deck deck;
    private final Foundation foundation;
    private final LinkedList<LinkedList<Card>> stock = new LinkedList<>(); // 2d, column:row:card
    private final LinkedList<SolitaireMove> moves = new LinkedList<>();
    private final LinkedList<SolitaireMove> movesUndone = new LinkedList<>();
    private Card drawnCard;

    private final Supplier<Integer> WIDTH = () -> g.getSize().getColumns();
    private final Supplier<Integer> HEIGHT = () -> g.getSize().getRows();

    private final int stockWidth = 7; // Seven for default solitaire, hopefully configurable later

    private final Supplier<Integer> deckX = () -> 0;
    private final Supplier<Integer> deckY = () -> HEIGHT.get() - 14;

    private int selectedColumn = -1;

    private Card selectedCard;
    private final Supplier<Location> selectedCardLocation;

    enum Location {
        DECK,
        DISCARD,
        STOCK,
        FOUNDATION
    }

    public Solitaire() {
        this(new Random().nextLong());
    }

    public Solitaire(long SEED) {
        this.SEED = SEED;
        this.deck = new Deck(SEED);
        this.foundation = new Foundation(deck);
        this.selectedCardLocation =
                () ->
                        stock.stream().map(l -> l.contains(selectedCard)).toList().contains(true)
                                ? Location.STOCK
                                : selectedCard == drawnCard
                                        ? Location.DECK
                                        : selectedCard == deck.getTopDiscard()
                                                ? Location.DISCARD
                                                : Location.FOUNDATION;
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

    @Override
    public void redraw() {
        Main.screen.clear();
        deck.redrawDeck();
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

        foundation.redraw();
        if (drawnCard != null) drawnCard.redraw();
    }

    private boolean nextDrawShuffles = false;

    @Override
    public void drawCard(boolean logged) {
        if (nextDrawShuffles) {
            SolitaireMove move =
                    new SolitaireMove(
                            this,
                            drawnCard,
                            Location.DISCARD,
                            Location.DECK,
                            -1,
                            SolitaireMove.Type.SHUFFLE);
            move.setShuffleIndex(deck.getNextShuffleIndex());
            move.setOldDiscard(deck.shuffleDiscard());
            move.setShuffled(deck.getAllCards());
            moves.add(move);

            deck.redrawDiscard();
            deck.redrawDeck();

            nextDrawShuffles = false;
            return;
        }
        if (drawnCard != null) {
            if (!drawnCard.isFaceup()) {
                drawnCard.draw(deckX.get(), deckY.get(), true);
                if (logged) {
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
                return;
            }
            deck.discardCard(drawnCard);
        }
        deck.redrawDiscard();

        drawnCard = deck.drawCard();
        if (drawnCard == null) {
            // Deck has run out
            if (logged) {
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
        }
        if (logged) {
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
        drawnCard.draw(deckX.get(), deckY.get(), true);
    }

    void drawGame() {
        deck.drawDeck(deckX.get(), deckY.get());
        deck.drawDiscard(deckX.get() + 9, deckY.get());

        int startingX = g.getSize().getColumns() - (9 * stockWidth);
        int startingY = 0;

        for (int column = 0; column < stockWidth; column++) {
            List<Card> thisColumn = stock.get(column);
            if (thisColumn.isEmpty()) {
                try {
                    drawCardSpace(startingX + (column) * 9, startingY);
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
                    drawCardSpace(startingX + (stock.indexOf(list)) * 9, startingY);
                } catch (IOException _) {
                }
            else list.getLast().flip(true);
        }

        int foundationX = 0;
        int foundationY = HEIGHT.get() - 7;

        foundation.draw(foundationX, foundationY);

        if (drawnCard != null) drawnCard.draw(deckX.get(), deckY.get());
        else deck.redrawDeck();
    }

    /*
    Escape: close
    Backtick: draw card
    Space: select drawn card
    Tab: select discarded card
    1,2,3,4,5,6,7: select column
        1-9,0: select row (card)
    -: deselect all
    [/]: undo/redo
     */
    @Override
    public void handleInput(KeyStroke keyStroke) throws IOException {
        if (checkHandleWin()) return;
        if (keyStroke.getKeyType() == KeyType.Character) {
            char key = keyStroke.getCharacter();
            try {
                if (key == '-') {
                    clearSelected();
                    redraw();
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
                            selectedCard = faceupCards.getLast();
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
                    if (selectedCard == drawnCard) clearSelected();
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
            }
        } else if (keyStroke.getKeyType() == KeyType.Tab) {
            // Top discard
            if (deck.getTopDiscard() != null) selectCard(Location.DISCARD, 0);
        } else if (keyStroke.getKeyType() == KeyType.F1) {
            // Foundation 1
            if (selectedCard == null) selectCard(Location.FOUNDATION, 0);
            else moveSelected(Location.FOUNDATION, -1);
        } else if (keyStroke.getKeyType() == KeyType.F2) {
            // Foundation 2
            if (selectedCard == null) selectCard(Location.FOUNDATION, 1);
            else moveSelected(Location.FOUNDATION, -1);
        } else if (keyStroke.getKeyType() == KeyType.F3) {
            // Foundation 3
            if (selectedCard == null) selectCard(Location.FOUNDATION, 2);
            else moveSelected(Location.FOUNDATION, -1);
        } else if (keyStroke.getKeyType() == KeyType.F4) {
            // Foundation 4
            if (selectedCard == null) selectCard(Location.FOUNDATION, 3);
            else moveSelected(Location.FOUNDATION, -1);
        }
        drawGame();
        screen.refresh(Screen.RefreshType.DELTA);
        checkHandleWin();
    }

    private void selectCard(Location loc, int index) {
        if (loc == null || index == -1) {
            selectedCard = null;
            selectedColumn = -1;
            return;
        }
        if (selectedCard != null) selectedCard.setTextColor(TextColor.ANSI.WHITE);
        switch (loc) {
            case DECK:
                selectedCard = drawnCard;
                break;
            case DISCARD:
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
            screen.clear();
            clearSelected();
            setDrawnCard(null);

            g.enableModifiers(SGR.BLINK);
            g.setBackgroundColor(BLACK);
            g.setForegroundColor(TextColor.ANSI.WHITE);
            // @formatter:off
            Main.drawText(WIDTH.get()/2, HEIGHT.get()/2-2, "▄▄▄   ▄▄▄   ▄▄▄▄▄   ▄▄▄  ▄▄▄   ▄▄▄▄  ▄▄▄  ▄▄▄▄ ▄▄▄▄▄ ▄▄▄    ▄▄▄");
            Main.drawText(WIDTH.get()/2, HEIGHT.get()/2-1, "███   ███ ▄███████▄ ███  ███   ▀███  ███  ███▀  ███  ████▄  ███");
            Main.drawText(WIDTH.get()/2, HEIGHT.get()/2,   "▀███▄███▀ ███   ███ ███  ███    ███  ███  ███   ███  ███▀██▄███");
            Main.drawText(WIDTH.get()/2, HEIGHT.get()/2+1, "  ▀███▀   ███▄▄▄███ ███▄▄███    ███▄▄███▄▄███   ███  ███  ▀████");
            Main.drawText(WIDTH.get()/2, HEIGHT.get()/2+2, "   ███     ▀█████▀  ▀██████▀     ▀████▀████▀   ▄███▄ ███    ███");
            // @formatter:on
            terminal.bell();
            g.disableModifiers(SGR.BLINK);
            screen.refresh(Screen.RefreshType.COMPLETE);
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
        moves.getLast().undo();
        movesUndone.add(moves.getLast());
        moves.removeLast();
    }

    private void redo() {
        if (movesUndone.isEmpty()) return;
        movesUndone.getLast().execute();
        moves.add(movesUndone.getLast());
        movesUndone.removeLast();
    }

    private void drawCardSpace(int x, int y) throws IOException {
        g.setBackgroundColor(Main.BLACK);
        g.setForegroundColor(new TextColor.RGB(50, 50, 50));

        g.drawLine(x + 1, y, x + 7, y, '─');
        g.drawLine(x + 1, y + 6, x + 7, y + 6, '─');
        g.drawLine(x, y + 1, x, y + 5, '│');
        g.drawLine(x + 8, y + 1, x + 8, y + 5, '│');

        // Switch corner characters to square for windows (curved corner don't display correctly)
        if (getOS() != Main.OS.WINDOWS) {
            g.setCharacter(x, y, '╭');
            g.setCharacter(x + 8, y, '╮');
            g.setCharacter(x, y + 6, '╰');
            g.setCharacter(x + 8, y + 6, '╯');
        } else {
            g.setCharacter(x, y, '┌');
            g.setCharacter(x + 8, y, '┐');
            g.setCharacter(x, y + 6, '└');
            g.setCharacter(x + 8, y + 6, '┘');
        }
    }

    private void clearSelected() {
        selectedColumn = -1;
        if (selectedCard != null) selectedCard.setTextColor(TextColor.ANSI.WHITE);
        selectedCard = null;
    }

    void nullDrawnCard() {
        drawnCard = null;
    }

    void setDrawnCard(Card card) {
        drawnCard = card;
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
