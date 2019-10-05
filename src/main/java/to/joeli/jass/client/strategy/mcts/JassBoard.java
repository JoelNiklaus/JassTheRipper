package to.joeli.jass.client.strategy.mcts;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.game.GameSession;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.client.game.Result;
import to.joeli.jass.client.strategy.helpers.CardKnowledgeBase;
import to.joeli.jass.client.strategy.helpers.CardSelectionHelper;
import to.joeli.jass.client.strategy.helpers.TrumpfSelectionHelper;
import to.joeli.jass.client.strategy.mcts.src.Board;
import to.joeli.jass.client.strategy.mcts.src.CallLocation;
import to.joeli.jass.client.strategy.mcts.src.Move;
import to.joeli.jass.client.strategy.mcts.src.PlayoutSelectionPolicy;
import to.joeli.jass.client.strategy.training.Arena;
import to.joeli.jass.client.strategy.training.networks.CardsEstimator;
import to.joeli.jass.client.strategy.training.networks.ScoreEstimator;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;


/**
 * Created by joelniklaus on 06.05.17.
 */
public class JassBoard implements Board {

	private final Set<Card> availableCards; // NOTE: should only be used in duplicating. Use player.getCards() otherwise
	private GameSession gameSession;
	private boolean shifted;
	private Game game;
	private boolean cheating; // Determines if the player knows the cards of the other players or not (used for experiments)

	// The neural network of the player choosing the move at the beginning. If null -> use random playout instead
	private final ScoreEstimator scoreEstimator;
	// The neural network of the player estimating the hidden cards of the other players. If null -> only use heuristics
	private final CardsEstimator cardsEstimator;

	public static final Logger logger = LoggerFactory.getLogger(JassBoard.class);

	private JassBoard(Set<Card> availableCards, GameSession gameSession, boolean shifted, Game game, boolean cheating, ScoreEstimator scoreEstimator, CardsEstimator cardsEstimator) {
		this.availableCards = availableCards;
		this.gameSession = gameSession;
		this.shifted = shifted;
		this.game = game;
		this.cheating = cheating;
		this.scoreEstimator = scoreEstimator;
		this.cardsEstimator = cardsEstimator;
	}

	/**
	 * Constructs a JassBoard which can be used in the trumpf selection phase. The game session is defined and the game null.
	 *
	 * @param availableCards
	 * @param gameSession
	 * @param shifted
	 * @param scoreEstimator
	 * @param cardsEstimator
	 * @return
	 */
	public static JassBoard constructTrumpfSelectionJassBoard(Set<Card> availableCards, GameSession gameSession, boolean shifted, boolean cheating, ScoreEstimator scoreEstimator, CardsEstimator cardsEstimator) {
		JassBoard jassBoard = new JassBoard(EnumSet.copyOf(availableCards), new GameSession(gameSession), shifted, null, cheating, scoreEstimator, cardsEstimator);
		jassBoard.sampleCardDeterminizationToPlayersInTrumpfSelection();
		return jassBoard;
	}

