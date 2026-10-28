package me.pintoadmin.cardTerminal;

import static me.pintoadmin.cardTerminal.Main.*;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import java.io.*;
import java.util.*;

public class Card {
    private final TextGraphics g = Main.g;

    private final UUID uuid;
    private final CardRank rank;
    private final CardSuit suit;

    private boolean faceup = false;

    private int x;
    private int y;

    public enum CardSuit {
        SPADE,
        CLUB,
        HEART,
        DIAMOND
    }

    public enum CardRank {
        ACE,
        TWO,
        THREE,
        FOUR,
        FIVE,
        SIX,
        SEVEN,
        EIGHT,
        NINE,
        TEN,
        JACK,
        QUEEN,
        KING
    }

    public Card(CardRank rank, CardSuit suit) {
        uuid = UUID.randomUUID();
        this.rank = rank;
        this.suit = suit;
    }

    public void redraw() {
        draw(x, y, faceup);
    }

    public void draw(int x, int y) {
        draw(x, y, faceup);
    }

    public void draw(int x, int y, boolean faceup) {
        try {
            this.x = x;
            this.y = y;
            this.faceup = faceup;

            if (faceup) drawCard(x, y, rank, suit);
            else drawCardBack(x, y);
        } catch (IOException e) {
            System.err.println("Could not draw card " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void drawCard(int x, int y, Card.CardRank rank, Card.CardSuit suit) throws IOException {
        g.setBackgroundColor(Main.BLACK);
        g.setForegroundColor(TextColor.ANSI.WHITE);

        g.fillRectangle(new TerminalPosition(x, y), new TerminalSize(9, 6), ' ');

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

        // Draw card rank
        if (rankToString(rank).length() == 1) {
            g.setCharacter(x + 1, y + 1, rankToString(rank).charAt(0));
            g.setCharacter(x + 7, y + 5, rankToString(rank).charAt(0));
        } else {
            g.setCharacter(x + 1, y + 1, rankToString(rank).charAt(0));
            g.setCharacter(x + 2, y + 1, rankToString(rank).charAt(1));
            g.setCharacter(x + 6, y + 5, rankToString(rank).charAt(0));
            g.setCharacter(x + 7, y + 5, rankToString(rank).charAt(1));
        }

        // Draw card suit
        char cardSuit = Main.suitToChar(suit);
        g.setCharacter(x + 1, y + 2, cardSuit);
        g.setCharacter(x + 4, y + 3, cardSuit);
        g.setCharacter(x + 7, y + 4, cardSuit);
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

    public void flip(boolean faceup) {
        if (this.faceup != faceup) {
            this.faceup = !this.faceup;
        }
        draw(x, y, faceup);
    }

    public void flip() {
        faceup = !faceup;
        draw(x, y, !faceup);
    }

    public CardRank getRank() {
        return rank;
    }

    public CardSuit getSuit() {
        return suit;
    }

    public boolean isBlack() {
        return suit == CardSuit.SPADE || suit == CardSuit.CLUB;
    }

    public boolean isRed() {
        return suit == CardSuit.DIAMOND || suit == CardSuit.HEART;
    }

    public boolean isFaceup() {
        return faceup;
    }
}
