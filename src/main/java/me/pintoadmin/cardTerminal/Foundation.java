package me.pintoadmin.cardTerminal;

import static me.pintoadmin.cardTerminal.Card.Suit.*;
import static me.pintoadmin.cardTerminal.Main.getOS;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import java.io.*;
import java.util.*;

public class Foundation {
    private final TextGraphics g = Main.g;
    private final List<Card> cards = new ArrayList<>();
    private final Map<Card.Suit, Integer> piles =
            new HashMap<>(Map.of(HEART, 0, SPADE, 0, DIAMOND, 0, CLUB, 0));
    private final Deck deck;

    private int x;
    private int y;

    public Foundation(Deck deck) {
        this.deck = deck;
    }

    public void redraw() {
        draw(x, y);
    }

    public void draw(int x, int y) {
        this.x = x;
        this.y = y;
        try {
            if (piles.get(HEART) == 0) drawCardSpace(x, y, HEART);
            else deck.getCard(Main.intToRank(piles.get(HEART)), HEART).draw(x, y);
            if (piles.get(SPADE) == 0) drawCardSpace(x + 9, y, SPADE);
            else deck.getCard(Main.intToRank(piles.get(SPADE)), SPADE).draw(x + 9, y);
            if (piles.get(DIAMOND) == 0) drawCardSpace(x + 18, y, DIAMOND);
            else deck.getCard(Main.intToRank(piles.get(DIAMOND)), DIAMOND).draw(x + 18, y);
            if (piles.get(CLUB) == 0) drawCardSpace(x + 27, y, CLUB);
            else deck.getCard(Main.intToRank(piles.get(CLUB)), CLUB).draw(x + 27, y);
        } catch (IOException e) {
            System.err.println("Could not draw foundation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void clear() {
        try {
            drawCardSpace(x, y, HEART);
            drawCardSpace(x + 9, y, SPADE);
            drawCardSpace(x + 18, y, DIAMOND);
            drawCardSpace(x + 27, y, CLUB);
        } catch (IOException e) {
            System.err.println("Could not clear foundation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void drawCardSpace(int x, int y, Card.Suit suit) throws IOException {
        drawCardSpace(x, y);
        g.setCharacter(x + 4, y + 3, suit.toChar());
    }

    private void drawCardSpace(int x, int y) {
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

    public boolean addCard(Card card) {
        if (cards.contains(card)) return false;
        if (card.getRank().toInt() - 1 == piles.get(card.getSuit())) {
            piles.compute(card.getSuit(), (_, v) -> v + 1);
            cards.add(card);
            return true;
        }
        return false;
    }

    public boolean removeCard(Card card) {
        if (!cards.contains(card)) return false;
        if (card.getRank().toInt() == piles.get(card.getSuit())) {
            piles.compute(card.getSuit(), (_, v) -> v - 1);
            return true;
        }
        return false;
    }

    public Card getTopCard(Card.Suit suit) {
        Card.Rank rank = Main.intToRank(piles.get(suit));
        if (rank == null) return null;
        return deck.getCard(rank, suit);
    }
}
