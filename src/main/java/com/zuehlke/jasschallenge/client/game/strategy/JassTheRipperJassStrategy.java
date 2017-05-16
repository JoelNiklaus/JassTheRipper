package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.JassHelper;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.MCTSHelper;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// TODO BAD_MESSAGE überprüfen. Spielt karte statt trump. ???


public class JassTheRipperJassStrategy extends RandomJassStrategy implements JassStrategy, Serializable {

	// the maximal number of milliseconds per choose card move
	private static final int MAX_THINKING_TIME = 400;


	private final int max_schift_rating_val = 30;


	// TODO Wo sollten die Exceptions gecatcht werden???
	// TODO hilfsmethoden bockVonJederFarbe, TruempfeNochImSpiel, statistisches Modell von möglichen Karten von jedem Spieler
	// TODO alle gespielten Karten merken

	// wähle trumpf mit besten voraussetzungen -> ranking
	// bei drei sicheren stichen -> obeabe oder undeufe
	//
	// wenn nicht gut -> schieben
	@Override
	public Mode chooseTrumpf(Set<Card> availableCards, GameSession session, boolean isGschobe) {
		printCards(availableCards);

		Mode mode = JassHelper.getRandomMode(isGschobe);

		//mode = predictTrumpf(availableCards, mode);

		System.out.println("Chose Trumpf " + mode);

		return mode;
	}

	private Mode predictTrumpf(Set<Card> availableCards, Mode prospectiveMode) {
		int max = 0;
		for (Color color : Color.values()) {
			int colorTrumpRating = rate(availableCards, color);
			if (colorTrumpRating > max) {
				max = colorTrumpRating;
				prospectiveMode = Mode.from(Trumpf.TRUMPF, color);
			}
		}
		if (rateObeabe(availableCards) > max)
			prospectiveMode = Mode.topDown();
		if (rateUndeufe(availableCards) > max)
			prospectiveMode = Mode.bottomUp();
		System.out.println("ChooseTrumpf succeeded!");
		if (max < max_schift_rating_val)
			prospectiveMode = Mode.shift();
		return prospectiveMode;
	}


	private int rate(Set<Card> cardStream, Color color) {
		return 30;
	}

	public int rateObeabe(Set<Card> cards) {
		int sum = 0;
		for (Color color : Color.values()) {
			sum += rateObeabeColor(cards, color);
		}
		return sum;
	}

	public int rateUndeufe(Set<Card> cards) {
		int sum = 0;
		for (Color color : Color.values()) {
			sum += rateUndeufeColor(cards, color);
		}
		return sum;
	}

	public int rateObeabeColor(Set<Card> cards, Color color) {
	    // Get the cards in descending order
	    List<Card> sortedCards = sortCardsDescending(cards, color);
	    if (sortedCards.size() == 0)
	        return 0;
	    // Number of cards I have that are higher than the nextCard
		int higherCards = 0;
		// Number of the cards I have of this color
		int numberOfMyCards = sortedCards.size();
		// Estimate how safe you make a Stich with your highest card
        float safety = calculateInitialSafetyObeabe(sortedCards);
        // The last card rated
        Card lastCard = sortedCards.get(0);
        // 1 Stich entspricht 10 ratingPoints
        float rating = safety * 10;
        // remove the last card tested
        sortedCards.remove(lastCard);
		while(sortedCards.size() > 0) {
		    // The next card to be tested
		    Card nextCard = sortedCards.get(0);
		    // Estimate how safe you Stich with that card
            safety *= safetyOfStich(numberOfMyCards, higherCards, nextCard, lastCard);
            // How safe is the Stich? * Stichvalue
            rating += safety * 10;
            sortedCards.remove(0);
            // One card is higher than the last
            higherCards++;
            lastCard = nextCard;
        }
		return (int) Math.ceil(rating);
	}

    public List<Card> sortCardsDescending(Set<Card> cards, Color color) {
        Set<Card> cardsOfColor = JassHelper.getSortedCardsOfColor(cards, color);
        return cardsOfColor.stream().sorted(Comparator.comparing(Card::getRank).reversed()).collect(Collectors.toList());
    }

    public List<Card> sortCardsAscending(Set<Card> cards, Color color) {
        Set<Card> cardsOfColor = JassHelper.getSortedCardsOfColor(cards, color);
        return cardsOfColor.stream().sorted(Comparator.comparing(Card::getRank)).collect(Collectors.toList());
    }

    public float calculateInitialSafetyObeabe(List<Card> sortedCards) {
        Card nextCard = sortedCards.get(0);
        int rank = nextCard.getRank();
        float safety = 1;
        // Probability that my partner has all the higher cards
        for (int i = 0; i < 9-rank; i++)
            safety /= 3;
        return safety;
    }

