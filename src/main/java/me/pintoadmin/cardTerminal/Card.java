package me.pintoadmin.cardTerminal;

import static me.pintoadmin.cardTerminal.Main.*;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import java.io.*;
import java.util.*;

public class Card {
    private final TextGraphics g = Main.g;

    private final UUID uuid;
    private final Rank rank;
    private final Suit suit;

    private boolean faceup = false;

    private int x;
    private int y;

    private TextColor cardTextColor = TextColor.ANSI.WHITE;

    public enum Suit {
        SPADE,
        CLUB,
        HEART,
        DIAMOND;

        public boolean isOppositeColor(Suit suit) {
            return (this == Suit.HEART || this == Suit.DIAMOND)
                            && (suit == Suit.SPADE || suit == Suit.CLUB)
                    || (suit == Suit.HEART || suit == Suit.DIAMOND)
                            && (this == Suit.SPADE || this == Suit.CLUB);
        }

        public boolean isBlack() {
            return this == Suit.SPADE || this == Suit.CLUB;
        }

        public boolean isRed() {
            return this == Suit.DIAMOND || this == Suit.HEART;
        }

        public char toChar() {
            return switch (this) {
                case SPADE -> Main.SPADE;
                case CLUB -> Main.CLUB;
                case HEART -> Main.HEART;
                case DIAMOND -> Main.DIAMOND;
            };
        }
    }

    public enum Rank {
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
        KING,
        JOKER;

        public String toChars() {
            return switch (this) {
                case ACE -> "A";
                case TWO -> "2";
                case THREE -> "3";
                case FOUR -> "4";
                case FIVE -> "5";
                case SIX -> "6";
                case SEVEN -> "7";
                case EIGHT -> "8";
                case NINE -> "9";
                case TEN -> "10";
                case JACK -> "J";
                case QUEEN -> "Q";
                case KING -> "K";
                case JOKER -> "JK";
            };
        }

        public int toInt() {
            return switch (this) {
                case ACE -> 1;
                case TWO -> 2;
                case THREE -> 3;
                case FOUR -> 4;
                case FIVE -> 5;
                case SIX -> 6;
                case SEVEN -> 7;
                case EIGHT -> 8;
                case NINE -> 9;
                case TEN -> 10;
                case JACK -> 11;
                case QUEEN -> 12;
                case KING -> 13;
                case JOKER -> 14;
            };
        }
    }

    public Card(Rank rank, Suit suit) {
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

    public void clear() {
        for (int xi = 0; xi < 9; xi++) {
            for (int yi = 0; yi < 7; yi++) {
                g.setCharacter(x + xi, y + yi, ' ');
            }
        }
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

    private void drawCard(int x, int y, Rank rank, Suit suit) throws IOException {
        g.setBackgroundColor(BLACK);
        g.setForegroundColor(cardTextColor);

        g.fillRectangle(new TerminalPosition(x, y), new TerminalSize(9, 6), ' ');

        g.drawLine(x + 1, y, x + 7, y, '─');
        g.drawLine(x + 1, y + 6, x + 7, y + 6, '─');
        g.drawLine(x, y + 1, x, y + 5, '│');
        g.drawLine(x + 8, y + 1, x + 8, y + 5, '│');

        // Switch corner characters to square for windows (curved corner don't display correctly)
        if (getOS() != OS.WINDOWS) {
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
        if (rank.toChars().length() == 1) {
            g.setCharacter(x + 1, y + 1, rank.toChars().charAt(0));
            g.setCharacter(x + 7, y + 5, rank.toChars().charAt(0));
        } else {
            g.setCharacter(x + 1, y + 1, rank.toChars().charAt(0));
            g.setCharacter(x + 2, y + 1, rank.toChars().charAt(1));
            g.setCharacter(x + 6, y + 5, rank.toChars().charAt(0));
            g.setCharacter(x + 7, y + 5, rank.toChars().charAt(1));
        }

        // Draw card suit
        char cardSuit = suit.toChar();
        g.setCharacter(x + 1, y + 2, cardSuit);
        g.setCharacter(x + 4, y + 3, cardSuit);
        g.setCharacter(x + 7, y + 4, cardSuit);
    }

    private void drawCardBack(int x, int y) throws IOException {
        g.setBackgroundColor(BLACK);
        g.setForegroundColor(cardTextColor);

        g.drawLine(x + 1, y, x + 7, y, '─');
        g.drawLine(x + 1, y + 6, x + 7, y + 6, '─');
        g.drawLine(x, y + 1, x, y + 5, '│');
        g.drawLine(x + 8, y + 1, x + 8, y + 5, '│');

        // Switch corner characters to square for windows (curved corner don't display correctly)
        if (getOS() != OS.WINDOWS) {
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
                g.setCharacter(x + xi, y + yi, '█');
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

    public void setTextColor(TextColor color) {
        cardTextColor = color;
    }

    public Rank getRank() {
        return rank;
    }

    public Suit getSuit() {
        return suit;
    }

    public boolean isFaceup() {
        return faceup;
    }
}
