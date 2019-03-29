package com.zuehlke.jasschallenge.client.game.strategy.mcts;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.CardSelectionHelper;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.JassHelper;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Board;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.CallLocation;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Move;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;


/**
 * Created by joelniklaus on 06.05.17.
 */
public class JassBoard implements Board, Serializable {

	private final Set<Card> availableCards;
	private Game game;
	private final GameSession gameSession;

	private boolean shifted;
	private boolean isChoosingTrumpf;

	public static final Logger logger = LoggerFactory.getLogger(JassBoard.class);


	/**
	 * Constructs a new JassBoard based on a game session. The game session is needed for choosing a trumpf.
	 *
	 * @param availableCards
	 * @param gameSession
	 * @param newRandomCards
	 * @param isChoosingTrumpf
	 * @param shifted
	 */
	public JassBoard(Set<Card> availableCards, GameSession gameSession, boolean newRandomCards, boolean isChoosingTrumpf, boolean shifted) {
		this.availableCards = EnumSet.copyOf(availableCards);
		this.isChoosingTrumpf = isChoosingTrumpf;
		this.shifted = shifted;

		if (isChoosingTrumpf) {
			this.gameSession = new GameSession(gameSession);
			this.game = this.gameSession.getCurrentGame();
			if (newRandomCards)
				distributeCardsForPlayers(this.availableCards, this.gameSession);
		} else {
			this.gameSession = null;
			this.game = new Game(gameSession.getCurrentGame());
			if (newRandomCards)
				CardKnowledgeBase.sampleCardDeterminizationToPlayers(this.game, this.availableCards);
		}
	}

	/**
	 * Constructs a new JassBoard based on a game. If the flag is set, deals new random cards to the players.
	 *
	 * @param availableCards
	 * @param game
	 * @param newRandomCards
	 */
	JassBoard(Set<Card> availableCards, Game game, boolean newRandomCards) {
		this.availableCards = EnumSet.copyOf(availableCards);

		this.gameSession = null;
		this.game = new Game(game);
		// INFO: The version with copy constructors is almost factor 100 more efficient than the fastest other version
		//this.game = (Game) DeepCopy.copy(game);
		//this.game = (Game) new Cloner().deepClone(game);
		//this.game = ObjectCloner.deepCopySerialization(game);
		//this.game = SerializationUtils.clone(game);

		if (newRandomCards)
			CardKnowledgeBase.sampleCardDeterminizationToPlayers(this.game, this.availableCards);

	}

