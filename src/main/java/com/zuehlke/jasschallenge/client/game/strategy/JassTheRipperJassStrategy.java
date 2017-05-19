package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.JassHelper;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.MCTSHelper;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JassTheRipperJassStrategy extends RandomJassStrategy implements JassStrategy, Serializable {

	// the maximal number of milliseconds per choose card move
	private static final int MAX_THINKING_TIME = 350;

	// IMPORTANT: If but does not work properly, try setting this to false
	private static final boolean PARALLELISATION_ENABLED = true;


	private final int max_schift_rating_val = 60;


	// TODO Wo sollten die Exceptions gecatcht werden???
	// TODO hilfsmethoden bockVonJederFarbe, TruempfeNochImSpiel, statistisches Modell von möglichen Karten von jedem Spieler

	// wähle trumpf mit besten voraussetzungen -> ranking
	// bei drei sicheren stichen -> obeabe oder undeufe
	//
	// wenn nicht gut -> schieben
	@Override
	public Mode chooseTrumpf(Set<Card> availableCards, GameSession session, boolean isGschobe) {
		final long startTime = System.currentTimeMillis();
		printCards(availableCards);

		Mode mode = JassHelper.getRandomMode(isGschobe);

		mode = predictTrumpf(availableCards, mode, isGschobe);

		final long endTime = (System.currentTimeMillis() - startTime);
		System.out.println("Total time for move: " + endTime + "ms");
		System.out.println("Chose Trumpf " + mode);

		return mode;
	}

	private Mode predictTrumpf(Set<Card> availableCards, Mode prospectiveMode, boolean isGschobe) {
		int max = 0;
		for (Color color : Color.values()) {
			int colorTrumpRating = rateColorForTrumpf(availableCards, color);
			if (colorTrumpRating > max) {
				max = colorTrumpRating;
				prospectiveMode = Mode.from(Trumpf.TRUMPF, color);
			}
		}
		// rateObeabe and rateUndeUfe are 180 at max; 180 = can make all Stich
		// Rate ObeAbe and UndeUfe much much lower (with 1/3) when it is gschobe
		// TODO: This is an ugly hotfix, make it nicer ;)
		float gschobeFactor = 1;
		if (isGschobe)
			gschobeFactor = 1/3;
		if (gschobeFactor * rateObeabe(availableCards) > max) {
			prospectiveMode = Mode.topDown();
			max = rateObeabe(availableCards);
		}
		if (gschobeFactor * rateUndeufe(availableCards) > max) {
			prospectiveMode = Mode.bottomUp();
			max = rateUndeufe(availableCards);
		}
		System.out.println("ChooseTrumpf succeeded!");
		if (max < max_schift_rating_val && !isGschobe)
			return Mode.shift();
		return prospectiveMode;
	}

	public int rateColorForTrumpf(Set<Card> cards, Color color) {
		Set<Card> cardsOfColor = JassHelper.getCardsOfColor(cards, color);
		if (cardsOfColor.size() <= 1)
			return 0;
		List<Card> cardsOfColorL = new ArrayList<>(cardsOfColor);
		boolean[] prospectiveTrumpfCards = new boolean[9];
		for (int i = 0; i < 9; i++) {
		    if (cardsOfColorL.isEmpty())
		        break;
			if (cardsOfColorL.get(0).getRank() == i + 1) {
				prospectiveTrumpfCards[i] = true;
				cardsOfColorL.remove(0);
			}
		}
		int rating = 0;
		// If you have 6 or more Cards of this color, rate it insanely high!
        if (cardsOfColor.size() >= 6)
            rating += 120;
		float qualityOfTrumpfCards = 0;
		// rate plus 2.25 * Trumpfrank (9 for Jack, 8 for Nell, 7 for Ace, …)
		// Maximum is 90 this way
		for (Card card : cardsOfColor) {
			qualityOfTrumpfCards += 2.25 * card.getTrumpfRank();
		}
		rating += (int) qualityOfTrumpfCards;
		// If you have Jack, rate higher
		if (prospectiveTrumpfCards[5])
			rating += 10;
		// If you have Nell, rate higher
		if (prospectiveTrumpfCards[3])
			rating += 7;
		// If you have Ace, rate slightly higher
		if (prospectiveTrumpfCards[8])
			rating += 3;

		// If a lot of a color, it is rather good Trumpf (small weight)
		for (Card card : cardsOfColor) {
			// TODO: maybe do something with * instead of + here?
			rating += 3;
		}
		// If Jack and Nell and another Trumpf and 2 Aces: good Trumpf
		if ((prospectiveTrumpfCards[5] && prospectiveTrumpfCards[3]) && cardsOfColor.size() > 2 && containsTwoOrMoreAces(cards))
			if (cardsOfColor.size() > 3)
				rating += 40;
			else
				rating += 30;
		// If you have Jack + 3 cards, one of which is good => would be good Trumpf!
		if (prospectiveTrumpfCards[5] && cardsOfColor.size() > 3 && qualityOfTrumpfCards - 9 > 12)
			if (cardsOfColor.size() > 4)
				rating += 40;
			else
				rating += 30;
		// If Nell + 4 --> is good Trumpf; + 3 solid Trumpf
		if ((prospectiveTrumpfCards[3] && cardsOfColor.size() > 3))
			if (cardsOfColor.size() > 4)
				rating += 30;
			else
				rating += 20;
		// If Nell, Ass + 3 --> is good Trumpf
		if ((prospectiveTrumpfCards[5] && prospectiveTrumpfCards[8] && cardsOfColor.size() > 4))
			if (cardsOfColor.size() > 5)
				rating += 40;
			else
				rating += 30;

		// Try to consider the value of the other cards; is kind of similar to ObeAbe, but a lot less valuable…
		for (Color c : Color.values()) {
			// Does it make sense to exclude the own color?
			//if (c != color)
			rating += (rateObeabeColor(cards, c) / 4);
		}
		return rating;
	}

	private boolean containsTwoOrMoreAces(Set<Card> cardStream) {
		List<Card> cardsSorted = cardStream.stream().sorted(Comparator.comparing(Card::getRank).reversed()).collect(Collectors.toList());
		int countAces = 0;
		while (cardsSorted.get(0).getRank() == 9) {
			countAces++;
			cardsSorted.remove(0);
		}
		return countAces >= 2;
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
		List<Card> sortedCardOfColor = sortCardsOfColorDescending(cards, color);
		if (sortedCardOfColor.isEmpty())
			return 0;
		// Number of cards I have that are higher than the nextCard
		int higherCards = 0;
		// Number of the cards I have of this color
		int numberOfMyCards = sortedCardOfColor.size();
		// Estimate how safe you make a Stich with your highest card
		float safety = calculateInitialSafetyObeabe(sortedCardOfColor);
		// The last card rated
		Card lastCard = sortedCardOfColor.get(0);
		// 1 Stich entspricht 20 ratingPoints
		float rating = safety * 20;
		// remove the last card tested
		sortedCardOfColor.remove(lastCard);
		while (sortedCardOfColor.size() > 0) {
			// The next card to be tested
			Card nextCard = sortedCardOfColor.get(0);
			// Estimate how safe you Stich with that card
			int numberOfCardsInbetween = lastCard.getRank() - nextCard.getRank() - 1;
			higherCards += numberOfCardsInbetween;
			safety *= safetyOfStich(numberOfMyCards, higherCards, nextCard, lastCard);
			// How safe is the Stich? * Stichvalue
			rating += safety * 20;
			sortedCardOfColor.remove(0);
			// One card is higher than the last
			higherCards++;
			lastCard = nextCard;
		}
		// TODO: Take 'Opferkarten' into account; King with 7 should be much more valuable than only King!
		return (int) Math.ceil(rating);
	}

	public List<Card> sortCardsOfColorDescending(Set<Card> cards, Color color) {
		Set<Card> cardsOfColor = JassHelper.getCardsOfColor(cards, color);
		return cardsOfColor.stream().sorted(Comparator.comparing(Card::getRank).reversed()).collect(Collectors.toList());
	}

	public List<Card> sortCardsOfColorAscending(Set<Card> cards, Color color) {
		Set<Card> cardsOfColor = JassHelper.getCardsOfColor(cards, color);
		return cardsOfColor.stream().sorted(Comparator.comparing(Card::getRank)).collect(Collectors.toList());
	}

	public float calculateInitialSafetyObeabe(List<Card> sortedCards) {
		Card nextCard = sortedCards.get(0);
		int rank = nextCard.getRank();
		float safety = 1;
		// Probability that my partner has all the higher cards
		for (int i = 0; i < 9 - rank; i++)
			safety /= 3;
		return safety;
	}

	public float calculateInitialSafetyUndeUfe(List<Card> sortedCards) {
		Card nextCard = sortedCards.get(0);
		int rank = nextCard.getRank();
		float safety = 1;
		// Probability that my partner has all the lower cards
		for (int i = 0; i < rank - 1; i++)
			safety /= 3;
		return safety;
	}

	private float safetyOfStich(int numberOfCards, int higherCards, Card nextCard, Card lastCard) {
		int numberOfCardsBetween = lastCard.getRank() - nextCard.getRank() - 1;
		// Have the next-higher card => probability to stich is the same as with the next higher card
		if (numberOfCardsBetween == 0)
			return 1;
			// Probability to stich is 1 - the probability, that enemy has a higher card + enough cards to discard
		else
			return 1 - ((float) 2 / 3 * enemenyHasNoMoreCards(numberOfCards, higherCards, numberOfCardsBetween));
	}

	private float enemenyHasNoMoreCards(int numberOfMyCards, int higherCards, int numberOfCardsBetween) {
		float estimate = 1;
		int otherColorCards = 9 - numberOfMyCards - 1;
		int otherCards = 27 - 1;
		// Only rough estimate of the probability, that a player of the other team has enough cards to discard (i.e. I
		// have an Ace and King, but he has 6 and 7 so can discard those invaluable cards
		estimate *= (float) (otherColorCards) / otherCards;
		estimate *= factorial(otherColorCards-1);
		for (int i = 0; i < higherCards; i++) {
			otherColorCards--;
			estimate *= (float) (otherColorCards) / otherCards;
		}
		// Estimate of a mathematician: if the number of ranks between the last one and this one is increased by one,
		// the probability of me making the Stich is roughly halved.
		for (int i = 0; i < numberOfCardsBetween; i++)
			estimate *= 0.45;
		if (estimate > 1)
		    estimate = 0.8f;
		if (estimate > 0)
			return estimate;
		else
			return 0;
	}

	private float factorial(int n) {
		if (n == 1 || n == 0)
			return 1;
		else if (n < 0)
			return 0;
		else
			return n * factorial(n - 1);
	}

	public int rateUndeufeColor(Set<Card> cards, Color color) {
		// Get the cards in ascending order
		List<Card> sortedCards = sortCardsOfColorAscending(cards, color);
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
		// 1 Stich entspricht 20 ratingPoints
		float rating = safety * 20;
		// remove the last card tested
		sortedCards.remove(lastCard);
		while (sortedCards.size() > 0) {
			// The next card to be tested
			Card nextCard = sortedCards.get(0);
			// Estimate how safe you Stich with that card
			safety *= safetyOfStich(numberOfMyCards, lowerCards, nextCard, lastCard);
			// How safe is the Stich? * Stichvalue
			rating += safety * 20;
			sortedCards.remove(0);
			// One card is higher than the last
			lowerCards++;
			lastCard = nextCard;
		}
		return (int) Math.ceil(rating);
	}

	@Override
	public Card chooseCard(Set<Card> availableCards, GameSession session) {
		try {
			final long startTime = System.currentTimeMillis();
			long time = MAX_THINKING_TIME;
			if (session.isFirstMove())
				time -= 50;
			final long endingTime = startTime + time;
			printCards(availableCards);
			final Game game = session.getCurrentGame();
			final Set<Card> possibleCards = JassHelper.getPossibleCards(availableCards, game);

			if (possibleCards.isEmpty())
				System.err.println("We have a serious problem! No possible card to play!");

			if (possibleCards.size() == 1)
				for (Card card : possibleCards) {
					System.out.println("Only one possible card to play: " + card + "\n\n");
					return card;
				}

			Card card = JassHelper.getRandomCard(possibleCards, game);

			System.out.println("Thinking now...");
			try {
				final Card mctsCard = MCTSHelper.getCard(availableCards, game, endingTime, PARALLELISATION_ENABLED);
				if (possibleCards.contains(card)) {
					System.out.println("Chose Card based on MCTS, Hurra!");
					card = mctsCard;
				} else
					System.out.println("Card chosen not in available cards. Had to choose random card, Damn it!");
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Something went wrong. Had to choose random card, Damn it!");
			}

			final long endTime = (System.currentTimeMillis() - startTime);
			System.out.println("Total time for move: " + endTime + "ms");
			System.out.println("Played " + card + " out of possible Cards " + possibleCards + " out of available Cards " + availableCards + "\n\n");
			assert card != null;
			assert possibleCards.contains(card);
			return card;
		}
		catch (Exception e) {
			e.printStackTrace();
			return new RandomJassStrategy().chooseCard(availableCards, session);
		}
	}

	private void printCards(Set<Card> availableCards) {
		System.out.println("Hi there! I am JassTheRipper and these are my cards: " + availableCards);
	}
}
