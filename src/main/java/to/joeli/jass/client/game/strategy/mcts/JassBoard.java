package to.joeli.jass.client.game.strategy.mcts;

import to.joeli.jass.client.game.*;
import to.joeli.jass.client.game.strategy.helpers.CardKnowledgeBase;
import to.joeli.jass.client.game.strategy.helpers.CardSelectionHelper;
import to.joeli.jass.client.game.strategy.helpers.PerfectInformationGameSolver;
import to.joeli.jass.client.game.strategy.helpers.TrumpfSelectionHelper;
import to.joeli.jass.client.game.strategy.mcts.src.Board;
import to.joeli.jass.client.game.strategy.mcts.src.CallLocation;
import to.joeli.jass.client.game.strategy.mcts.src.Move;
import to.joeli.jass.client.game.strategy.training.Arena;
import to.joeli.jass.client.game.strategy.training.CardsEstimator;
import to.joeli.jass.client.game.strategy.training.ScoreEstimator;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Created by joelniklaus on 06.05.17.
 */
public class JassBoard implements Board {

	private final Set<Card> availableCards; // NOTE: should only be used in duplicating. Use player.getCards() otherwise
	private GameSession gameSession;
	private boolean shifted;
	private Game game;

	// The neural network of the player choosing the move at the beginning. If null -> use random playout instead
	private final ScoreEstimator scoreEstimator;
	// The neural netowrk of the player estimating the hidden cards of the other players. If null -> only use heuristics
	private final CardsEstimator cardsEstimator;

	public static final Logger logger = LoggerFactory.getLogger(JassBoard.class);

	private JassBoard(Set<Card> availableCards, GameSession gameSession, boolean shifted, Game game, ScoreEstimator scoreEstimator, CardsEstimator cardsEstimator) {
		this.availableCards = availableCards;
		this.gameSession = gameSession;
		this.shifted = shifted;
		this.game = game;
		this.scoreEstimator = scoreEstimator;
		this.cardsEstimator = cardsEstimator;
	}

	public static JassBoard constructTrumpfSelectionJassBoard(Set<Card> availableCards, GameSession gameSession, boolean shifted, ScoreEstimator scoreEstimator, CardsEstimator cardsEstimator) {
		JassBoard jassBoard = new JassBoard(EnumSet.copyOf(availableCards), new GameSession(gameSession), shifted, null, scoreEstimator, cardsEstimator);
		jassBoard.sampleCardDeterminizationToPlayersInTrumpfSelection();
		return jassBoard;
	}

	public static JassBoard constructCardSelectionJassBoard(Set<Card> availableCards, Game game, ScoreEstimator scoreEstimator, CardsEstimator cardsEstimator) {
		return new JassBoard(EnumSet.copyOf(availableCards), null, false, new Game(game), scoreEstimator, cardsEstimator);
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
			CardKnowledgeBase.sampleCardDeterminizationToPlayers(this.game, this.availableCards, cardsEstimator);
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
	 * Duplicates the board and determinizes the cards of the other players.
	 *
	 * @return
	 */
	@Override
	public Board duplicate(boolean newRandomCards) {
		if (isChoosingTrumpf())
			return constructTrumpfSelectionJassBoard(availableCards, gameSession, shifted, scoreEstimator, cardsEstimator);

		JassBoard jassBoard = constructCardSelectionJassBoard(availableCards, game, scoreEstimator, cardsEstimator);
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

			assert !possibleCards.isEmpty();

			// INFO: This would be pruning for cards. At the moment we do not want to do this.
			// It could be a possibility later on if we see that the bot still plays badly in the first 1-3 moves of a game.
			/*
			try {
				logger.info("Possible cards before refining: " + possibleCards);
				possibleCards = CardSelectionHelper.refineCardsWithJassKnowledge(possibleCards, game);
				logger.info("Possible cards after refining: " + possibleCards);
			} catch (Exception e) {
				logger.debug("{}", e);
				logger.info("Could not refine cards with Jass Knowledge. Just considering all possible cards now");
			}
			*/

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

			player.onMoveMade(cardMove);
			game.makeMove(cardMove);

			if (game.getCurrentRound().roundFinished()) {
				game.startNextRound();

				/* This makes chooseTrumpf tests break sometimes
				Round round = game.getCurrentRound();

				if (!game.gameFinished()) {
					assert round.getRoundNumber() < 9;

					for (Player currentPlayer : round.getPlayingOrder().getPlayersInInitialOrder()) {
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
			final List<Move> moves = getMoves(CallLocation.PLAYOUT); // This must only be called in playout!
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

		return scoreEstimator != null; // if there is a neural network set for the choosing player
	}

	@Override
	public double[] estimateScore() {
		double score = scoreEstimator.predictScore(game);
		// logger.info("The neural network predicted a score of " + score);
		double[] scores = new double[getQuantityOfPlayers()];
		for (Player player : game.getPlayers())
			if (player.equals(game.getCurrentPlayer()) || player.equals(game.getPartnerOfPlayer(game.getCurrentPlayer())))
				scores[player.getSeatId()] = score;
			else
				scores[player.getSeatId()] = Math.max(Arena.TOTAL_POINTS - score, 0); // Matchbonus disregarded for simplicity
		return scores;
	}
}
