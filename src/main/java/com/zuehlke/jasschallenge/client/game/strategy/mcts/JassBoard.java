package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.Board;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.CallLocation;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.Move;
import com.zuehlke.jasschallenge.game.cards.Card;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.*;


/**
 * Created by joelniklaus on 06.05.17.
 */
public class JassBoard implements Board, Serializable {

	private final Game game;
	private final int playerId;

	/**
	 * Private (!) Constructor used for duplicate method
	 *
	 * @param game
	 * @throws Exception
	 */
	private JassBoard(Game game) throws Exception {
		this.game = SerializationUtils.clone(game);
		//this.session = (GameSession) DeepCopy.copy(session);
		//this.session = (GameSession) ObjectCloner.deepCopy(session);
		this.playerId = game.getCurrentPlayer().getSeatId();
	}

	/**
	 * Public factory method which should be used from the outside to create an instance of JassBoard
	 *
	 * @param availableCards
	 * @param game
	 * @return
	 * @throws Exception
	 */
	public static JassBoard jassFactory(Set<Card> availableCards, Game game) throws Exception {
		JassBoard jassBoard = new JassBoard(game);
		jassBoard.distributeCardsForPlayers(availableCards);
		//jassBoard.distributeCardsForPlayers((Set<Card>) ObjectCloner.deepCopy(availableCards));
		return jassBoard;
	}

	/**
	 * add randomized available Cards for the other players based on already played cards
	 *
	 * @param availableCards
	 */
	private void distributeCardsForPlayers(Set<Card> availableCards) {
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();
		Set<Card> remainingCards = getRemainingCards(availableCards);
		double numberOfCardsToAdd = remainingCards.size() / 3.0; // rounds down the number


		for (Player player : order.getPlayerInOrder()) {
			int tempPlayerId = player.getSeatId();
			double numberOfCards = numberOfCardsToAdd;
			Set<Card> cards;
			if (tempPlayerId != playerId) { // randomize cards for the other players
				if (tempPlayerId > playerId) // if tempPlayer is seated after player add one card more
					numberOfCards = Math.ceil(numberOfCards);
				else
					numberOfCards = Math.floor(numberOfCards);

				cards = pickRandomSubSet(remainingCards, (int) numberOfCards);
				remainingCards.removeAll(cards);
			} else
				cards = availableCards;

			player.setCards(cards);
		}

	}

	private Set<Card> pickRandomSubSet(Set<Card> cards, int numberOfCards) {
		Set<Card> subset = EnumSet.noneOf(Card.class);
		Random random = new Random();
		int size = cards.size();
		while (subset.size() < numberOfCards) {
			int item = random.nextInt(size);
			int i = 0;
			for (Card card : cards) {
				if (i == item)
					subset.add(card);
				i++;
			}
		}
		return subset;
	}

	private Set<Card> getRemainingCards(Set<Card> availableCards) {
		Set<Card> cards = EnumSet.allOf(Card.class);
		cards.removeAll(availableCards);
		cards.removeAll(game.getAlreadyPlayedCards());
		return cards;
	}

	public Game getGame() {
		return game;
	}

	/**
	 * Reconstruct Game but add known random cards for players.
	 *
	 * @return
	 */
	@Override
	public Board duplicate() {
		JassBoard jassBoard = null;
		try {
			jassBoard = new JassBoard(game);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jassBoard;
	}

	@Override
	public ArrayList<Move> getMoves(CallLocation location) {
		ArrayList<Move> moves = new ArrayList<>();
		Player player = game.getCurrentPlayer();
		Set<Card> possibleCards = JassHelper.getPossibleCards(player.getCards(), game.getCurrentRound(), game.getCurrentRoundMode());
		// TODO exclude very bad moves
		for (Card card : possibleCards)
			moves.add(new CardMove(player, card));

		return moves;
	}


	/**
	 * Simulate game here
	 *
	 * @param move
	 */
	@Override
	public void makeMove(Move move) {
		// We can do that because we are only creating CardMoves
		CardMove cardMove = (CardMove) move;

		System.out.println(game.getCurrentRound());

		Player player = game.getCurrentPlayer();
		assert ((CardMove) move).getPlayer().equals(player);
		player.getCards().remove((cardMove).getPlayedCard());

		// TODO wrap in try block!
		game.makeMove(cardMove);


		if (game.getCurrentRound().roundFinished()) {
			game.startNextRound();

			Round round = game.getCurrentRound();

			for (Player current : round.getPlayingOrder().getPlayerInOrder()) {
				assert current.getCards().size() == 9 - round.getRoundNumber();
			}
		}
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
		double[] score = new double[getQuantityOfPlayers()];
		Result result = game.getResult();
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();
		for (Player player : order.getPlayerInOrder())
			score[player.getSeatId()] = result.getTeamScore(player);

		System.out.println(Arrays.toString(score));

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
		System.out.println(game.toString());
	}
}