    public float calculateInitialSafetyUndeUfe(List<Card> sortedCards) {
        Card nextCard = sortedCards.get(0);
        int rank = nextCard.getRank();
        float safety = 1;
        // Probability that my partner has all the lower cards
        for (int i = 0; i < rank-1; i++)
            safety /= 3;
        return safety;
    }

    private float safetyOfStich(int numberOfCards, int higherCards, Card nextCard, Card lastCard) {
	    int numberOfCardsBetween = lastCard.getRank() - nextCard.getRank();
	    // Have the next-higher card => probability to stich is the same as with the next higher card
	    if (numberOfCardsBetween == 0)
	        return 1;
	    // Probability to stich is 1 - the probability, that enemy has a higher card + enough cards to discard
	    else
	        return 1 - ((float)2/3 * enemenyHasNoMoreCards(numberOfCards, higherCards, numberOfCardsBetween));
    }

    private float enemenyHasNoMoreCards(int numberOfMyCards, int higherCards, int numberOfCardsBetween) {
	    float estimate = 1;
	    int otherColorCards = 9-numberOfMyCards-1;
	    int otherCards = 27-1;
	    // Only rough estimate of the probability, that a player of the other team has enough cards to discard (i.e. I
        // have an Ace and King, but he has 6 and 7 so can discard those invaluable cards
        estimate *= (otherColorCards)/otherCards;
	    for (int i = 0; i < higherCards; i++) {
	        otherColorCards--;
            estimate *= (otherColorCards)/otherCards;
        }
        // Estimate of a mathematician; if the number of ranks between the last one and this one is increased by one,
        // the probability of me making the Stich is roughly halved.
        for (int i = 0; i < numberOfCardsBetween; i++)
            estimate *= 0.45;
	    if (estimate > 0)
	        return estimate;
	    else
	        return 0;
    }

	public int rateUndeufeColor(Set<Card> cards, Color color) {
        // Get the cards in ascending order
        List<Card> sortedCards = sortCardsAscending(cards, color);
        if (sortedCards.size() == 0)
            return 0;
        // Number of cards I have that are lower than the nextCard
        int lowerCards = 0;
        // Number of the cards I have of this color
        int numberOfMyCards = sortedCards.size();
        // Estimate how safe you make a Stich with your highest card
        float safety = calculateInitialSafetyUndeUfe(sortedCards);
        // The last card rated
        Card lastCard = sortedCards.get(0);
        // 1 Stich entspricht 10 ratingPoints
        float rating = safety * 10;
        // remove the last card tested
        sortedCards.remove(lastCard);
        while(sortedCards.size() > 0) {
            // The next card to be tested
            Card nextCard = sortedCards.get(0);
            // Estimate how safe you Stich with that card
            safety *= safetyOfStich(numberOfMyCards, lowerCards, nextCard, lastCard);
            // How safe is the Stich? * Stichvalue
            rating += safety * 10;
            sortedCards.remove(0);
            // One card is higher than the last
            lowerCards++;
            lastCard = nextCard;
        }
        return (int) Math.ceil(rating);
	}

	@Override
	public Card chooseCard(Set<Card> availableCards, GameSession session) {
		final long startTime = System.nanoTime();
		final long endingTime = startTime + 1000000 * MAX_THINKING_TIME;
		printCards(availableCards);
		final Game game = session.getCurrentGame();
		final Set<Card> possibleCards = JassHelper.getPossibleCards(availableCards, game);

		if (possibleCards.isEmpty())
			System.err.println("We have a serious problem! No possible card to play!");

		if (possibleCards.size() == 1)
			for (Card card : possibleCards)
				return card;

		Card card = JassHelper.getRandomCard(possibleCards, game);

		try {
			final Card mctsCard = MCTSHelper.getCard(availableCards, game, endingTime);
			if (possibleCards.contains(card)) {
				System.out.println("Chose Card based on MCTS, Hurra!");
				card = mctsCard;
			} else
				System.out.println("Card chosen not in available cards. Had to choose random card, Damn it!");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Something went wrong. Had to choose random card, Damn it!");
		}

		final long endTime = (System.nanoTime() - startTime) / 1000000;
		System.out.println("Total time for move: " + endTime + "ms");
		System.out.println("Played " + card + " out of possible Cards " + possibleCards + " out of available Cards " + availableCards+ "\n\n");
		assert card != null;
		assert possibleCards.contains(card);
		return card;
	}

	private void printCards(Set<Card> availableCards) {
		System.out.println("Hi there! I am JassTheRipper and these are my cards: " + availableCards);
	}
}