	/**
	 * Distribute the unknown cards to the other players at the beginning of the game, when a player is choosing a trumpf.
	 *
	 * @param availableCards
	 * @param gameSession
	 */
	private static void distributeCardsForPlayers(Set<Card> availableCards, GameSession gameSession) {
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
	 * Add randomized available cards for the other players based on already played cards
	 *
	 * @param availableCards
	 * @deprecated
	 */
	private static void distributeCardsForPlayers(Set<Card> availableCards, Game game) {
		final Player currentPlayer = game.getCurrentPlayer();
		currentPlayer.setCards(EnumSet.copyOf(availableCards));
		final Round round = game.getCurrentRound();
		final List<Player> players = round.getPlayingOrder().getPlayersInInitialPlayingOrder();
		Set<Card> remainingCards = getRemainingCards(availableCards, game);
		final double numberOfCards = remainingCards.size() / 3.0; // rounds down the number

		for (Player player : players) {
			int numberOfCardsToAdd;
			if (!player.equals(currentPlayer)) { // randomize cards for the other players
				if (round.hasPlayerAlreadyPlayed(player))
					numberOfCardsToAdd = (int) Math.floor(numberOfCards);
				else
					numberOfCardsToAdd = (int) Math.ceil(numberOfCards);

				// Make certain cards unavailable (when one player did not follow suit)
				Set<Card> possibleCardsForPlayer = EnumSet.copyOf(remainingCards);
				Set<Card> impossibleCardsForPlayer = CardKnowledgeBase.getImpossibleCardsForPlayer(game, player);
				// TODO Like this it may not be able to estimate the last player's cards well. Try to find better solution. --> CardKnowledgeBase
				if (remainingCards.size() - impossibleCardsForPlayer.size() >= numberOfCardsToAdd)
					possibleCardsForPlayer.removeAll(impossibleCardsForPlayer);

				Set<Card> cards = pickRandomSubSet(possibleCardsForPlayer, numberOfCardsToAdd);
				player.setCards(cards);

				if (!remainingCards.removeAll(cards))
					JassHelper.logger.debug("Could not remove picked cards from remaining cards");
				assert !remainingCards.containsAll(cards);
			}

		}
		assert remainingCards.isEmpty();
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

	/**
	 * Get the cards remaining to be split up on the other players.
	 * All cards - already played cards - available cards
	 *
	 * @param availableCards
	 * @return
	 */
	public static Set<Card> getRemainingCards(Set<Card> availableCards, Game game) {
		Set<Card> cards = EnumSet.allOf(Card.class);
		assert cards.size() == 36;
		cards.removeAll(availableCards);
		Set<Card> alreadyPlayedCards = game.getAlreadyPlayedCards();
		Round round = game.getCurrentRound();
		assert alreadyPlayedCards.size() == round.getRoundNumber() * 4 + round.getPlayedCards().size();
		cards.removeAll(alreadyPlayedCards);
		return cards;
	}

	/**
	 * Reconstruct Game but add known random cards for players.
	 *
	 * @return
	 */
	@Override
	public Board duplicate(boolean newRandomCards) {
		if (isChoosingTrumpf)
			return new JassBoard(availableCards, gameSession, newRandomCards, true, shifted);
		return new JassBoard(availableCards, game, newRandomCards);
	}

	/**
	 * Puts together a list of moves containing possible (or reduced to only sensible) cards to play.
	 *
	 * @param location
	 * @return
	 */
	@Override
	public List<Move> getMoves(CallLocation location) {
		ArrayList<Move> moves = new ArrayList<>();

		if (isChoosingTrumpf) {
			Player player = gameSession.getCurrentPlayer();

			List<Mode> availableModes = Mode.allModes();
			if (shifted) {
				availableModes.remove(Mode.shift());
				player = gameSession.getPartnerOfPlayer(player);
			}
			for (Mode mode : availableModes) {
				moves.add(new TrumpfMove(player, mode));
			}
		} else {
			final Player player = game.getCurrentPlayer();


			Set<Card> possibleCards = CardSelectionHelper.getCardsPossibleToPlay(EnumSet.copyOf(player.getCards()), game);

			assert (possibleCards.size() > 0);

			try {
				//logger.info("Possible cards before refining: " + possibleCards);
				//possibleCards = CardSelectionHelper.refineCardsWithJassKnowledge(possibleCards, game);
				//logger.info("Possible cards after refining: " + possibleCards);
			} catch (Exception e) {
				logger.debug("{}", e);
				logger.info("Could not refine cards with Jass Knowledge. Just considering all possible cards now");
			}

			assert !possibleCards.isEmpty();

			for (Card card : possibleCards)
				moves.add(new CardMove(player, card));
			assert (!moves.isEmpty());
		}
		assert !moves.isEmpty();
		return moves;
	}


	/**
	 * Simulate game here
	 *
	 * @param move
	 */
	@Override
	public void makeMove(Move move) {
		assert move != null;

		if (isChoosingTrumpf) {
			assert move instanceof TrumpfMove;
			final TrumpfMove trumpfMove = (TrumpfMove) move;
			assert trumpfMove != null;

			Mode mode = trumpfMove.getChosenTrumpf();
			if (mode.equals(Mode.shift())) {
				//logger.debug("Shifted");
				this.shifted = true;
			} else {
				//logger.debug("Started game with trumpf {}", mode);
				this.gameSession.startNewGame(mode, shifted);
				this.isChoosingTrumpf = false;
				assert gameSession.getCurrentGame() != null;
				this.game = gameSession.getCurrentGame();
				try {
					CardKnowledgeBase.sampleCardDeterminizationToPlayers(this.game, this.availableCards);
				} catch (Exception e) {
					logger.debug("{}", e);
				}
			}
		} else {
			Player player = game.getCurrentPlayer();

			assert move instanceof CardMove;
			// We can do that because we are only creating CardMoves
			final CardMove cardMove = (CardMove) move;

			assert cardMove != null;

			assert cardMove.getPlayer().equals(player);

			player.getCards().remove((cardMove).getPlayedCard());

			game.makeMove(cardMove);

			if (game.getCurrentRound().roundFinished()) {
				game.startNextRound();

				/* This makes chooseTrumpf tests break sometimes
				Round round = game.getCurrentRound();

				if (!game.gameFinished()) {
					assert round.getRoundNumber() < 9;

					for (Player currentPlayer : round.getPlayingOrder().getPlayersInInitialPlayingOrder()) {
						logger.debug("currentPlayer {}", currentPlayer);
						logger.debug("round {}", round);
						assert currentPlayer.getCards().size() == 9 - round.getRoundNumber();
					}
				}
				*/
			}
		}
	}

	@Override
	public int getQuantityOfPlayers() {
		return 4;
	}

	@Override
	public int getCurrentPlayer() {
		if (isChoosingTrumpf) {
			Player currentPlayer = gameSession.getGameStartingPlayerOrder().getCurrentPlayer();
			if (shifted)
				return gameSession.getPartnerOfPlayer(currentPlayer).getSeatId();
			return currentPlayer.getSeatId();
		}
		assert game != null;
		return game.getCurrentPlayer().getSeatId();
	}

	@Override
	public boolean gameOver() {
		if (isChoosingTrumpf)
			return false;
		assert game != null;
		return game.gameFinished();
	}

	@Override
	public double[] getScore() {
		assert game != null;

		double[] score = new double[getQuantityOfPlayers()];
		Result result = game.getResult();
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();
		for (Player player : order.getPlayersInInitialPlayingOrder())
			score[player.getSeatId()] = result.getTeamScore(player);

		return score;
	}

	/*
	 * This method is not used by this game, but at least
	 * a function body is required to fulfill the Board
	 * interface contract.
	 */
	public double[] getMoveWeights() {
		// TODO give high weights for good choices and low weights for bad choices. So in random choosing of moves good moves are favoured.
		return new double[game.getCurrentPlayer().getCards().size()];
	}
}
