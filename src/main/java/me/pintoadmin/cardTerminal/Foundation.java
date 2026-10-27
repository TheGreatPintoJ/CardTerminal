package me.pintoadmin.cardTerminal;

import static me.pintoadmin.cardTerminal.Card.CardSuit.*;
import static me.pintoadmin.cardTerminal.Main.getOS;
import static me.pintoadmin.cardTerminal.Main.rankToInt;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import java.io.*;
import java.util.*;

public class Foundation {
    private final TextGraphics g = Main.g;
    private final List<Card> cards = new ArrayList<>();
    private final Map<Card.CardSuit, Integer> piles =
            new HashMap<>(Map.of(HEART, 0, SPADE, 0, DIAMOND, 0, CLUB, 0));

    public Foundation() {}

    public void draw(int x, int y) {
        try {
            drawCardOutline(x, y);
            drawCardOutline(x + 9, y);
            drawCardOutline(x + 18, y);
            drawCardOutline(x + 27, y);
        } catch (IOException e) {
            System.err.println("Could not draw foundation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void drawCardOutline(int x, int y) throws IOException {
        g.setBackgroundColor(TextColor.ANSI.BLACK);
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
        if (rankToInt(card.getRank()) - 1 == piles.get(card.getSuit())) {
            piles.compute(card.getSuit(), (_, v) -> v + 1);
            return true;
        }
        return false;
    }
}
