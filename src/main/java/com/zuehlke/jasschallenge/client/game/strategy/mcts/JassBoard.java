package com.zuehlke.jasschallenge.client.game.strategy.mcts;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.JassTheRipperJassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.CardSelectionHelper;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.TrumpfSelectionHelper;
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

	private final Set<Card> availableCards; // NOTE: should only be used in duplicating. Use player.getCards() otherwise
	private GameSession gameSession;
	private boolean shifted;
	private Game game;

	// The neural network of the player choosing the move at the beginning. If null -> use random playout instead
	private final NeuralNetwork neuralNetwork;

	public static final Logger logger = LoggerFactory.getLogger(JassBoard.class);

	private JassBoard(Set<Card> availableCards, GameSession gameSession, boolean shifted, Game game, NeuralNetwork neuralNetwork) {
		this.availableCards = availableCards;
		this.gameSession = gameSession;
		this.shifted = shifted;
		this.game = game;
		this.neuralNetwork = neuralNetwork;
	}

	public static JassBoard constructTrumpfSelectionJassBoard(Set<Card> availableCards, GameSession gameSession, boolean shifted, NeuralNetwork neuralNetwork) {
		JassBoard jassBoard = new JassBoard(EnumSet.copyOf(availableCards), new GameSession(gameSession), shifted, null, neuralNetwork);
		jassBoard.sampleCardDeterminizationToPlayersInTrumpfSelection();
		return jassBoard;
	}

	public static JassBoard constructCardSelectionJassBoard(Set<Card> availableCards, Game game, NeuralNetwork neuralNetwork) {
		// INFO: The version with copy constructors is almost factor 100 more efficient than the fastest other version
		//this.game = (Game) DeepCopy.copy(game);
		//this.game = (Game) new Cloner().deepClone(game);
		//this.game = ObjectCloner.deepCopySerialization(game);
		//this.game = SerializationUtils.clone(game);
		return new JassBoard(EnumSet.copyOf(availableCards), null, false, new Game(game), neuralNetwork);
	}

	/**
	 * Specifies if we are in the trumpf selection phase or in the card selection phase
	 *
	 * @return
	 */
	private boolean isChoosingTrumpf() {
		return this.gameSession != null && this.game == null;
	}


	void sampleCardDeterminizationToPlayersInTrumpfSelection() {
		CardKnowledgeBase.sampleCardDeterminizationToPlayers(this.gameSession, this.availableCards);
	}

	void sampleCardDeterminizationToPlayersInCardPlay() {
		if (cardsAreNotDistributedYet())
			CardKnowledgeBase.sampleCardDeterminizationToPlayers(this.game, this.availableCards);
	}

	/**
	 * Checks if the players already have cards.
	 * If they do, we are in a Trumpf selection tree, where the cards are already distributed.
	 * If they don't, we are in a card selection tree where we the player do not have cards yet.
	 *
	 * @return
	 */
	private boolean cardsAreNotDistributedYet() {
		for (Player player : this.game.getPlayers()) {
			if (player.getCards().isEmpty())
				return true;
		}
		return false;
	}

	/**
	 * Duplicates the board and determinizes the the cards of the other players.
	 *
	 * @return
	 */
	@Override
	public Board duplicate(boolean newRandomCards) {
		if (isChoosingTrumpf())
			return constructTrumpfSelectionJassBoard(availableCards, gameSession, shifted, neuralNetwork);

		JassBoard jassBoard = constructCardSelectionJassBoard(availableCards, game, neuralNetwork);
		if (newRandomCards)
			jassBoard.sampleCardDeterminizationToPlayersInCardPlay();
		return jassBoard;
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

		if (isChoosingTrumpf()) {
			Player player = gameSession.getTrumpfSelectingPlayer();
			if (shifted)
				player = gameSession.getPartnerOfPlayer(player);

			// INFO: This performs pruning: removes all the trumpfs which are obviously bad, so that more time can be spent on the good trumpfs
			List<Mode> topTrumpfChoices = TrumpfSelectionHelper.getTopTrumpfChoices(player.getCards(), shifted);

			for (Mode mode : topTrumpfChoices)
				moves.add(new TrumpfMove(player, mode));
		} else {
			final Player player = game.getCurrentPlayer();

			Set<Card> possibleCards = CardSelectionHelper.getCardsPossibleToPlay(EnumSet.copyOf(player.getCards()), game);

			assert (possibleCards.size() > 0);

			// INFO: This would be pruning for cards. At the moment we do not want to do this.
			// It could be a possiblity later on if we see that the bot still plays badly in the first 1-3 moves of a game.
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

		if (isChoosingTrumpf()) {
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
				assert gameSession.getCurrentGame() != null;
				this.game = gameSession.getCurrentGame();
				this.gameSession = null; // NOTE: this is needed so that the method isChoosingTrumpf() will evaluate to false afterwards
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
		return currentPlayer().getSeatId();
	}

	private Player currentPlayer() {
		if (isChoosingTrumpf()) {
			Player trumpfSelectingPlayer = gameSession.getTrumpfSelectingPlayer();
			if (shifted)
				return gameSession.getPartnerOfPlayer(trumpfSelectingPlayer);
			return trumpfSelectingPlayer;
		}
		assert game != null;
		return game.getCurrentPlayer();
	}

	@Override
	public boolean gameOver() {
		if (isChoosingTrumpf())
			return false;
		assert game != null;
		return game.gameFinished();
	}

	@Override
	public double[] getScore() {
		assert game != null;

		double[] score = new double[getQuantityOfPlayers()];
		Result result = game.getResult();
		for (Player player : game.getPlayers())
			score[player.getSeatId()] = result.getTeamScore(player);

		return score;
	}

	/*
	 * This method is not used by this game (we do not have any chance nodes),
	 * but at least a function body is required to fulfill the Board interface contract.
	 */
	public double[] getMoveWeights() {
		return new double[game.getCurrentPlayer().getCards().size()];
	}

	@Override
	public Move getBestMove() {
		if (isChoosingTrumpf()) {
			final List<Move> moves = getMoves(CallLocation.playout); // This must only be called in playout!
			final Mode mode = TrumpfSelectionHelper.predictTrumpf(currentPlayer().getCards(), shifted);
			final Move move = new TrumpfMove(currentPlayer(), mode);
			final int bestTrumpfIndex = moves.indexOf(move);
			return moves.get(bestTrumpfIndex); // return top rated trumpf
		}

		return PerfectInformationGameSolver.getMove(game);
	}

	@Override
	public boolean hasScoreEstimator() {
		if (isChoosingTrumpf())
			return false; // So far we only estimate the score during the card play

		return neuralNetwork != null; // if there is a neural network set for the choosing player
	}

	@Override
	public double[] estimateScore() {
		double value = neuralNetwork.predictValue(game);
		// logger.info("The neural network predicted a value of " + value);
		double[] score = new double[getQuantityOfPlayers()];
		for (Player player : game.getPlayers())
			if (player.equals(game.getCurrentPlayer()) || player.equals(game.getPartnerOfPlayer(game.getCurrentPlayer())))
				score[player.getSeatId()] = value;
			else
				score[player.getSeatId()] = Math.max(157.0 - value, 0); // Matchbonus disregarded for simplicity
		return score;
	}

}
