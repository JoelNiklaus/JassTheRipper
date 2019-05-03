package com.zuehlke.jasschallenge.client.game.strategy.mcts;

import com.zuehlke.jasschallenge.client.game.Game;
import com.zuehlke.jasschallenge.client.game.GameSession;
import com.zuehlke.jasschallenge.client.game.Player;
import com.zuehlke.jasschallenge.client.game.Round;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.CardValue;
import com.zuehlke.jasschallenge.game.cards.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Can infer knowledge about which player might hold which card based on the course of a given game so far.
 * Can then sample from the given distributions to deal the remaining cards to the players.
 */
public class CardKnowledgeBase {

	public static final Logger logger = LoggerFactory.getLogger(CardKnowledgeBase.class);


	private CardKnowledgeBase() {

	}

	/**
	 * Distribute the unknown cards to the other players at the beginning of the game, when a player is choosing a trumpf.
	 * IMPORTANT: To be used before the game started, during trumpf selection!
	 *
	 * @param availableCards
	 * @param gameSession
	 */
	public static void sampleCardDeterminizationToPlayers(GameSession gameSession, Set<Card> availableCards) {
		Player currentPlayer = gameSession.getCurrentPlayer();
		currentPlayer.setCards(EnumSet.copyOf(availableCards));

		Set<Card> remainingCards = EnumSet.allOf(Card.class);
		remainingCards.removeAll(availableCards);
		assert !remainingCards.isEmpty();
		for (Player player : gameSession.getPlayersInInitialPlayingOrder())
			if (!player.equals(currentPlayer)) {
				Set<Card> cards = pickRandomSubSet(remainingCards, 9);
				player.setCards(cards);
				remainingCards.removeAll(cards);
			}
		assert remainingCards.isEmpty();
	}


	/**
	 * Samples a card determinization for a player with the given cards for the current player in a given game.
	 * If a player did not follow suit in the game so far, the player will not be distributed any cards of this suit.
	 * IMPORTANT: To be used during a game!
	 *
	 * @param game
	 * @param availableCards
	 */
	public static void sampleCardDeterminizationToPlayers(Game game, Set<Card> availableCards) {
		// INFO: This method should only be used when new cards are distributed (at the beginning of a move).
		for (Player player : game.getPlayers()) {
			assert player.getCards().isEmpty();
		}

		game.getCurrentPlayer().setCards(EnumSet.copyOf(availableCards));

		Map<Card, Distribution<Player>> cardDistributionMap = initCardDistributionMap(game, availableCards);

		// TODO extend this with a belief distribution: we can assume that a player has/has not some cards based on the game.
		//  For example when a player did not take a very valuable stich he probably does not have any trumpfs or higher cards of the given suit.

		while (cardsNeedToBeDistributed(cardDistributionMap)) {
			AtomicBoolean noConflictSoFar = new AtomicBoolean(true);
			getStreamWithNonNullDistributions(cardDistributionMap)
					.min(Comparator.comparingInt(o -> o.getValue().getNumEvents()))
					.ifPresent(cardDistributionEntry -> {
						Card card = cardDistributionEntry.getKey();
						Player player = cardDistributionEntry.getValue().sample();
						assert player != game.getCurrentPlayer();
						Set<Card> cards = EnumSet.copyOf(player.getCards());
						cards.add(card);
						player.setCards(cards);
						assert player.getCards().size() <= 9;
						// set distribution of already distributed card to null so it is not selected anymore in future runs
						cardDistributionEntry.setValue(null);

						// As soon as a player has enough cards, delete him from all distributions
						final double numberOfCards = getRemainingCards(availableCards, game).size() / 3.0; // rounds down the number
						if (cards.size() == getNumberOfCardsToAdd(game, numberOfCards, player)) {
							getStreamWithNonNullDistributions(cardDistributionMap)
									.filter(entry -> entry.getValue().hasEvent(player))
									.forEach(entry -> {
										noConflictSoFar.set(entry.getValue().deleteEventAndRebalance(player));
										if (!noConflictSoFar.get()) {
											//logger.debug("{}", card);
											//logger.debug("{}", player);
										}
									});
						}
					});
			// There has been a conflict in distributing the cards. Rollback and try again.
			if (!noConflictSoFar.get()) {
				logger.info("There has been a conflict in sampling the card determinizations for the players. Rolling back and trying again now.");
				logger.debug("{}", game);
				// Deletes the set cards from the players again.
				for (Player player : game.getPlayers()) {
					player.setCards(EnumSet.noneOf(Card.class));
				}
				sampleCardDeterminizationToPlayers(game, availableCards);
				return; // We started a new try. So do not finish the old one by continuing the while loop.
			}
		}

		for (Player player : game.getPlayers())
			assert !player.getCards().isEmpty();
	}