	/**
	 * Constructs a JassBoard which can be used in the card selection phase. The game session is null and the game defined.
	 *
	 * @param availableCards
	 * @param game
	 * @param scoreEstimator
	 * @param cardsEstimator
	 * @return
	 */
	public static JassBoard constructCardSelectionJassBoard(Set<Card> availableCards, Game game, boolean cheating, ScoreEstimator scoreEstimator, CardsEstimator cardsEstimator) {
		return new JassBoard(EnumSet.copyOf(availableCards), null, game.isShifted(), new Game(game), cheating, scoreEstimator, cardsEstimator);
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
		if (!cheating) // if cheating: do nothing -> all the cards are known
			CardKnowledgeBase.sampleCardDeterminizationToPlayers(this.gameSession, this.availableCards);
		else
			try {
				Thread.sleep(1); // to make comparison fairer
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	/**
	 * This method should only be called when we want to distribute new cards to the players
	 */
	void sampleCardDeterminizationToPlayersInCardPlay() {
		if (!cheating) // if cheating: do nothing -> all the cards are known
			CardKnowledgeBase.sampleCardDeterminizationToPlayers(this.game, this.availableCards, cardsEstimator);
		else
			try {
				Thread.sleep(1); // to make comparison fairer
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	/**
	 * Checks if the players already have cards.
	 * If they do, we are in a Trumpf selection tree, where the cards are already distributed.
	 * If they don't, we are in a card selection tree where we the player do not have cards yet.
	 *
	 * @return
	 * @deprecated Now this is solved with the isChoosingTrumpf method
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
			return constructTrumpfSelectionJassBoard(availableCards, gameSession, shifted, cheating, scoreEstimator, cardsEstimator);

		JassBoard jassBoard = constructCardSelectionJassBoard(availableCards, game, cheating, scoreEstimator, cardsEstimator);
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
		final Player player = currentPlayer();

		if (isChoosingTrumpf()) {
			// INFO: This performs pruning: removes all the trumpfs which are obviously bad, so that more time can be spent on the good trumpfs
			List<Mode> topTrumpfChoices = TrumpfSelectionHelper.getTopTrumpfChoices(player.getCards(), shifted);

			for (Mode mode : topTrumpfChoices)
				moves.add(new TrumpfMove(player, mode));
		} else {
			if ((player.getCards().isEmpty())) throw new AssertionError("The current player's cards are empty");

			Set<Card> possibleCards = CardSelectionHelper.getCardsPossibleToPlay(EnumSet.copyOf(player.getCards()), game);

			// if (possibleCards.isEmpty()) throw new AssertionError(); // NOTE: Might be a problem inside MCTS

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
		}
		if (moves.isEmpty()) throw new AssertionError();
		return moves;
	}


	/**
	 * Simulate game here
	 *
	 * @param move
	 */
	@Override
	public void makeMove(Move move) {
		if (move == null) throw new AssertionError();

		if (isChoosingTrumpf()) {
			if (!(move instanceof TrumpfMove)) throw new AssertionError();
			final TrumpfMove trumpfMove = (TrumpfMove) move;

			Mode mode = trumpfMove.getChosenTrumpf();
			if (mode.equals(Mode.shift())) {
				//logger.debug("Shifted");
				this.shifted = true;
			} else {
				//logger.debug("Started game with trumpf {}", mode);
				this.gameSession.startNewGame(mode, shifted);
				if (gameSession.getCurrentGame() == null) throw new AssertionError();
				this.game = gameSession.getCurrentGame();
				this.gameSession = null; // NOTE: this is needed so that the method isChoosingTrumpf() will evaluate to false afterwards
			}
		} else {
			final Player player = currentPlayer();

			if (!(move instanceof CardMove)) throw new AssertionError();
			// We can do that because we are only creating CardMoves
			final CardMove cardMove = (CardMove) move;

			if (!cardMove.getPlayer().equals(player)) throw new AssertionError();

			game.makeMove(cardMove);
			player.onMoveMade(cardMove);

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
		if (game == null) throw new AssertionError();
		return game.getCurrentPlayer();
	}

	@Override
	public boolean gameOver() {
		if (isChoosingTrumpf())
			return false;
		if (game == null) throw new AssertionError();
		return game.gameFinished();
	}

	@Override
	public double[] getScore() {
		if (game == null) throw new AssertionError();

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
	@Override
	public double[] getMoveWeights() {
		return new double[game.getCurrentPlayer().getCards().size()];
	}

	@Override
	public Move getBestMove(@NotNull PlayoutSelectionPolicy playoutSelectionPolicy) {
		if (isChoosingTrumpf()) {
			final Mode mode = TrumpfSelectionHelper.predictTrumpf(currentPlayer().getCards(), shifted);
			return new TrumpfMove(currentPlayer(), mode);
		}

		return playoutSelectionPolicy.runPlayout(game);
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
