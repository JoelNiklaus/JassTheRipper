package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.connectFour.ConnectFour;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.connectFour.ConnectFourMove;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.Board;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.CallLocation;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.Move;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

// TODO Somehow we have to generate random cards for the other players. (No perfect information available)


/**
 * Created by joelniklaus on 06.05.17.
 */
public class Jass implements Board, Serializable {

	private final GameSession session;
	private final Game game;
	private final Player player;

	public Jass(Set<Card> availableCards, GameSession session) {
		this.session = session;
		this.game = session.getCurrentGame();
		this.player = game.getCurrentPlayer();
		distributeCardsForPlayers(availableCards, game);
	}

	private void distributeCardsForPlayers(Set<Card> availableCards, Game game) {
		player.setCards(availableCards);
		// add randomized available Cards for the other players based on already played cards
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();
		Set<Card> remainingCards = getRemainingCards(availableCards);
		double numberOfCardsToAdd = remainingCards.size() / 3.0; // rounds down the number
		for (int i = 0; i < 4; i++) {
			int tempPlayerId = order.getCurrentPlayer().getSeatId();
			double numberOfCards = numberOfCardsToAdd;
			if (tempPlayerId != player.getSeatId()) { // randomize cards for the other players
				if (tempPlayerId > player.getSeatId()) // if tempPlayer is seated after player add one card more
					numberOfCards = Math.ceil(numberOfCards);
				else
					numberOfCards = Math.floor(numberOfCards);

				Set<Card> cardsToAdd = pickRandomSubSet(remainingCards, (int) numberOfCards);
				game.getCurrentPlayer().setCards(cardsToAdd);
				remainingCards.removeAll(cardsToAdd);

/*
				System.out.println("available " + availableCards);
				System.out.println("remaining " + remainingCards);
				System.out.println("random " + cardsToAdd);
*/
			}

			order.moveToNextPlayer();
		}
	}

	private Set<Card> pickRandomSubSet(Set<Card> cards, int numberOfCards) {
		Set<Card> subset = EnumSet.noneOf(Card.class);
		while (subset.size() < numberOfCards) {
			int size = cards.size();
			int item = new Random().nextInt(size); // In real life, the Random object should be rather more shared than this
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

	/**
	 * All the played cards until now
	 * The teams and players
	 * The points of each team
	 *
	 * @return
	 */
	@Override
	public Board duplicate() {
		try {
			return (Board) ObjectCloner.deepCopy(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public ArrayList<Move> getMoves(CallLocation location) {
		ArrayList<Move> moves = new ArrayList<Move>();
		Player player = game.getCurrentPlayer();
		//System.out.println(player.getSeatId() + player.toString());
		for (Card card : player.getCards()) {
			moves.add(new CardMove(player, card));
		}
		return moves;
	}


	/**
	 * Simulate game here
	 *
	 * @param move
	 */
	@Override
	public void makeMove(Move move) {
		if (game.getCurrentRound().roundFinished())
			game.startNextRound();


		// // TODO wrap in try block!
		// We can do that because we are only creating CardMoves
		game.makeMove((CardMove) move);

		// delete Card from available Cards of player making the move
		Player player = game.getCurrentPlayer();
		player.getCards().remove(((CardMove) move).getPlayedCard());


		System.out.println(player.toString());
		System.out.println(game.getCurrentRound());
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
		double[] score = new double[2];
		Player player = game.getCurrentPlayer();
		score[player.getSeatId() % 2] = game.getResult().getTeamScore(player);
		score[(player.getSeatId() + 1) % 2] = game.getResult().getOpponentTeamScore(player);
		return score;
	}

	/*
	 * This method is not used by this game, but at least
	 * a function body is required to fulfill the Board
	 * interface contract.
	 */
	public double[] getMoveWeights() {
		// TODO give high weights for good choices and low weights for bad choices. So in random choosing of moves good moves are favoured.
		return null;
	}

	@Override
	public void bPrint() {
		System.out.println(game.toString());
	}
}
