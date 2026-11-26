package me.pintoadmin.cardTerminal.games.multiplayer.hearts;

import java.util.*;
import java.util.function.*;
import me.pintoadmin.cardTerminal.*;
import me.pintoadmin.cardTerminal.networking.*;

public class HeartsEngine {
    private final long SEED;
    private final Deck deck;
    private final int endingPoints;
    private final HeartsServer server;

    private Thread autoplay;

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
    private boolean heartsBroken = false;

    Function<Card, Card.Rank> sortByRank = Card::getRank;
    Function<Card.Rank, Integer> sortByValue = Card.Rank::toIntAceHigh;
    Comparator<Card> compareRankThenValue =
            Comparator.comparing(sortByRank.andThen(sortByValue)).reversed();

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
        this.server = new HeartsServer(51337, this);

        autoplay =
                new Thread(
                        () -> {
                            int lastTrick = -1;
                            int lastTurn = -1;
                            while (true) {
                                try {
                                    if (state == GameState.TRICKS) {
                                        int turn = currentPlayerTurn.get();
                                        if (lastTurn == turn && lastTrick == trick) {
                                            Thread.sleep(100);
                                            continue;
                                        }
                                        if (!server.isRemotePlayerConnected(turn)) {
                                            Thread.sleep(
                                                    100); // 1k ms = 1s todo: change back to 1000
                                            int trickBeforePlay = trick;
                                            List<Card> all = getCards(turn);
                                            if (all.isEmpty()) continue;
                                            List<Card> playable = getPlayableCards(turn);
                                            List<Card.Suit> suits =
                                                    new ArrayList<>(
                                                            playable.stream()
                                                                    .map(Card::getSuit)
                                                                    .distinct()
                                                                    .toList());
                                            Card.Suit randSuit =
                                                    suits.size() > 1
                                                            ? suits.get(
                                                                    new Random()
                                                                            .nextInt(suits.size()))
                                                            : suits.getFirst();
                                            Card thisCard;
                                            if (startingPlayer == turn || highestRank == null) {
                                                // Get lowest of a random suit
                                                if (trick == 0) {
                                                    System.out.println("Trick is zero");
                                                    thisCard =
                                                            all.stream()
                                                                    .filter(
                                                                            c ->
                                                                                    c.getSuit()
                                                                                                    == Card
                                                                                                            .Suit
                                                                                                            .CLUB
                                                                                            && c
                                                                                                            .getRank()
                                                                                                    == Card
                                                                                                            .Rank
                                                                                                            .TWO)
                                                                    .toList()
                                                                    .getFirst();
                                                } else {
                                                    thisCard =
                                                            playable.stream()
                                                                    .filter(
                                                                            c ->
                                                                                    c.getSuit()
                                                                                            == randSuit)
                                                                    .sorted(compareRankThenValue)
                                                                    .toList()
                                                                    .reversed()
                                                                    .getFirst();
                                                }
                                            } else {
                                                // get all playable lower than the highest rank
                                                List<Card> lowerThan =
                                                        new ArrayList<>(
                                                                playable.stream()
                                                                        .filter(
                                                                                c ->
                                                                                        c.getRank()
                                                                                                        .toIntAceHigh()
                                                                                                < highestRank
                                                                                                        .toIntAceHigh())
                                                                        .toList()
                                                                        .reversed());
                                                lowerThan.sort(compareRankThenValue);
                                                List<Card> higherThan =
                                                        new ArrayList<>(
                                                                playable.stream()
                                                                        .filter(
                                                                                c ->
                                                                                        c.getRank()
                                                                                                        .toIntAceHigh()
                                                                                                > highestRank
                                                                                                        .toIntAceHigh())
                                                                        .toList()
                                                                        .reversed());
                                                higherThan.sort(compareRankThenValue);
                                                if (!lowerThan.isEmpty()) {
                                                    thisCard = lowerThan.getLast();
                                                } else {
                                                    if (!higherThan.isEmpty()) {
                                                        thisCard = higherThan.getFirst();
                                                    } else {
                                                        // lower than is empty, higher than is empty
                                                        // (not cards of that suit)
                                                        // get all hearts
                                                        List<Card> hearts =
                                                                new ArrayList<>(
                                                                        all.stream()
                                                                                .filter(
                                                                                        c ->
                                                                                                c
                                                                                                                .getSuit()
                                                                                                        == Card
                                                                                                                .Suit
                                                                                                                .HEART)
                                                                                .sorted(
                                                                                        compareRankThenValue)
                                                                                .toList());
                                                        if (hearts.isEmpty()) {
                                                            // play the highest ranked card in
                                                            // general
                                                            thisCard =
                                                                    all.stream()
                                                                            .sorted(
                                                                                    compareRankThenValue)
                                                                            .toList()
                                                                            .getLast();
                                                        } else {
                                                            // play the highest hearts card
                                                            thisCard = hearts.getLast();
                                                        }
                                                    }
                                                }
                                            }
                                            if (playCard(turn, thisCard)) {
                                                lastTrick = trickBeforePlay;
                                                lastTurn = turn;
                                            } else {
                                                System.err.println(
                                                        "Bot "
                                                                + turn
                                                                + " could not play card: "
                                                                + thisCard.getRank().toChars()
                                                                + thisCard.getSuit().toChar());
                                            }
                                        }
                                    } else if (state == GameState.PASSING) {
                                        for (int player = 1; player < 4; player++) {
                                            if (!server.isRemotePlayerConnected(player)
                                                    && passingCards.get(player) == null) {
                                                List<Card> all = getCards(player);
                                                Card[] cards = new Card[3];
                                                for (int i = 0; i < 3; i++) {
                                                    cards[i] =
                                                            all.get(
                                                                    new Random()
                                                                            .nextInt(all.size()));
                                                }
                                                passCards(player, cards);
                                            }
                                        }
                                        Thread.sleep(100);
                                    }
                                } catch (NoSuchElementException e) {
                                    System.out.println(
                                            "No card after filtering - " + e.getMessage());
                                } catch (InterruptedException e) {
                                    System.out.println("Ignored exception: " + e.getMessage());
                                }
                            }
                        });
    }

    public synchronized void start() {
        if (state == GameState.PREGAME) {
            newDeal();
            server.updateClients();

            autoplay.start();
        }
    }

    private void newDeal() {
        heartsBroken = false;
        trick = 0;
        playedCards.clear();
        passingCards.clear();
        wonTrickCards.values().forEach(List::clear);
        for (List<Card> hand : hands) hand.clear();

        server.updateClients();
        server.resizeClients();

        deck.shuffleDiscard();
        for (int i = 0; i < 13; i++) {
            hands[0].add(deck.drawCard());
            hands[1].add(deck.drawCard());
            hands[2].add(deck.drawCard());
            hands[3].add(deck.drawCard());
        }

        sortCards();

        deal++;
        if (deal % 4 == 3) {
            state = GameState.TRICKS;
            initPassCards();
        } else state = GameState.PASSING;
        System.out.println("Pass type number: " + (deal % 4));

        server.updateClients();
        server.resizeClients();
    }

    private void sortCards() {
        for (List<Card> hand : hands) {
            hand.sort(Comparator.comparing(Card::getSuit).thenComparing(compareRankThenValue));
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
        sortCards();
        server.updateClients();
        server.resizeClients();
    }

    public synchronized boolean passCards(int player, Card[] cards) {
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

    public synchronized boolean playCard(int player, Card card) {
        if (!(player >= 0 && player <= 3)) return false;
        if (state != GameState.TRICKS) return false;
        if (currentPlayerTurn.get() != player) return false;
        if (card == null) return false;

        boolean isHeart = card.getSuit() == me.pintoadmin.cardTerminal.Card.Suit.HEART;

        if (getPlayableCards(player).contains(card)) {
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

            server.updateClients();
            server.resizeClients();
            if (playerTurn == 4) finishTrick();
            return true;
        } else return false;
    }

    private static boolean isPenaltyCard(Card card) {
        return card.getSuit() == me.pintoadmin.cardTerminal.Card.Suit.HEART
                || (card.getSuit() == me.pintoadmin.cardTerminal.Card.Suit.SPADE
                        && card.getRank() == me.pintoadmin.cardTerminal.Card.Rank.QUEEN);
    }

    private void finishTrick() {
        server.updateClients();
        server.resizeClients();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.out.println("Ignored exception: " + e.getMessage());
        }

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

        server.updateClients();
        server.resizeClients();

        if (trick++ == 12) finishDeal();
    }

    private void finishDeal() {
        // count hearts of each player, add to total
        for (int i = 0; i < 4; i++) {
            int score = getScore(wonTrickCards.get(i));

            // shooting the moon
            if (score >= 26) {
                int finalI = i;
                scores.forEach(
                        (k, v) -> {
                            // all players except 'i'
                            if (k != finalI) {
                                scores.compute(k, (p, s) -> s += 26);
                            }
                        });
            } else scores.compute(i, (k, v) -> v += score);
        }

        server.updateClients();
        server.showScoresClients();
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            System.out.println("Ignored exception: " + e.getMessage());
        }
        server.resizeClients();

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

    public static int getScore(List<Card> cards) {
        int score =
                Math.toIntExact(
                        cards.stream()
                                .filter(
                                        c ->
                                                c.getSuit()
                                                        == me.pintoadmin
                                                                .cardTerminal
                                                                .Card
                                                                .Suit
                                                                .HEART)
                                .count());
        if (cards.stream()
                .anyMatch(
                        c ->
                                c.getSuit() == me.pintoadmin.cardTerminal.Card.Suit.SPADE
                                        && c.getRank()
                                                == me.pintoadmin.cardTerminal.Card.Rank.QUEEN)) {
            score += 13;
        }
        return score;
    }

    public void endGame() {
        state = GameState.ENDED;

        Integer winningPlayer = getLowestPlayer();
        Integer winningScore = scores.get(winningPlayer);

        // TODO: announce who won (HeartsEnd)
        server.showScoresClients();

        server.deinit();
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
        return getPlayableCards(hands[player], activeSuit, heartsBroken, trick, playerTurn == 0);
    }

    /**
     * Single source of truth for which cards in {@code hand} may legally be played. Used by {@link
     * #playCard(int, Card)} for validation and by clients/bots for display and selection.
     */
    static List<Card> getPlayableCards(
            List<Card> hand,
            Card.Suit activeSuit,
            boolean heartsBroken,
            int trick,
            boolean leading) {
        if (leading) {
            // First lead of the deal must be the two of clubs
            if (trick == 0)
                return hand.stream()
                        .filter(c -> c.getSuit() == Card.Suit.CLUB && c.getRank() == Card.Rank.TWO)
                        .toList();
            // Hearts can't be led until broken, unless only hearts remain
            boolean hasNonHearts = hand.stream().anyMatch(c -> c.getSuit() != Card.Suit.HEART);
            if (heartsBroken || !hasNonHearts) return List.copyOf(hand);
            return hand.stream().filter(c -> c.getSuit() != Card.Suit.HEART).toList();
        }

        // Must follow the led suit if possible
        boolean hasLedSuit =
                activeSuit != null && hand.stream().anyMatch(c -> c.getSuit() == activeSuit);
        List<Card> playable =
                hasLedSuit
                        ? hand.stream().filter(c -> c.getSuit() == activeSuit).toList()
                        : List.copyOf(hand);

        // No penalty cards on the first trick unless there's no other legal choice
        if (trick == 0 && playable.stream().anyMatch(c -> !isPenaltyCard(c)))
            playable = playable.stream().filter(c -> !isPenaltyCard(c)).toList();
        return playable;
    }

    public int getNumCards(int player) {
        return hands[player].size();
    }

    public boolean[] getRemotePlayers() {
        boolean[] arr = new boolean[4];
        for (int i = 0; i < 4; i++) {
            arr[i] = getServer().isRemotePlayerConnected(i);
        }
        return arr;
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

    public Card.Suit getActiveSuit() {
        return activeSuit;
    }

    public int getCurrentPlayerTurn() {
        return currentPlayerTurn.get();
    }

    public int getStartingPlayer() {
        return startingPlayer;
    }

    public GameState getState() {
        return state;
    }

    public Map<Integer, List<Card>> getWonTricks() {
        return wonTrickCards;
    }

    public int getTrick() {
        return trick;
    }

    public int getDeal() {
        return deal;
    }

    public HeartsServer getServer() {
        return server;
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
