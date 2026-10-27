package me.pintoadmin.cardTerminal;

import static me.pintoadmin.cardTerminal.Main.getOS;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import java.io.*;
import java.util.*;

public class Deck {
    private final TextGraphics g = Main.g;
    private final List<Card> cardsInDeck = new ArrayList<>();

    public Deck() {
        for (Card.CardSuit suit : Card.CardSuit.values()) {
            for (Card.CardRank rank : Card.CardRank.values()) {
                cardsInDeck.add(new Card(rank, suit));
            }
        }
    }

    public Card drawCard() {
        if (cardsInDeck.isEmpty()) return null;
        Random random = new Random();
        Card randomCard = cardsInDeck.get(random.nextInt(cardsInDeck.size()));
        if (!cardsInDeck.remove(randomCard)) return drawCard();
        return randomCard;
    }

    public void drawDeck(int x, int y) {
        try {
            drawCardBack(x, y);
        } catch (IOException e) {
            System.err.println("Could not draw deck: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void drawCardBack(int x, int y) throws IOException {
        g.setBackgroundColor(TextColor.ANSI.BLACK);
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
                g.setForegroundColor(TextColor.ANSI.BLACK);
                g.setCharacter(x + xi, y + yi, ' ');
                //                g.setCharacter(x + xi, y + yi, xi + yi % 2 == 0 ? '░' : '▒');
            }
        }
    }
}
