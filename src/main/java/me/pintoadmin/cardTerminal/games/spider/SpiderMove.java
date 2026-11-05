package me.pintoadmin.cardTerminal.games.spider;

import java.util.LinkedList;
import java.util.UUID;
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
                int i = 0;
                for (LinkedList<Card> list : spider.getStock()) {
                    if (list.remove(card)) fromIndex = i;
                    i++;
                }
                res = spider.getStock().get(toIndex).add(card);
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
