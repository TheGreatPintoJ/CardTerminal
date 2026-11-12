package me.pintoadmin.cardTerminal.games.multiplayer.hearts;

import java.util.*;
import java.util.function.*;
import me.pintoadmin.cardTerminal.*;

public class HeartsEngine {
    private final long SEED;
    private final Deck deck;
    private final int endingPoints;

    private int deal = -1;
    private int trick = -1;
    private final List<Card>[] hands =
            new ArrayList[] {
                new ArrayList<Card>(),
                new ArrayList<Card>(),
                new ArrayList<Card>(),
                new ArrayList<Card>()
            };
    private final LinkedList<Card> playedCards = new LinkedList<>();
    private final Map<Integer, List<Card>> wonTrickCards =
            new HashMap<>() {
                {
                    put(0, new ArrayList<>());
                    put(1, new ArrayList<>());
                    put(2, new ArrayList<>());
                    put(3, new ArrayList<>());
                }
            }; // Players to cards of winning tricks
    private final Map<Integer, Integer> scores =
            new HashMap<>(Map.of(0, 0, 1, 0, 2, 0, 3, 0)); // Player (integer) to score

    private final Map<Integer, Card[]> passingCards = new HashMap<>();

    private int startingPlayer = 0;
    private int playerTurn = 0;
    private final Supplier<Integer> currentPlayerTurn = () -> (startingPlayer + playerTurn) % 4;

    private Card.Suit activeSuit;
    private Card.Rank highestRank;

    public HeartsEngine(int endingPoints) {
        this(new Random().nextLong(), endingPoints);
    }

    public HeartsEngine(long SEED, int endingPoints) {
        this.SEED = SEED;
        this.deck = new Deck(SEED);
        this.endingPoints = endingPoints;

        // Deal hands
        newDeal();
    }

    public void newDeal() {
        trick = 0;
        deal++;
        playedCards.clear();
        passingCards.clear();
        wonTrickCards.values().forEach(List::clear);
        for (List<Card> hand : hands) hand.clear();

        deck.shuffleDiscard();
        for (int i = 0; i < 13; i++) {
            hands[0].add(deck.drawCard());
            hands[1].add(deck.drawCard());
            hands[2].add(deck.drawCard());
            hands[3].add(deck.drawCard());
        }
    }

    private void initPassCards() {
        if (deal % 4 != 3) {
            for (int i = 0; i < 4; i++) {
                int modifier =
                        switch (deal % 4) {
                            // to the left; +1
                            case 0 -> 1;
                            // to the right; -1
                            case 1 -> -1;
                            // across; +2
                            case 2 -> 2;
                            default ->
                                    throw new IllegalStateException(
                                            "Unexpected value: " + deal % 4);
                        };
                List<Card> passingToHand = hands[Math.floorMod(i + modifier, 4)];
                List<Card> passingFromHand = hands[i];

                if (!passingFromHand.containsAll(Arrays.stream(passingCards.get(i)).toList()))
                    continue;

                passingToHand.addAll(Arrays.stream(passingCards.get(i)).toList());
                hands[i].removeAll(Arrays.stream(passingCards.get(i)).toList());
            }
        }
        passingCards.clear();
    }

    public boolean passCards(int player, Card[] cards) {
        if (deal % 4 == 3) {
            initPassCards();
            return true;
        }
        if (cards.length != 3) return false;
        if (!hands[player].containsAll(Arrays.stream(cards).toList())) return false;

        passingCards.put(player, cards);

        // if all players has passed cards
        if (passingCards.size() == 4) initPassCards();
        return true;
    }

    public boolean playCard(int player, Card card) {
        if (currentPlayerTurn.get() != player) return false;
        if (card == null) return false;
        boolean canPlay =
                card.getSuit() == activeSuit
                        // player doesn't have any playable cards in hand
                        || (hands[player].stream()
                                        .filter(c -> c.getSuit() == activeSuit)
                                        .toList()
                                        .isEmpty()
                                // AND the card they're playing is a heart
                                && card.getSuit() == Card.Suit.HEART);

        if (hands[player].contains(card) && canPlay) {
            // First card in trick, set active suit
            if (playerTurn == 0) {
                activeSuit = card.getSuit();
                highestRank = card.getRank();
            }

            // Check if higher than previous highest rank
            if (card.getRank().toInt() > highestRank.toInt()) highestRank = card.getRank();

            playedCards.add(card);
            hands[player].remove(card);

            playerTurn++;
            if (playerTurn == 4) finishTrick();
            return true;
        } else return false;
    }

    private void finishTrick() {
        int winningPlayer =
                playedCards.indexOf(
                        playedCards.stream()
                                .filter(
                                        c ->
                                                c.getRank() == highestRank
                                                        && c.getSuit() == activeSuit)
                                .toList()
                                .getFirst());
        wonTrickCards.get(winningPlayer).addAll(playedCards);

        playedCards.forEach(deck::discardCard);
        playedCards.clear();

        startingPlayer = winningPlayer;
        playerTurn = 0;

        highestRank = null;
        activeSuit = null;

        if (trick++ == 13) finishDeal();
    }

    private void finishDeal() {
        // count hearts of each player, add to total
        for (int i = 0; i < 4; i++) {
            int score =
                    Math.toIntExact(
                            wonTrickCards.get(i).stream()
                                    .filter(c -> c.getSuit() == Card.Suit.HEART)
                                    .count());
            if (wonTrickCards.get(i).stream()
                    .anyMatch(
                            c ->
                                    c.getSuit() == Card.Suit.SPADE
                                            && c.getRank() == Card.Rank.QUEEN)) {
                score += 13;
            }
            int finalScore = score;

            // shooting the moon
            if (finalScore >= 26) {
                int finalI = i;
                scores.forEach(
                        (k, v) -> {
                            // all players except 'i'
                            if (k != finalI) {
                                scores.compute(k, (p, s) -> s += 26);
                            }
                        });
            } else scores.compute(i, (k, v) -> v += finalScore);
        }

        Optional<Map.Entry<Integer, Integer>> player =
                scores.entrySet().stream().max(Map.Entry.comparingByValue());
        if (player.isPresent()) {
            int highestScore = player.get().getValue();

            if (highestScore >= endingPoints) {
                endGame();
            }
        }

        newDeal();
    }

    private void endGame() {
        Optional<Map.Entry<Integer, Integer>> player =
                scores.entrySet().stream().max(Map.Entry.comparingByValue());
        if (player.isPresent()) {
            int highestPlayer = player.get().getKey();
            int highestScore = player.get().getValue();

            // TODO: announce who won
        }
    }

    public List<Card> getCards(int player) {
        return hands[player];
    }

    public int getNumTricksWon(int player) {
        return wonTrickCards.get(player).size() / 4;
    }

    public Map<Integer, Integer> getScores() {
        return scores;
    }

    public HeartsEngine newGame() {
        return new HeartsEngine(endingPoints);
    }

    public HeartsEngine newGame(int endingPoints) {
        return new HeartsEngine(endingPoints);
    }

    public HeartsEngine restart() {
        return new HeartsEngine(SEED, endingPoints);
    }

    public long getSeed() {
        return SEED;
    }
}
