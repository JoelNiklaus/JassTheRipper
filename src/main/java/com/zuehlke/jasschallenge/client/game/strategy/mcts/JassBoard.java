package com.zuehlke.jasschallenge.client.game.strategy.mcts;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.JassTheRipperJassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.deepcopy.DeepCopy;
import com.zuehlke.jasschallenge.client.game.strategy.deepcopy.ObjectCloner;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.Helper;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.JassHelper;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Board;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.CallLocation;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Move;
import com.zuehlke.jasschallenge.game.cards.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;


/**
 * Created by joelniklaus on 06.05.17.
 */
public class JassBoard implements Board, Serializable {

	private final Set<Card> availableCards;
	private final Game game;

	public final static Logger logger = LoggerFactory.getLogger(JassTheRipperJassStrategy.class);



	/**
	 * Constructs a new Jassboard. If the flag is set, deals new random cards to the players.
	 *
	 * @param availableCards
	 * @param game
	 * @param newRandomCards
	 */
	public JassBoard(Set<Card> availableCards, Game game, boolean newRandomCards) throws Exception {
		long startTime = System.currentTimeMillis();

		this.availableCards = EnumSet.copyOf(availableCards);

		this.game = new Game(game);
		// INFO: The version with copy constructors is around factor 10 more efficient than the other versions
		//this.game = (Game) DeepCopy.copy(game);
		//this.game = (Game) new Cloner().deepClone(game);
		//this.game = ObjectCloner.deepCopySerialization(game);
		//this.game = SerializationUtils.clone(game);

		if (newRandomCards)
			JassHelper.distributeCardsForPlayers(this.availableCards, this.game);

		//Helper.printMethodTime(startTime);
	}

	/**
	 * Reconstruct Game but add known random cards for players.
	 *
	 * @return
	 */
	@Override
	public Board duplicate() throws Exception {
		return new JassBoard(availableCards, game, false);
	}

	@Override
	public Board duplicateWithNewRandomCards() throws Exception {
		return new JassBoard(availableCards, game, true);
	}

	/**
	 * Puts together a list of moves containing possible (or reduced to only sensible) cards to play.
	 *
	 * @param location
	 * @return
	 */
	@Override
	public ArrayList<Move> getMoves(CallLocation location) {
		//final long startTime = System.currentTimeMillis();

		ArrayList<Move> moves = new ArrayList<>();
		final Player player = game.getCurrentPlayer();
		Set<Card> possibleCards = JassHelper.getPossibleCards(EnumSet.copyOf(player.getCards()), game);

		assert (possibleCards.size() > 0);

		try {
			//logger.info("Possible cards before refining: " + possibleCards);
			possibleCards = JassHelper.refineCardsWithJassKnowledge(possibleCards, game);
			//logger.info("Possible cards after refining: " + possibleCards);
		} catch (Exception e) {
			logger.info("Could not refine cards with Jass Knowledge. Just considering all possible cards now");
			e.printStackTrace();
		}

		assert possibleCards.size() > 0;

		for (Card card : possibleCards)
			moves.add(new CardMove(player, card));
		assert (moves.size() > 0);

		//Helper.printMethodTime(startTime);

		return moves;
	}


	/**
	 * Simulate game here
	 *
	 * @param move
	 */
	@Override
	public void makeMove(Move move) {
		//final long startTime = System.currentTimeMillis();

		// We can do that because we are only creating CardMoves
		final CardMove cardMove = (CardMove) move;

		assert cardMove != null;

		Player player = game.getCurrentPlayer();

		assert cardMove.getPlayer().equals(player);

		player.getCards().remove((cardMove).getPlayedCard());

		game.makeMove(cardMove);

		if (game.getCurrentRound().roundFinished()) {
			game.startNextRound();

			Round round = game.getCurrentRound();

			if (round.getRoundNumber() == 9)
				assert game.gameFinished();

			for (Player current : round.getPlayingOrder().getPlayerInOrder()) {
				assert current.getCards().size() == 9 - round.getRoundNumber();
			}
		}

		//Helper.printMethodTime(startTime);
	}

	@Override
	public int getQuantityOfPlayers() {
		return 4;
	}

	@Override
	public int getCurrentPlayer() {
		return game.getCurrentPlayer().getSeatId();
	}

	@Override
	public boolean gameOver() {
		return game.gameFinished();
	}

	@Override
	public double[] getScore() {
		//final long startTime = System.currentTimeMillis();

		double[] score = new double[getQuantityOfPlayers()];
		Result result = game.getResult();
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();
		for (Player player : order.getPlayerInOrder())
			score[player.getSeatId()] = result.getTeamScore(player);

		//Helper.printMethodTime(startTime);

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

	@Override
	public void bPrint() {
		logger.info(game.toString());
	}
}
