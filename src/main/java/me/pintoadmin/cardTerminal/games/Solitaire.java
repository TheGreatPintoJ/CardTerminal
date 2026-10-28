package me.pintoadmin.cardTerminal.games;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import java.io.*;
import java.util.*;
import java.util.function.*;
import me.pintoadmin.cardTerminal.*;

public class Solitaire extends Game {
    private final long SEED = new Random().nextLong();
    private final Deck deck = new Deck(SEED);
    private final Foundation foundation = new Foundation(deck);
    private final List<List<Card>> cards = new ArrayList<>(); // 2d, column:row:card
    private final LinkedList<SolitaireMove> moves = new LinkedList<>();
    private final LinkedList<SolitaireMove> movesUndone = new LinkedList<>();
    private Card drawnCard;

    private final Supplier<Integer> WIDTH = () -> Main.g.getSize().getColumns();
    private final Supplier<Integer> HEIGHT = () -> Main.g.getSize().getRows();

    private final int stockWidth = 7; // Seven for default solitaire, hopefully configurable later

    private final Supplier<Integer> deckX = () -> 0;
    private final Supplier<Integer> deckY = () -> HEIGHT.get() - 14;

    private int selectedColumn = -1;

    private Card selectedCard;

    enum Location {
        DECK,
        DISCARD,
        STOCK,
        FOUNDATION
    }

    public Solitaire() {
        System.out.println("Solitaire game seed: " + SEED);
        for (int column = 0; column < stockWidth; column++) {
            List<Card> thisColumn = new ArrayList<>();
            for (int row = 0; row <= column; row++) {
                Card card = deck.drawCard();
                if (card == null) continue;
                thisColumn.add(card);
            }
            cards.add(thisColumn);
        }
        drawnCard = deck.drawCard();

        drawGame();
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

        for (List<Card> list : cards) {
            for (Card card : list) {
                card.redraw();
            }
        }

        for (List<Card> list : cards) {
            list.getLast().flip(true);
        }

        foundation.redraw();
        drawnCard.redraw();
    }

    private boolean nextDrawShuffles = false;

    @Override
    public void drawCard() {
        selectCard(null, -1);
        if (nextDrawShuffles) {
            deck.shuffle();
            deck.redrawDiscard();
            deck.redrawDeck();

            drawnCard = deck.drawCard();
            drawnCard.draw(deckX.get(), deckY.get(), false);

            nextDrawShuffles = false;
            return;
        }
        if (!drawnCard.isFaceup()) {
            drawnCard.draw(deckX.get(), deckY.get(), true);
            return;
        }
        deck.discardCard(drawnCard);
        deck.redrawDiscard();
        drawnCard = deck.drawCard();
        if (drawnCard == null) {
            // Deck has run out
            deck.redrawDeck();
            nextDrawShuffles = true;
            return;
        }
        drawnCard.draw(deckX.get(), deckY.get(), true);
    }

    void drawGame() {
        deck.drawDeck(deckX.get(), deckY.get());
        deck.drawDiscard(deckX.get() + 9, deckY.get());

        int startingX = Main.g.getSize().getColumns() - (9 * stockWidth);
        int startingY = 0;

        for (int column = 0; column < stockWidth; column++) {
            int ci = 0;
            for (Card card : cards.get(column)) {
                card.setTextColor(TextColor.ANSI.WHITE);
                if (column == selectedColumn)
                    if (selectedCard == null || card == selectedCard)
                        card.setTextColor(
                                card.isFaceup() ? TextColor.ANSI.GREEN : TextColor.ANSI.RED);
                card.draw(startingX + (column) * 9, startingY + ci * 3);
                ci++;
            }
        }

        for (List<Card> list : cards) {
            list.getLast().flip(true);
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
    [/]: undo/redo
     */
    @Override
    public void handleInput(KeyStroke keyStroke) throws IOException {
        if (keyStroke.getKeyType() == KeyType.Character) {
            char key = keyStroke.getCharacter();
            try {
                int intKey = Integer.parseInt(String.valueOf(key));
                if (intKey < stockWidth && selectedColumn == -1) {
                    selectedColumn = intKey;
                } else if (intKey < cards.get(selectedColumn).size()) {
                    selectedCard = cards.get(selectedColumn).get(intKey);
                }
            } catch (NumberFormatException | IndexOutOfBoundsException _) {
            }
            switch (key) {
                case '`':
                    drawCard();
                    break;
                case ' ':
                    // Select drawn card
                    selectCard(Location.DECK, 0);
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
            selectCard(Location.DISCARD, 0);
        } else if (keyStroke.getKeyType() == KeyType.F1) {
            // Foundation 1
            if (selectedCard == null) selectCard(Location.FOUNDATION, 0);
            else moveSelected(Location.FOUNDATION, 0);
        } else if (keyStroke.getKeyType() == KeyType.F2) {
            // Foundation 2
            if (selectedCard == null) selectCard(Location.FOUNDATION, 1);
            else moveSelected(Location.FOUNDATION, 1);
        } else if (keyStroke.getKeyType() == KeyType.F3) {
            // Foundation 3
            if (selectedCard == null) selectCard(Location.FOUNDATION, 2);
            else moveSelected(Location.FOUNDATION, 2);
        } else if (keyStroke.getKeyType() == KeyType.F4) {
            // Foundation 4
            if (selectedCard == null) selectCard(Location.FOUNDATION, 3);
            else moveSelected(Location.FOUNDATION, 3);
        }
        drawGame();
        Main.screen.refresh(Screen.RefreshType.DELTA);
    }

    private void selectCard(int column, int row) {
        selectedCard = cards.get(column).get(row);
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
                Card.CardSuit suit = Card.CardSuit.HEART;
                if (index == 1) suit = Card.CardSuit.SPADE;
                else if (index == 2) suit = Card.CardSuit.DIAMOND;
                else if (index == 3) suit = Card.CardSuit.CLUB;
                selectedCard = foundation.getTopCard(suit);
                break;
        }
        if (selectedCard != null)
            selectedCard.setTextColor(
                    selectedCard.isFaceup() ? TextColor.ANSI.GREEN : TextColor.ANSI.RED);
    }

    private void moveSelected(Location toLocation, int index) {
        moves.add(new SolitaireMove(this, selectedCard, toLocation, index));
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

    public Foundation getFoundation() {
        return foundation;
    }

    public Deck getDeck() {
        return deck;
    }

    public List<List<Card>> getStock() {
        return cards;
    }

    public int getStockWidth() {
        return stockWidth;
    }
}
