package me.pintoadmin.cardTerminal;

import java.awt.*;
import java.io.*;
import java.util.*;

public class Card {
    private final UUID uuid;
    private final Main.CardNumber number;
    private final Main.CardSuit suit;

    private boolean flipped = false;

    private int x;
    private int y;

    public Card(Main.CardNumber number, Main.CardSuit suit) {
        uuid = UUID.randomUUID();
        this.number = number;
        this.suit = suit;
    }

    public void draw(int x, int y, boolean flipped) {
        try {
            this.x = x;
            this.y = y;

            if (flipped) Main.drawCard(x, y, number, suit);
            else Main.drawCardBack(x, y);
        } catch (IOException e) {
            System.err.println("Could not draw card " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void flip() {
        draw(x, y, !flipped);
    }

    public Main.CardNumber getNumber() {
        return number;
    }

    public Main.CardSuit getSuit() {
        return suit;
    }

    public boolean isFlipped() {
        return flipped;
    }
}
