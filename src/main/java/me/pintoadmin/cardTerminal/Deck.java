package me.pintoadmin.cardTerminal;

import static me.pintoadmin.cardTerminal.Main.getOS;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import java.io.*;
import java.util.*;

public class Deck {
    private final TextGraphics g = Main.g;
    private final List<Card> allCards = new ArrayList<>();
    private final List<Card> cardsInDeck = new ArrayList<>();
    private final List<Card> cardsInDiscard = new ArrayList<>();

    private long SEED;
    private Random random;

    private int deckX;
    private int deckY;
    private int discardX;
    private int discardY;

    public Deck(long SEED) {
        this.SEED = SEED;
        random = new Random(SEED);
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                if (rank == Card.Rank.JOKER) continue;
                Card card = new Card(rank, suit);
                allCards.add(card);
                cardsInDeck.add(card);
            }
        }
        Collections.shuffle(cardsInDeck, random);
    }

    public Card dealCard() {
        if (cardsInDeck.isEmpty()) return null;
        Card randomCard = cardsInDeck.get(random.nextInt(cardsInDeck.size()));
        if (!cardsInDeck.remove(randomCard)) return drawCard();
        return randomCard;
    }

    public Card drawCard() {
        if (cardsInDeck.isEmpty()) return null;
        Card card = cardsInDeck.getLast();
        if (!cardsInDeck.remove(card)) return drawCard();
        return card;
    }

    public void addTopDeck(Card card) {
        cardsInDeck.addLast(card);
    }

    public void discardCard(Card card) {
        if (cardsInDiscard.contains(card)) return;
        cardsInDiscard.add(card);
        cardsInDeck.remove(card);
    }

    public Card getTopDiscard() {
        if (cardsInDiscard.isEmpty()) return null;
        return cardsInDiscard.getLast();
    }

    public void removeTopDiscard() {
        if (cardsInDiscard.isEmpty()) return;
        cardsInDiscard.removeLast();
    }

    public Card getCard(Card.Rank rank, Card.Suit suit) {
        return allCards.stream()
                .filter(c -> c.getRank() == rank && c.getSuit() == suit)
                .toList()
                .getFirst();
    }

    public void shuffleDiscard() {
        cardsInDeck.addAll(cardsInDiscard);
        cardsInDiscard.clear();

        Collections.shuffle(cardsInDeck, random);
    }

    public void shuffle() {
        Collections.shuffle(cardsInDeck, random);
    }

    public void redrawDeck() {
        drawDeck(deckX, deckY);
    }

    public void drawDeck(int x, int y) {
        deckX = x;
        deckY = y;
        try {
            if (!cardsInDeck.isEmpty()) drawCardBack(x, y);
            else {
                drawCardSpaceFilled(x, y);
                g.setForegroundColor(TextColor.ANSI.WHITE_BRIGHT);
                g.setCharacter(x + 4, y + 3, '⇄');
            }
        } catch (IOException e) {
            System.err.println("Could not draw deck: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void redrawDiscard() {
        drawDiscard(discardX, discardY);
    }

    public void drawDiscard(int x, int y) {
        discardX = x;
        discardY = y;
        try {
            try {
                Card topCard = cardsInDiscard.getLast();
                topCard.draw(x, y, true);
            } catch (NoSuchElementException _) {
                drawCardSpaceFilled(x, y);
            }
            drawCardSpace(x, y);
        } catch (IOException e) {
            System.err.println("Could not draw discard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void drawCardBack(int x, int y) throws IOException {
        g.setBackgroundColor(Main.BLACK);
        g.setForegroundColor(TextColor.ANSI.WHITE);

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

        for (int xi = 1; xi < 8; xi++) {
            for (int yi = 1; yi < 6; yi++) {
                g.setBackgroundColor(TextColor.ANSI.WHITE);
                g.setForegroundColor(Main.BLACK);
                g.setCharacter(x + xi, y + yi, ' ');
                //                g.setCharacter(x + xi, y + yi, xi + yi % 2 == 0 ? '░' : '▒');
            }
        }
    }

    private void drawCardSpaceFilled(int x, int y) throws IOException {
        for (int xi = 1; xi < 8; xi++) {
            for (int yi = 1; yi < 6; yi++) {
                g.setBackgroundColor(Main.BLACK);
                g.setForegroundColor(TextColor.ANSI.WHITE);
                g.setCharacter(x + xi, y + yi, ' ');
            }
        }
        drawCardSpace(x, y);
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
}