	/**
	 * Picks a random sub set out of the given cards with the given size.
	 *
	 * @param cards
	 * @param numberOfCards
	 * @return
	 */
	static Set<Card> pickRandomSubSet(Set<Card> cards, int numberOfCards) {
		assert (numberOfCards > 0 || numberOfCards <= 9);
		List<Card> listOfCards = new LinkedList<>(cards);
		assert numberOfCards <= listOfCards.size();
		Collections.shuffle(listOfCards);
		List<Card> randomSublist = listOfCards.subList(0, numberOfCards);
		Set<Card> randomSubSet = EnumSet.copyOf(randomSublist);
		assert (cards.containsAll(randomSubSet));
		return randomSubSet;
	}

	private static Map<Card, Distribution<Player>> initCardDistributionMap(Game game, Set<Card> availableCards) {
		Map<Card, Distribution<Player>> cardDistributionMap = new EnumMap<>(Card.class);

		for (Card card : getRemainingCards(availableCards, game)) {
			Map<Player, Double> probabilitiesMap = new HashMap<>();
			List<Player> players = new ArrayList<>(game.getPlayers());
			players.remove(game.getCurrentPlayer());
			for (Player player : players) {
				probabilitiesMap.put(player, 1.0 / players.size());
			}
			cardDistributionMap.put(card, new Distribution<>(probabilitiesMap));
		}

		for (Player player : game.getPlayers()) {
			Set<Card> impossibleCardsForPlayer = getImpossibleCardsForPlayer(game, player);
			for (Card card : impossibleCardsForPlayer) {
				if (cardDistributionMap.containsKey(card))
					cardDistributionMap.get(card).deleteEventAndRebalance(player);
			}
		}

		return cardDistributionMap;
	}

	private static int getNumberOfCardsToAdd(Game game, double numberOfCards, Player player) {
		if (game.getCurrentRound().hasPlayerAlreadyPlayed(player))
			return (int) Math.floor(numberOfCards);
		return (int) Math.ceil(numberOfCards);
	}

	private static Stream<Map.Entry<Card, Distribution<Player>>> getStreamWithNonNullDistributions(Map<Card, Distribution<Player>> cardDistributionMap) {
		return cardDistributionMap.entrySet().stream().filter(entry -> entry.getValue() != null);
	}

	private static boolean cardsNeedToBeDistributed(Map<Card, Distribution<Player>> cardDistributionMap) {
		return getStreamWithNonNullDistributions(cardDistributionMap).count() > 0;
	}

	/**
	 * Composes a set of cards which are impossible for a given player to be held at a given point in a game
	 * If player did not follow suit earlier in the game, add all cards of this suit to this set.
	 *
	 * @param game
	 * @param player
	 * @return
	 */
	public static Set<Card> getImpossibleCardsForPlayer(Game game, Player player) {
		Set<Card> impossibleCards = EnumSet.noneOf(Card.class);
		game.getPreviousRounds().forEach(round -> addImpossibleCardsForRound(game, player, impossibleCards, round));
		addImpossibleCardsForRound(game, player, impossibleCards, game.getCurrentRound());
		return impossibleCards;
	}

	private static void addImpossibleCardsForRound(Game game, Player player, Set<Card> impossibleCards, Round round) {
		if (!round.hasPlayerAlreadyPlayed(player))
			return;
		Color playerCardColor = round.getCardOfPlayer(player).getColor();
		Color trumpfColor = game.getMode().getTrumpfColor();
		Color leadingColor = round.getMoves().get(0).getPlayedCard().getColor();
		boolean playerPlayedTrumpf = playerCardColor.equals(trumpfColor);
		boolean playerFollowedSuit = playerCardColor.equals(leadingColor);
		if (!player.wasStartingPlayer(round) && !playerFollowedSuit && !playerPlayedTrumpf) {
			Set<Card> impossibleCardsToAdd = EnumSet.allOf(Card.class).stream()
					.filter(card -> !cardIsPossible(trumpfColor, leadingColor, card))
					.collect(Collectors.toSet());
			impossibleCards.addAll(impossibleCardsToAdd);
		}
	}

	private static boolean cardIsPossible(Color trumpfColor, Color leadingColor, Card card) {
		if (card.getColor().equals(leadingColor)) {
			boolean cardIsTrumpfJack = card.getColor().equals(trumpfColor) && card.getValue().equals(CardValue.JACK);
			return leadingColor.equals(trumpfColor) && cardIsTrumpfJack;
		}
		return true;
	}

	/**
	 * Get the cards remaining to be split up on the other players.
	 * All cards - already played cards - available cards
	 *
	 * @param availableCards
	 * @return
	 */
	private static Set<Card> getRemainingCards(Set<Card> availableCards, Game game) {
		Set<Card> cards = EnumSet.allOf(Card.class);
		assert cards.size() == 36;
		cards.removeAll(availableCards);
		Set<Card> alreadyPlayedCards = game.getAlreadyPlayedCards();
		Round round = game.getCurrentRound();
		assert alreadyPlayedCards.size() == round.getRoundNumber() * 4 + round.getPlayedCards().size();
		cards.removeAll(alreadyPlayedCards);
		return cards;
	}
}
