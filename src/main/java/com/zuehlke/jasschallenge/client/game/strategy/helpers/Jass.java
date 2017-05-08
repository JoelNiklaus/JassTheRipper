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
import java.lang.management.GarbageCollectorMXBean;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Created by joelniklaus on 06.05.17.
 */
public class Jass implements Board, Serializable {

	private final GameSession originalSession;
	private final Set<Card> originalAvailableCards;

	private final GameSession session;
	private final Game game;
	private final int playerId;
	private final Set<Card> availableCards;

	private List<Set<Card>> cardsOfPlayers = new ArrayList<>();

	private Jass(Set<Card> availableCards, GameSession session, List<Set<Card>> cardsOfPlayers) throws Exception {
		this.originalSession = (GameSession) ObjectCloner.deepCopy(session); // Not to be changed ever! Needed for duplicate method
		this.originalAvailableCards = (Set<Card>) ObjectCloner.deepCopy(availableCards); // Not to be changed ever! Needed for duplicate method
		this.session = session;
		this.game = session.getCurrentGame();
		this.playerId = game.getCurrentPlayer().getSeatId();
		this.availableCards = availableCards;
		this.cardsOfPlayers = cardsOfPlayers;
	}

	/**
	 * Public factory method which should be used from the outside to create an instance of Jass
	 * @param availableCards
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public static Jass jassFactory(Set<Card> availableCards, GameSession session) throws Exception {
		Jass jass = new Jass(availableCards, session, new ArrayList<>());
		jass.distributeCardsForPlayers();
		return jass;
	}

	// add randomized available Cards for the other players based on already played cards
	private void distributeCardsForPlayers() {
		// init cardsOfPlayers
		for (int i = 0; i < 4; i++)
			cardsOfPlayers.add(EnumSet.noneOf(Card.class));

		PlayingOrder order = game.getCurrentRound().getPlayingOrder();
		Set<Card> remainingCards = getRemainingCards(availableCards);
		double numberOfCardsToAdd = remainingCards.size() / 3.0; // rounds down the number
		for (int i = 0; i < 4; i++) {
			int tempPlayerId = order.getCurrentPlayer().getSeatId();
			double numberOfCards = numberOfCardsToAdd;
			Set<Card> cards;
			if (tempPlayerId != playerId) { // randomize cards for the other players
				if (tempPlayerId > playerId) // if tempPlayer is seated after player add one card more
					numberOfCards = Math.ceil(numberOfCards);
				else
					numberOfCards = Math.floor(numberOfCards);

				cards = pickRandomSubSet(remainingCards, (int) numberOfCards);
				remainingCards.removeAll(cards);

/*
				System.out.println("available " + availableCards);
				System.out.println("remaining " + remainingCards);
				System.out.println("random " + cards);
*/
			} else
				cards = availableCards;


			Player player = game.getCurrentPlayer();
			player.setCards(cards);
			cardsOfPlayers.get(player.getSeatId()).addAll(cards);

			//System.out.println("cardsOfPlayers" + cardsOfPlayers.get(player.getSeatId()));

			order.moveToNextPlayer();
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

	public List<Set<Card>> getCardsOfPlayers() {
		return cardsOfPlayers;
	}

	public void setCardsOfPlayers(List<Set<Card>> cardsOfPlayers) {
		this.cardsOfPlayers = cardsOfPlayers;
	}

	/**
	 * Reconstruct original Game but add known random cards for players.
	 *
	 * @return
	 */
	@Override
	public Board duplicate() {
		Jass jass = null;
		try {
			jass = new Jass(originalAvailableCards, originalSession, cardsOfPlayers);
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (int i = 0; i < 4; i++) {
			System.out.println("cardsOfPlayers" + cardsOfPlayers.get(i));

			jass.getGame().getCurrentPlayer().setCards(jass.getCardsOfPlayers().get(i));
			jass.getGame().getCurrentRound().getPlayingOrder().moveToNextPlayer();
		}
		return jass;
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

		System.out.println(game.getCurrentRound());


		// // TODO wrap in try block!
		// We can do that because we are only creating CardMoves
		game.makeMove((CardMove) move);

		// delete Card from available Cards of player making the move
		Player player = game.getCurrentPlayer();
		player.makeMove(session);

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
