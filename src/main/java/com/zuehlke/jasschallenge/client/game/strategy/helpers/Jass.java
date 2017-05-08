package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.connectFour.ConnectFour;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.connectFour.ConnectFourMove;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.Board;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.CallLocation;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main.Move;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;
import com.zuehlke.jasschallenge.messages.PlayerJoined;

import java.io.*;
import java.lang.management.GarbageCollectorMXBean;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Created by joelniklaus on 06.05.17.
 */
public class Jass implements Board, Serializable {

	//private final GameSession originalSession;
	//private final Set<Card> originalAvailableCards;
	//private final Set<Card> availableCards;


	private final GameSession session;
	private final Game game;
	private final int playerId;

	private List<Set<Card>> cardsOfPlayers = new ArrayList<>();

	/**
	 * Private (!) Constructor used for duplicate method
	 *
	 * @param session
	 * @throws Exception
	 */
	private Jass(GameSession session, PlayingOrder playingOrder) throws Exception {
		//this.originalSession = (GameSession) ObjectCloner.deepCopy(session); // Not to be changed ever! Needed for duplicate method
		//this.originalAvailableCards = (Set<Card>) ObjectCloner.deepCopy(availableCards); // Not to be changed ever! Needed for duplicate method
		//this.availableCards = (Set<Card>) ObjectCloner.deepCopy(availableCards);
		this.session = (GameSession) ObjectCloner.deepCopy(session);
		this.cardsOfPlayers = (List<Set<Card>>) ObjectCloner.deepCopy(cardsOfPlayers);
		this.game = this.session.getCurrentGame();
		this.playerId = game.getCurrentPlayer().getSeatId();

/*
		Round round = game.getCurrentRound();
		for (Player current : round.getPlayingOrder().getPlayerInOrder()) {
			System.out.println("NumberOfCards " + current.getCards().size() + " of current: " + current);
			//assert current.getCards().size() == 9 - round.getRoundNumber();

		}


		playingOrder = (PlayingOrder) ObjectCloner.deepCopy(playingOrder);
		PlayingOrder order = game.getCurrentRound().getPlayingOrder();
		// Assign randomly dealt cards from before to players again
		for (Player player : order.getPlayerInOrder()) {
			for (Player originalPlayer : playingOrder.getPlayerInOrder()) {
				if (player.equals(originalPlayer)) {
					player.setCards(originalPlayer.getCards());
					System.out.println("Set cards");
					System.out.println(player);
					System.out.println(originalPlayer);
				}
			}
			//player.setCards(jass.getCardsOfPlayers().get(player.getSeatId()));
		}


		round = game.getCurrentRound();
		for (Player current : round.getPlayingOrder().getPlayerInOrder()) {
			System.out.println("NumberOfCards " + current.getCards().size() + " of current: " + current);
			//assert current.getCards().size() == 9 - round.getRoundNumber();

		}
*/
	}

	/**
	 * Public factory method which should be used from the outside to create an instance of Jass
	 *
	 * @param availableCards
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public static Jass jassFactory(Set<Card> availableCards, GameSession session) throws Exception {
		Jass jass = new Jass(session, session.getCurrentRound().getPlayingOrder());
		jass.distributeCardsForPlayers((Set<Card>) ObjectCloner.deepCopy(availableCards));
		return jass;
	}

	/**
	 * add randomized available Cards for the other players based on already played cards
	 *
	 * @param availableCards
	 */
	private void distributeCardsForPlayers(Set<Card> availableCards) {
		// init cardsOfPlayers
		for (int i = 0; i < 4; i++)
			cardsOfPlayers.add(EnumSet.noneOf(Card.class));

		PlayingOrder order = game.getCurrentRound().getPlayingOrder();
		Set<Card> remainingCards = getRemainingCards(availableCards);
		double numberOfCardsToAdd = remainingCards.size() / 3.0; // rounds down the number


		System.out.println(numberOfCardsToAdd);

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
			cardsOfPlayers.get(player.getSeatId()).addAll(cards);
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
	 * Reconstruct Game but add known random cards for players.
	 *
	 * @return
	 */
	@Override
	public Board duplicate() {
		Jass jass = null;
		try {
			jass = new Jass(session, game.getCurrentRound().getPlayingOrder());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jass;
	}

	@Override
	public ArrayList<Move> getMoves(CallLocation location) {
		ArrayList<Move> moves = new ArrayList<>();
		Player player = game.getCurrentPlayer();
		//System.out.println(player.getSeatId() + player.toString());
		Set<Card> possibleCards = JassHelper.getPossibleCards(player.getCards(), game.getCurrentRound(), game.getCurrentRoundMode());
		for (Card card : possibleCards) {
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
		//System.out.println("Before makeMove: " + game.getCurrentRound());

		Player player = game.getCurrentPlayer();

		//System.out.println(player);
		//System.out.println(((CardMove) move).getPlayer());

		assert ((CardMove) move).getPlayer().equals(player);
		//player.makeMove(session);
		player.getCards().remove(((CardMove) move).getPlayedCard());
		// // TODO wrap in try block!
		// We can do that because we are only creating CardMoves
		game.makeMove((CardMove) move);

		//System.out.println(move);

		// TODO was tun wenn game fertig, oder jemand keine karten mehr hat.

		//System.out.println("After makeMove: " + game.getCurrentRound());

		if (game.getCurrentRound().roundFinished()) {
			//System.out.println("currentPlayer: " + game.getCurrentRound().getCurrentPlayer());
			Round round = game.getCurrentRound();


			for (Player current : round.getPlayingOrder().getPlayerInOrder()) {
				System.out.println("NumberOfCards " + current.getCards().size() + " of current: " +current.getSeatId() + current);
				//assert current.getCards().size() == 9 - round.getRoundNumber();
			}


			game.startNextRound();


			//System.out.println("currentPlayer: " + game.getCurrentRound().getCurrentPlayer());
			round = game.getCurrentRound();

			System.out.println("next Round started: " + round.getRoundNumber());


			for (Player current : round.getPlayingOrder().getPlayerInOrder()) {
				//System.out.println("NumberOfCards " + current.getCards().size() + " of current: " + current);
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
		for(Player player: order.getPlayerInOrder()) {
			score[player.getSeatId()] = result.getTeamScore(player);
			System.out.println(player.getSeatId() + " " + result.getTeamScore(player));
		}
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
