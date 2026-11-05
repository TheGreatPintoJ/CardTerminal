package me.pintoadmin.cardTerminal.games.spider;

import java.util.*;
import me.pintoadmin.cardTerminal.Card;

public class SpiderMove {
    private final UUID uuid = UUID.randomUUID();
    private final Spider spider;
    private final Card card;
    private final Integer toIndex;
    private int fromIndex;
    private final SpiderMove.Type type;

    enum Type {
        MOVE,
        DEAL
    }

    SpiderMove(Spider spider, Card card, Integer toIndex, Type type) {
        this.spider = spider;
        this.card = card;
        this.toIndex = toIndex;
        this.type = type;
    }

    public boolean execute() {
        boolean res = true;
        switch (type) {
            case MOVE:
                fromIndex =
                        spider.getStock()
                                .indexOf(
                                        spider.getStock().stream()
                                                .filter(l -> l.contains(card))
                                                .toList()
                                                .getFirst());
                LinkedList<Card> fromPile = spider.getStock().get(fromIndex);
                LinkedList<Card> toPile = spider.getStock().get(toIndex);

                if (!toPile.isEmpty()
                        && toPile.getLast().getRank().toInt() != card.getRank().toInt() + 1)
                    return false;

                LinkedList<Card> afterCards =
                        new LinkedList<>(spider.getRun(fromPile, fromPile.indexOf(card) + 1));

                fromPile.remove(card);
                res = toPile.add(card);

                if (!afterCards.isEmpty()) {
                    // moving more than one card
                    if (!fromPile.removeAll(afterCards)) res = false;
                    if (!toPile.addAll(afterCards)) res = false;
                }
                break;
            case DEAL:
                for (int column = 0; column < 10; column++) {
                    Card card = spider.getDeck().drawCard();
                    if (card == null) break;
                    if (!spider.getStock().get(column).add(card)) res = false;
                }
                break;
        }
        return res;
    }

    public void undo() {
        switch (type) {
            case MOVE:
                spider.getStock().get(toIndex).remove(card);
                spider.getStock().get(fromIndex).add(card);
                break;
            case DEAL:
                for (int column = 0; column < 10; column++) {
                    Card card = spider.getDeck().drawCard();
                    if (card == null) break;
                    spider.getStock().get(column).add(card);
                }
                break;
        }
    }
}
