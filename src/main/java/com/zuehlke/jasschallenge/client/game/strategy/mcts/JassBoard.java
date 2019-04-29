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
	private GameSession gameSession;
	private boolean shifted;
	private Game game;

	public static final Logger logger = LoggerFactory.getLogger(JassBoard.class);

	private JassBoard(Set<Card> availableCards, GameSession gameSession, boolean shifted, Game game) {
		this.availableCards = availableCards;
		this.gameSession = gameSession;
		this.shifted = shifted;
		this.game = game;
	}

	public static JassBoard constructTrumpfSelectionJassBoard(Set<Card> availableCards, GameSession gameSession, boolean shifted) {
		return new JassBoard(EnumSet.copyOf(availableCards), new GameSession(gameSession), shifted, null);
	}

	public static JassBoard constructCardSelectionJassBoard(Set<Card> availableCards, Game game) {
		// INFO: The version with copy constructors is almost factor 100 more efficient than the fastest other version
		//this.game = (Game) DeepCopy.copy(game);
		//this.game = (Game) new Cloner().deepClone(game);
		//this.game = ObjectCloner.deepCopySerialization(game);
		//this.game = SerializationUtils.clone(game);
		return new JassBoard(EnumSet.copyOf(availableCards), null, false, new Game(game));
	}


	/**
	 * Duplicates the board and determinizes the the cards of the other players.
	 *
	 * @return
	 */
	@Override
	public Board duplicate(boolean newRandomCards) {
		if (isChoosingTrumpf())
			return constructTrumpfSelectionJassBoard(availableCards, gameSession, shifted);

		JassBoard jassBoard = constructCardSelectionJassBoard(availableCards, game);
		if (newRandomCards)
			jassBoard.sampleCardDeterminizationToPlayers();
		return jassBoard;
	}

	void sampleCardDeterminizationToPlayers() {
		CardKnowledgeBase.sampleCardDeterminizationToPlayers(this.game, this.availableCards);
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
				CardKnowledgeBase.sampleCardDeterminizationToPlayers(this.game, this.availableCards);
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
		if (isChoosingTrumpf()) {
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

	/**
	 * Specifies if we are in the trumpf selection phase or in the card selection phase
	 *
	 * @return
	 */
	private boolean isChoosingTrumpf() {
		return this.gameSession != null && this.game == null;
	}
}
