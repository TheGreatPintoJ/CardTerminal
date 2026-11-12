package me.pintoadmin.cardTerminal.games.multiplayer.hearts;

import me.pintoadmin.cardTerminal.*;

import java.util.*;
import java.util.function.*;

public class HeartsEngine {
    private final long SEED;
    private final Deck deck;

    private int deal = -1;
    private int trick = -1;
    private final List<Card>[] hands = new ArrayList[]{
            new ArrayList<Card>(),
            new ArrayList<Card>(),
            new ArrayList<Card>(),
            new ArrayList<Card>()
    };
    private final LinkedList<Card> playedCards = new LinkedList<>();
    private final Map<Integer, List<Card>> wonTrickCards = new HashMap<>(){
        {
            put(0, new ArrayList<>());
            put(1, new ArrayList<>());
            put(2, new ArrayList<>());
            put(3, new ArrayList<>());
        }
    }; // Players to number of tricks won

    private final Map<Integer, Card[]> passingCards = new HashMap<>();

    private int startingPlayer = 0;
    private int playerTurn = 0;
    private final Supplier<Integer> currentPlayerTurn = () -> (startingPlayer + playerTurn) % 4;

    private Card.Suit activeSuit;
    private Card.Rank highestRank;

    public HeartsEngine(){
        this(new Random().nextLong());
    }

    public HeartsEngine(long SEED){
        this.SEED = SEED;
        this.deck = new Deck(SEED);

        // Deal hands
        newDeal();
    }

    public void newDeal(){
        trick = 0;
        deal++;
        playedCards.clear();
        passingCards.clear();
        for (List<Card> hand : hands) hand.clear();

        deck.shuffleDiscard();
        for (int i = 0; i < 13; i++){
            hands[0].add(deck.drawCard());
            hands[1].add(deck.drawCard());
            hands[2].add(deck.drawCard());
            hands[3].add(deck.drawCard());
        }
    }

    private void initPassCards(){
        if(deal % 4 != 3){
            for (int i = 0; i < 4; i++) {
                int modifier = switch (deal % 4) {
                    // to the left; +1
                    case 0 -> 1;
                    // to the right; -1
                    case 1 -> -1;
                    // across; +2
                    case 2 -> 2;
                    default -> throw new IllegalStateException("Unexpected value: " + deal % 4);
                };
                List<Card> passingToHand = hands[Math.floorMod(i + modifier, 4)];
                List<Card> passingFromHand = hands[i];

                if(!passingFromHand.containsAll(Arrays.stream(passingCards.get(i)).toList())) continue;

                passingToHand.addAll(Arrays.stream(passingCards.get(i)).toList());
                hands[i].removeAll(Arrays.stream(passingCards.get(i)).toList());
            }
        }
        passingCards.clear();
    }

    public boolean passCards(int player, Card[] cards){
        if(deal % 4 == 3) {
            initPassCards();
            return true;
        }
        if(cards.length != 3) return false;
        if(!hands[player].containsAll(Arrays.stream(cards).toList())) return false;

        passingCards.put(player, cards);

        // if all players has passed cards
        if(passingCards.size() == 4) initPassCards();
        return true;
    }

    public boolean playCard(int player, Card card) {
        if(currentPlayerTurn.get() != player) return false;
        if(card == null) return false;
        boolean canPlay = card.getSuit() == activeSuit
                // player doesn't have any playable cards in hand
                || (hands[player].stream().filter(c -> c.getSuit() == activeSuit).toList().isEmpty()
                    // AND the card they're playing is a heart
                    && card.getSuit() == Card.Suit.HEART);

        if (hands[player].contains(card) && canPlay){
            // First card in trick, set active suit
            if(playerTurn == 0) {
                activeSuit = card.getSuit();
                highestRank = card.getRank();
            }

            // Check if higher than previous highest rank
            if(card.getRank().toInt() > highestRank.toInt())
                highestRank = card.getRank();

            playerTurn++;
            if(playerTurn == 4) {
                finishTrick();
            } else {
                playedCards.add(card);
                hands[player].remove(card);
            }
            return true;
        } else return false;
    }

    private void finishTrick(){
        int winningPlayer = playedCards.indexOf(playedCards.stream().filter(c -> c.getRank() == highestRank && c.getSuit() == activeSuit).toList().getFirst());
        wonTrickCards.get(winningPlayer).addAll(playedCards);

        playedCards.forEach(deck::discardCard);
        playedCards.clear();

        startingPlayer = winningPlayer;
        playerTurn = 0;

        highestRank = null;
        activeSuit = null;

        if(trick++ == 13) finishDeal();
    }

    private void finishDeal(){
        // TODO: count hearts of each player, add to total

        newDeal();
    }

    public List<Card> getCards(int player){
        return hands[player];
    }

    public int getNumTricksWon(int player){
        return wonTrickCards.get(player).size() / 4;
    }

    public HeartsEngine newGame() {
        return new HeartsEngine();
    }

    public HeartsEngine restart() {
        return new HeartsEngine(SEED);
    }

    public long getSeed() {
        return SEED;
    }
}
