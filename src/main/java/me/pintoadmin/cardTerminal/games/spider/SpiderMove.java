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
    private LinkedList<Card> runCards = new LinkedList<>();
    private Card revealedCard; // face-down card this move uncovered, or null

    enum Type {
        MOVE,
        DEAL,
        RUN
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
                        && (toPile.getLast().getRank().toInt() != card.getRank().toInt() + 1
                                || toPile.getLast().getSuit() != card.getSuit())) return false;

                LinkedList<Card> movingCards =
                        new LinkedList<>(spider.getRun(fromPile, fromPile.indexOf(card)));

                revealedCard = faceDownBelow(fromPile, card);
                movingCards.forEach(Card::clear);
                if (!fromPile.removeAll(movingCards)) res = false;
                if (!toPile.addAll(movingCards)) res = false;
                break;
            case DEAL:
                for (int column = 0; column < 10; column++) {
                    Card card = spider.getDeck().drawCard();
                    if (card == null) break;
                    if (!spider.getStock().get(column).add(card)) res = false;
                }
                break;
            case RUN:
                revealedCard = faceDownBelow(spider.getStock().get(toIndex), runCards.getFirst());
                res = spider.getStock().get(toIndex).removeAll(runCards);
                break;
        }
        return res;
    }

    public void undo() {
        switch (type) {
            case MOVE:
                LinkedList<Card> fromPile = spider.getStock().get(fromIndex);
                LinkedList<Card> toPile = spider.getStock().get(toIndex);

                LinkedList<Card> movingCards =
                        new LinkedList<>(spider.getRun(toPile, toPile.indexOf(card)));

                movingCards.forEach(Card::clear);
                fromPile.addAll(movingCards);
                toPile.removeAll(movingCards);
                if (revealedCard != null) revealedCard.setFlipped(false);
                break;
            case DEAL:
                for (int column = 9; column > -1; column--) {
                    Card card = spider.getStock().get(column).getLast();
                    if (card == null) break;
                    spider.getStock().get(column).remove(card);
                    card.setFlipped(false);
                    spider.getDeck().addTopDeck(card);
                }
                break;
            case RUN:
                spider.getStock().get(toIndex).addAll(runCards);
                if (revealedCard != null) revealedCard.setFlipped(false);
                spider.finishedRuns--;
                break;
        }
    }

    private static Card faceDownBelow(LinkedList<Card> pile, Card firstMoved) {
        int i = pile.indexOf(firstMoved);
        if (i <= 0) return null;
        Card below = pile.get(i - 1);
        return below.isFaceup() ? null : below;
    }

    public Card getCard() {
        return card;
    }

    public Integer getToIndex() {
        return toIndex;
    }

    Type getType() {
        return type;
    }

    public void setRunCards(LinkedList<Card> runCards) {
        this.runCards = runCards;
    }
}
