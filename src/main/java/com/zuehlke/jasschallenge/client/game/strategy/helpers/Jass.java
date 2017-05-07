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

	private Set<Card> availableCards;
	private final Game game;
	private final Player player;
	private int currentPlayer = 0;

	private List<Set<Card>> cardsOfPlayers = new ArrayList<>();

	public Jass(Set<Card> availableCards, Game game) {
		this.game = game;
		this.player = game.getCurrentPlayer();
		this.availableCards = availableCards;
		this.currentPlayer = player.getSeatId();

		// initialize with available cards
		for (int i = 0; i < 4; i++) {
			cardsOfPlayers.add(availableCards);
		}
		// add randomized available Cards for the other players based on already played cards
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();
		Set<Card> remainingCards = getRemainingCards(availableCards);
		int numberOfCardsToAdd = remainingCards.size() / 3; // rounds down the number
		for (int i = 0; i < 4; i++) {
			int tempPlayerId = order.getCurrentPlayer().getSeatId();
			int numberOfCards = numberOfCardsToAdd;
			if (tempPlayerId != player.getSeatId()) { // randomize cards for the other players
				if (tempPlayerId > player.getSeatId()) // if tempPlayer is seated after player add one card more
					numberOfCards++;

				Set<Card> cardsToAdd = pickRandomSubSet(remainingCards);
				cardsOfPlayers.add(tempPlayerId, cardsToAdd);
				remainingCards.removeAll(cardsToAdd);
			}

			order.moveToNextPlayer();
		}
	}

	private Set<Card> pickRandomSubSet(Set<Card> cards) {
		Set<Card> subset = EnumSet.noneOf(Card.class);
		int size = cards.size();
		int item = new Random().nextInt(size); // In real life, the Random object should be rather more shared than this
		int i = 0;
		for (Card card : cards) {
			if (i == item)
				subset.add(card);
			i++;
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
		/*
		String string = null;
		try {
			string = toString(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(" Encoded serialized version ");
		System.out.println(string);
		Board board = null;
		try {
			board = (Board) fromString(string);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("\n\nReconstituted object");
		System.out.println(board);
		// Copy board data
		return board;
		*/
	}

	/**
	 * Read the object from Base64 string.
	 */
	private static Object fromString(String s) throws IOException,
			ClassNotFoundException {
		byte[] data = Base64.getDecoder().decode(s);
		ObjectInputStream ois = new ObjectInputStream(
				new ByteArrayInputStream(data));
		Object o = ois.readObject();
		ois.close();
		return o;
	}

	/**
	 * Write the object to a Base64 string.
	 */
	private static String toString(Serializable o) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.close();
		return Base64.getEncoder().encodeToString(baos.toByteArray());
	}

	@Override
	public ArrayList<Move> getMoves(CallLocation location) {
		ArrayList<Move> moves = new ArrayList<Move>();
		// TODO Exclude very bad choices here
		for (Card card : availableCards) {
			moves.add(new CardMove(game.getCurrentPlayer(), card));
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
		// We can do that because we are only creating CardMoves
		game.makeMove((CardMove) move);
		// delete Card from available Cards of player making the move

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
		score[currentPlayer] = game.getResult().getTeamScore(player);
		// TODO enter score for opponent team
		//score[(currentPlayer+1) %2] = game.getResult().getTeamScore(player);
		return score;
	}

	/*
	 * This method is not used by this game, but at least
	 * a function body is required to fulfill the Board
	 * interface contract.
	 */
	public double[] getMoveWeights() {
		return null;
	}

	@Override
	public void bPrint() {
		System.out.println(game.toString());
	}
}
