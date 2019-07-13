package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.google.common.collect.ImmutableMap;
import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.training.CardsEstimator;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.CardValue;
import com.zuehlke.jasschallenge.game.cards.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
		Player currentPlayer = gameSession.getTrumpfSelectingPlayer();
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
	public static void sampleCardDeterminizationToPlayers(Game game, Set<Card> availableCards, CardsEstimator cardsEstimator) {
		// INFO: This method should only be used when new cards are distributed (at the beginning of a move).
		for (Player player : game.getPlayers()) {
			assert player.getCards().isEmpty();
		}

		Map<Card, Distribution> cardKnowledge;
		if (cardsEstimator == null) {
			cardKnowledge = CardKnowledgeBase.initCardDistributionMap(game, availableCards);
		} else {
			cardKnowledge = cardsEstimator.predictCardDistribution(game, availableCards);
		}

		//game.getCurrentPlayer().setCards(EnumSet.copyOf(availableCards));

		// TODO extend this with a belief distribution: we can assume that a player has/has not some cards based on the game.
		//  For example when a player did not take a very valuable stich he probably does not have any trumpfs or higher cards of the given suit.


		while (cardsNeedToBeDistributed(cardKnowledge)) {
			//AtomicBoolean noConflictSoFar = new AtomicBoolean(true);
			getStreamWithNotSampledDistributions(cardKnowledge)
					.min(Comparator.comparingInt(o -> o.getValue().getNumEvents())) // Select the card with the least possible players
					.ifPresent(cardDistributionEntry -> {
						Card card = cardDistributionEntry.getKey();
						Player player = cardDistributionEntry.getValue().sample(); // Select a player at random based on the probabilities of the distribution
						player.addCard(card);
						assert player.getCards().size() <= 9;
						// Set distribution of already distributed card to sampled so it is not selected anymore in future runs
						cardDistributionEntry.getValue().setSampled(true);

						// As soon as a player has enough cards, delete him from all remaining distributions
						final double numberOfCards = getRemainingCards(availableCards, game).size() / 3.0; // rounds down the number
						if (player.getCards().size() == getNumberOfCardsToAdd(game, numberOfCards, player)) {
							getStreamWithNotSampledDistributions(cardKnowledge)
									.filter(entry -> entry.getValue().hasPlayer(player))
									.forEach(entry -> {
										entry.getValue().deleteEventAndReBalance(player);
									/*
									noConflictSoFar.set(entry.getValue().deleteEventAndReBalance(player));
										if (!noConflictSoFar.get()) {
											//logger.debug("{}", card);
											//logger.debug("{}", player);
										}
										*/
									});
						}
					});
			/* NOTE: It seems to be stable enough so we can make this simplification here
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
			*/
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
	public static Set<Card> pickRandomSubSet(Set<Card> cards, int numberOfCards) {
		assert (numberOfCards > 0 || numberOfCards <= 9);
		List<Card> listOfCards = new LinkedList<>(cards);
		assert numberOfCards <= listOfCards.size();
		Collections.shuffle(listOfCards);
		List<Card> randomSublist = listOfCards.subList(0, numberOfCards);
		Set<Card> randomSubSet = EnumSet.copyOf(randomSublist);
		assert (cards.containsAll(randomSubSet));
		return randomSubSet;
	}

	public static Map<Card, Distribution> initCardDistributionMap(Game game, Set<Card> availableCards) {
		return initCardDistributionMap(game, availableCards, null);
	}

	/**
	 * Initializes a basic card distribution based only on the information we know for sure. Only certainties are encoded.
	 * This could be extended with rule based knowledge or with learning based approaches.
	 *
	 * @param game
	 * @param availableCards
	 * @return
	 */
	public static Map<Card, Distribution> initCardDistributionMap(Game game, Set<Card> availableCards, List<Color> colors) {
		Map<Card, Distribution> cardKnowledge = new EnumMap<>(Card.class);

		// Set simple distributions for the cards of the current player

		availableCards.forEach(card -> {
			final Card respectiveCard = DataAugmentationHelper.getRespectiveCard(card, colors);
			cardKnowledge.put(respectiveCard, new Distribution(ImmutableMap.of(game.getCurrentPlayer(), 1d), false));
		});


		// Init remaining unknown cards with equal probability for the other players
		for (Card card : getRemainingCards(availableCards, game)) {
			Map<Player, Double> probabilitiesMap = new HashMap<>();
			List<Player> players = new ArrayList<>(game.getPlayers());
			players.remove(game.getCurrentPlayer());
			for (Player player : players) {
				probabilitiesMap.put(player, 1.0 / players.size());
			}
			cardKnowledge.put(DataAugmentationHelper.getRespectiveCard(card, colors), new Distribution(probabilitiesMap, false));
		}

		deleteImpossibleCardsFromCardKnowledge(game, colors, cardKnowledge);

		final List<Move> historyMoves = DataAugmentationHelper.getRespectiveMoves(game.getAlreadyPlayedMovesInOrder(), colors);
		// Add already played moves to card knowledge
		historyMoves.forEach(move -> cardKnowledge.put(move.getPlayedCard(), new Distribution(Collections.singletonMap(move.getPlayer(), 1d), true)));

		return cardKnowledge;
	}


	private static void deleteImpossibleCardsFromCardKnowledge(Game game, List<Color> colors, Map<Card, Distribution> cardKnowledge) {
		for (Player player : game.getPlayers()) {
			Set<Card> impossibleCardsForPlayer = getImpossibleCardsForPlayer(game, player);
			for (Card card : impossibleCardsForPlayer) {
				card = DataAugmentationHelper.getRespectiveCard(card, colors);
				if (cardKnowledge.containsKey(card))
					cardKnowledge.get(card).deleteEventAndReBalance(player);
			}
		}
	}


	private static int getNumberOfCardsToAdd(Game game, double numberOfCards, Player player) {
		if (game.getCurrentRound().hasPlayerAlreadyPlayed(player))
			return (int) Math.floor(numberOfCards);
		return (int) Math.ceil(numberOfCards);
	}

	private static Stream<Map.Entry<Card, Distribution>> getStreamWithNotSampledDistributions(Map<Card, Distribution> cardDistributionMap) {
		return cardDistributionMap.entrySet().stream().filter(entry -> !entry.getValue().isSampled());
	}

	private static boolean cardsNeedToBeDistributed(Map<Card, Distribution> cardDistributionMap) {
		return getStreamWithNotSampledDistributions(cardDistributionMap).count() > 0;
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
