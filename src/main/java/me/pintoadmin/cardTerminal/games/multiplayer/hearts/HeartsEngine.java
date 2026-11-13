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

    Card.Suit activeSuit;
    private Card.Rank highestRank;
    private boolean heartsBroken = false;

    private GameState state = GameState.PREGAME;

    public enum GameState {
        PREGAME,
        PASSING,
        TRICKS,
        ENDED
    }

    public HeartsEngine(int endingPoints) {
        this(new Random().nextLong(), endingPoints);
    }

    public HeartsEngine(long SEED, int endingPoints) {
        this.SEED = SEED;
        this.deck = new Deck(SEED);
        if (endingPoints > 0) this.endingPoints = endingPoints;
        else this.endingPoints = 100;
    }

    public void start() {
        if (state == GameState.PREGAME) newDeal();
    }

    private void newDeal() {
        heartsBroken = false;
        trick = 0;
        deal++;
        playedCards.clear();
        passingCards.clear();
        wonTrickCards.values().forEach(List::clear);
        for (List<Card> hand : hands) hand.clear();

        deck.shuffleDiscard();
        for (int i = 0; i < 13; i++) {
            // TESTING: add two of clubs to first player's hand
            if (i == 0) {
                hands[0].add(deck.getCard(Card.Rank.TWO, Card.Suit.CLUB));
                deck.removeCard(deck.getCard(Card.Rank.TWO, Card.Suit.CLUB));
            } else hands[0].add(deck.drawCard());
            hands[1].add(deck.drawCard());
            hands[2].add(deck.drawCard());
            hands[3].add(deck.drawCard());
        }

        if (deal % 4 == 3) {
            state = GameState.TRICKS;
            initPassCards();
        } else state = GameState.PASSING;
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
        state = GameState.TRICKS;
        // set startingPlayer based on two of clubs
        int i = 0;
        for (List<Card> hand : hands) {
            int finalI = i;
            hand.forEach(
                    c -> {
                        if (c.getSuit() == Card.Suit.CLUB && c.getRank() == Card.Rank.TWO) {
                            startingPlayer = finalI;
                        }
                    });
            i++;
        }
    }

    public boolean passCards(int player, Card[] cards) {
        if (!(player >= 0 && player <= 3)) return false;
        if (state != GameState.PASSING) return false;
        if (passingCards.containsKey(player)) return false;
        if (deal % 4 == 3) {
            state = GameState.TRICKS;
            initPassCards();
            return true;
        }
        if (cards.length != 3) return false;
        if (!hands[player].containsAll(Arrays.stream(cards).toList())) return false;
        // Distinct check
        if (Arrays.stream(cards).distinct().count() != cards.length) return false;

        if (!passingCards.containsKey(player)) passingCards.put(player, cards.clone());

        // if all players has passed cards
        if (passingCards.size() == 4) initPassCards();
        return true;
    }

    public boolean playCard(int player, Card card) {
        if (!(player >= 0 && player <= 3)) return false;
        if (state != GameState.TRICKS) return false;
        if (currentPlayerTurn.get() != player) return false;
        if (card == null) return false;

        boolean firstTurn = playerTurn == 0;
        boolean hasNonHearts = hands[player].stream().anyMatch(c -> c.getSuit() != Card.Suit.HEART);
        boolean isHeart = card.getSuit() == me.pintoadmin.cardTerminal.Card.Suit.HEART;
        boolean isActiveSuit = card.getSuit() == activeSuit;
        boolean hasLedSuit =
                activeSuit != null
                        && hands[player].stream().anyMatch(c -> c.getSuit() == activeSuit);
        boolean canLead = !isHeart || heartsBroken || !hasNonHearts;
        boolean canFollow = isActiveSuit || !hasLedSuit;

        // force two of clubs on first play of first trick
        if (trick == 0 && firstTurn)
            if (card.getSuit() != Card.Suit.CLUB || card.getRank() != Card.Rank.TWO) return false;

        boolean hasNonPenaltyLegalCard =
                hands[player].stream()
                        .anyMatch(
                                c ->
                                        (!hasLedSuit || c.getSuit() == activeSuit)
                                                && !isPenaltyCard(c));
        boolean canPlay =
                (firstTurn ? canLead : canFollow)
                        && !(trick == 0 && isPenaltyCard(card) && hasNonPenaltyLegalCard);

        if (hands[player].contains(card) && canPlay) {
            if (isHeart) heartsBroken = true;
            // First card in trick, set active suit
            if (playerTurn == 0) {
                activeSuit = card.getSuit();
                highestRank = card.getRank();
            }

            // Check if higher than previous highest rank
            if (card.getRank().toIntAceHigh() > highestRank.toIntAceHigh()
                    && card.getSuit() == activeSuit) highestRank = card.getRank();

            playedCards.add(card);
            hands[player].remove(card);

            playerTurn++;
            if (playerTurn == 4) finishTrick();
            return true;
        } else return false;
    }

    private boolean isPenaltyCard(Card card) {
        return card.getSuit() == Card.Suit.HEART
                || (card.getSuit() == Card.Suit.SPADE && card.getRank() == Card.Rank.QUEEN);
    }

    private void finishTrick() {
        int winningPlayer =
                Math.floorMod(
                        playedCards.indexOf(
                                        playedCards.stream()
                                                .filter(
                                                        c ->
                                                                c.getRank() == highestRank
                                                                        && c.getSuit()
                                                                                == activeSuit)
                                                .toList()
                                                .getFirst())
                                + startingPlayer,
                        4);
        wonTrickCards.get(winningPlayer).addAll(playedCards);

        playedCards.forEach(deck::discardCard);
        playedCards.clear();

        startingPlayer = winningPlayer;
        playerTurn = 0;

        highestRank = null;
        activeSuit = null;

        if (trick++ == 12) finishDeal();
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

        Integer highestPlayer = getHighestPlayer();
        Integer highestScore = scores.get(highestPlayer);

        Integer lowestPlayer = getLowestPlayer();
        Integer lowestScore = scores.get(lowestPlayer);

        // highest is equal or above threshold for game end
        if (highestScore >= endingPoints
                // AND there's no more than one player who has the lowest score
                && scores.values().stream().filter(i -> i.equals(lowestScore)).count() <= 1) {
            // tiebreakers will be determined by ignoring win condition and playing another deal
            endGame();
            return;
        }

        newDeal();
    }

    private void endGame() {
        state = GameState.ENDED;

        Integer winningPlayer = getLowestPlayer();
        Integer winningScore = scores.get(winningPlayer);

        // TODO: announce who won
    }

    private Integer getHighestPlayer() {
        Optional<Map.Entry<Integer, Integer>> player =
                scores.entrySet().stream().max(Map.Entry.comparingByValue());
        return player.map(Map.Entry::getKey).orElse(null);
    }

    private Integer getLowestPlayer() {
        Optional<Map.Entry<Integer, Integer>> player =
                scores.entrySet().stream().min(Map.Entry.comparingByValue());
        return player.map(Map.Entry::getKey).orElse(null);
    }

    public List<Card> getCards(int player) {
        return hands[player];
    }

    public List<Card> getPlayedCards() {
        return playedCards;
    }

    List<Card> getPlayableCards(int player) {
        List<Card> sameSuit =
                hands[player].stream().filter(c -> c.getSuit() == activeSuit).toList();
        List<Card> notSuit = hands[player].stream().filter(c -> c.getSuit() != activeSuit).toList();
        return sameSuit.isEmpty() ? notSuit : sameSuit;
    }

    public int getNumTricksWon(int player) {
        return wonTrickCards.get(player).size() / 4;
    }

    public Map<Integer, Integer> getScores() {
        return scores;
    }

    public boolean isHeartsBroken() {
        return heartsBroken;
    }

    public GameState getState() {
        return state;
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
