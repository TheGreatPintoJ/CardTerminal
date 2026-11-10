package me.pintoadmin.cardTerminal;

import static me.pintoadmin.cardTerminal.Main.getOS;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import java.io.*;
import java.util.*;

public class Deck {
    private final TextGraphics g = Main.g;
    protected final List<Card> allCards = new ArrayList<>();
    protected final List<Card> cardsInDeck = new LinkedList<>();
    protected final List<Card> cardsInDiscard = new LinkedList<>();

    private final long SEED;
    private final int deckMultiplier;
    protected Random random;
    private long nextShuffleIndex;

    private int deckX;
    private int deckY;
    private int discardX;
    private int discardY;

    public Deck(long SEED) {
        this(SEED, Arrays.stream(Card.Suit.values()).toList(), 1);
    }

    public Deck(long SEED, int deckMultiplier) {
        this(SEED, Arrays.stream(Card.Suit.values()).toList(), deckMultiplier);
    }

    public Deck(long SEED, List<Card.Suit> suits, int deckMultiplier) {
        this.SEED = SEED;
        this.deckMultiplier = deckMultiplier;
        random = new Random(SEED);
        for (int i = 0; i < deckMultiplier; i++) {
            for (Card.Suit suit : suits) {
                for (Card.Rank rank : Card.Rank.values()) {
                    if (rank == Card.Rank.JOKER) continue;
                    Card card = new Card(rank, suit, i);
                    allCards.add(card);
                    cardsInDeck.add(card);
                }
            }
        }
        Collections.shuffle(cardsInDeck, random);
    }

    public Card drawCard() {
        if (cardsInDeck.isEmpty()) return null;
        Card card = cardsInDeck.getFirst();
        if (!cardsInDeck.remove(card)) return drawCard();
        return card;
    }

    public void addTopDeck(Card card) {
        if (cardsInDeck.contains(card)) return;
        cardsInDeck.addFirst(card);
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

    public void clearDiscard() {
        cardsInDiscard.clear();
    }

    public Card getCard(Card.Rank rank, Card.Suit suit) {
        return getCard(rank, suit, 0);
    }

    public Card getCard(Card.Rank rank, Card.Suit suit, int index) {
        return allCards.stream()
                .filter(
                        c ->
                                c.getRank() == rank
                                        && c.getSuit() == suit
                                        && c.getDeckNumber() == index)
                .findFirst()
                .orElse(null);
    }

    public List<Card> restartDiscard() {
        List<Card> list = List.copyOf(cardsInDiscard);
        cardsInDeck.addAll(cardsInDiscard);
        cardsInDiscard.clear();
        return list;
    }

    public List<Card> shuffleDiscard() {
        List<Card> list = List.copyOf(cardsInDiscard);
        cardsInDeck.addAll(cardsInDiscard);
        cardsInDiscard.clear();

        long index = nextShuffleIndex++;
        Collections.shuffle(cardsInDeck, new Random(SEED ^ index));
        return list;
    }

    private void shuffle() {
        long index = nextShuffleIndex++;
        Collections.shuffle(cardsInDeck, new Random(SEED ^ index));
    }

    public long getNextShuffleIndex() {
        return nextShuffleIndex;
    }

    public void setNextShuffleIndex(long index) {
        nextShuffleIndex = index;
    }

    public List<Card> getAllCards() {
        return allCards;
    }

    public List<Card> getCardsInDeck() {
        return cardsInDeck;
    }

    public void setCardsInDeck(List<Card> cards) {
        cardsInDeck.clear();
        cardsInDeck.addAll(cards);
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
        } catch (IOException e) {
            System.err.println("Could not draw discard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void drawCardBack(int x, int y) throws IOException {
        Card.drawCardBack(x, y, TextColor.ANSI.WHITE);
    }

    private void drawCardSpaceFilled(int x, int y) throws IOException {
        for (int xi = 1; xi < 8; xi++) {
            for (int yi = 1; yi < 6; yi++) {
                g.setBackgroundColor(Main.BLACK);
                g.setForegroundColor(TextColor.ANSI.WHITE);
                g.setCharacter(x + xi, y + yi, ' ');
            }
        }

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

    public int getDeckMultiplier() {
        return deckMultiplier;
    }
}
