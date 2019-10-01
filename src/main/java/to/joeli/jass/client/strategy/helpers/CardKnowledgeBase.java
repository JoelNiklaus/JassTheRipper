package to.joeli.jass.client.strategy.helpers;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.client.game.*;
import to.joeli.jass.client.strategy.training.networks.CardsEstimator;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.CardValue;
import to.joeli.jass.game.cards.Color;

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
		if (remainingCards.isEmpty()) throw new AssertionError();
		for (Player player : gameSession.getPlayersInInitialPlayingOrder())
			if (!player.equals(currentPlayer)) {
				Set<Card> cards = pickRandomSubSet(remainingCards, 9);
				player.setCards(cards);
				remainingCards.removeAll(cards);
			}
		if (!remainingCards.isEmpty()) throw new AssertionError();
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

		/*
		boolean inPerfectInformationSetting = false; // Determines if the method is invoked from a perfect information setting (aka from the Arena)
		int[] numCards = new int[4];
		for (int i = 0; i < 4; i++) {
			numCards[i] = game.getPlayers().get(i).getCards().size();
			if (numCards[i] > 0)
				inPerfectInformationSetting = true;
		}
		*/

		Map<Card, Distribution> cardKnowledge;
		if (cardsEstimator == null) {
			cardKnowledge = CardKnowledgeBase.initCardKnowledge(game, availableCards);
		} else {
			// The cards estimator extends this with a belief distribution: we can assume that a player has/has not some cards based on the game.
			// For example when a player did not take a very valuable stich he probably does not have any trumpfs or higher cards of the given suit.
			// This could also be solved with rule based approaches but we hope that the learning based one is superior
			cardKnowledge = cardsEstimator.predictCardDistribution(game, availableCards);
		}

		// Delete all the cards of the players so we can distribute the determinization
		game.getPlayers().forEach(player -> player.setCards(EnumSet.noneOf(Card.class)));

		while (cardsNeedToBeDistributed(cardKnowledge)) {
			getStreamWithNotSampledDistributions(cardKnowledge)
					.min(Comparator.comparingInt(entry -> entry.getValue().size())) // Select the card with the least possible players
					.ifPresent(entry -> {
						Card card = entry.getKey();
						Player player = entry.getValue().sample(); // Select a player at random based on the probabilities of the distribution
						player.addCard(card);
						// Set distribution of already distributed card to sampled so it is not selected anymore in future runs
						entry.getValue().setSampled(true);

						deletePlayerFromRemainingDistributions(game, availableCards, cardKnowledge, player);
					});
		}

		/*
		if (inPerfectInformationSetting)
			for (int i = 0; i < 4; i++) {
				if (game.getPlayers().get(i).getCards().size() != numCards[i]) {
					logger.error("Some weird coincidence made it impossible to sample the cards for the other players validly");
				}
			}
		*/
	}

	/**
	 * As soon as a player has enough cards, delete him from all remaining distributions
	 *
	 * @param game
	 * @param availableCards
	 * @param cardKnowledge
	 * @param player
	 */
	private static void deletePlayerFromRemainingDistributions(Game game, Set<Card> availableCards, Map<Card, Distribution> cardKnowledge, Player player) {
		final double numberOfCards = getRemainingCards(availableCards, game).size() / 3.0;
		if (player.getCards().size() == getNumberOfCardsToAdd(game, numberOfCards, player)) {
			getStreamWithNotSampledDistributions(cardKnowledge)
					.filter(entry -> entry.getValue().hasPlayer(player))
					.forEach(entry -> entry.getValue().deleteEventAndReBalance(player));
		}
	}

	/**
	 * Picks a random sub set out of the given cards with the given size.
	 *
	 * @param cards
	 * @param numberOfCards
	 * @return
	 */
	public static Set<Card> pickRandomSubSet(Set<Card> cards, int numberOfCards) {
		if ((numberOfCards <= 0 || numberOfCards > 9)) throw new AssertionError();
		if (numberOfCards > cards.size()) throw new AssertionError();
		List<Card> listOfCards = new LinkedList<>(cards);
		Collections.shuffle(listOfCards);
		List<Card> randomSublist = listOfCards.subList(0, numberOfCards);
		Set<Card> randomSubSet = EnumSet.copyOf(randomSublist);
		if ((!cards.containsAll(randomSubSet))) throw new AssertionError();
		return randomSubSet;
	}

	public static Map<Card, Distribution> initCardKnowledge(Game game, Set<Card> availableCards) {
		return initCardKnowledge(game, availableCards, null);
	}

	/**
	 * Initializes a basic card distribution based only on the information we know for sure. Only certainties are encoded.
	 * This could be extended with rule based knowledge or with learning based approaches.
	 *
	 * @param game
	 * @param availableCards
	 * @return
	 */
	public static Map<Card, Distribution> initCardKnowledge(Game game, Set<Card> availableCards, List<Color> colors) {
		assert !availableCards.isEmpty();

		Map<Card, Distribution> cardKnowledge = new EnumMap<>(Card.class);

		// Set simple distributions for the cards of the current player
		availableCards.forEach(card -> {
			final Card respectiveCard = DataAugmentationHelper.getRespectiveCard(card, colors);
			cardKnowledge.put(respectiveCard, new Distribution(ImmutableMap.of(game.getCurrentPlayer(), 1f), false));
		});


		// Init remaining unknown cards with equal probability for the other players
		for (Card card : getRemainingCards(availableCards, game)) {
			Map<Player, Float> probabilitiesMap = new HashMap<>();
			List<Player> players = new ArrayList<>(game.getPlayers());
			players.remove(game.getCurrentPlayer());
			for (Player player : players) {
				probabilitiesMap.put(player, 1.0f / players.size());
			}
			cardKnowledge.put(DataAugmentationHelper.getRespectiveCard(card, colors), new Distribution(probabilitiesMap, false));
		}

		deleteImpossibleCardsFromCardKnowledge(game, colors, cardKnowledge);

		final List<Move> historyMoves = DataAugmentationHelper.getRespectiveMoves(game.getAlreadyPlayedMovesInOrder(), colors);
		// Add already played moves to card knowledge
		historyMoves.forEach(move -> cardKnowledge.put(move.getPlayedCard(), new Distribution(Collections.singletonMap(move.getPlayer(), 1f), true)));

		return cardKnowledge;
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
	 * Deletes all the cards which are not possible to be held by players given the previous rounds.
	 *
	 * @param game
	 * @param colors
	 * @param cardKnowledge
	 */
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
		game.getPreviousRounds().forEach(round -> addImpossibleCardsFromRoundForPlayer(impossibleCards, round, player));
		addImpossibleCardsFromRoundForPlayer(impossibleCards, game.getCurrentRound(), player);
		return impossibleCards;
	}

	/**
	 * Adds cards which are impossible for a player to hold based on a given round
	 *
	 * @param impossibleCards
	 * @param round
	 * @param player
	 */
	private static void addImpossibleCardsFromRoundForPlayer(Set<Card> impossibleCards, Round round, Player player) {
		if (!round.hasPlayerAlreadyPlayed(player))
			return;
		Color playerCardColor = round.getCardOfPlayer(player).getColor();
		Color trumpfColor = round.getMode().getTrumpfColor();
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

	/**
	 * Determines if it is allowed by the rules to play a card based on the trumpf color and the color of the leading card
	 *
	 * @param trumpfColor
	 * @param leadingColor
	 * @param card
	 * @return
	 */
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
		if (cards.size() != 36) throw new AssertionError();
		cards.removeAll(availableCards);
		Set<Card> alreadyPlayedCards = game.getAlreadyPlayedCards();
		Round round = game.getCurrentRound();
		if (alreadyPlayedCards.size() != round.getRoundNumber() * 4 + round.getPlayedCards().size())
			throw new AssertionError();
		cards.removeAll(alreadyPlayedCards);
		return cards;
	}

}
