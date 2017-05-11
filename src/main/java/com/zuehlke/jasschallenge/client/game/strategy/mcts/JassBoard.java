package com.zuehlke.jasschallenge.client.game.strategy.mcts;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.deepcopy.DeepCopy;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.JassHelper;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Board;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.CallLocation;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Move;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.*;


/**
 * Created by joelniklaus on 06.05.17.
 */
public class JassBoard implements Board, Serializable {

	private final Set<Card> availableCards;
	private final Game game;
	private final int playerId;


	public JassBoard(Set<Card> availableCards, Game game, boolean newRandomCards) {
		this.availableCards = (Set<Card>) DeepCopy.copy(availableCards);
		this.game = SerializationUtils.clone(game);
		//this.session = (GameSession) DeepCopy.copy(session);
		//this.session = (GameSession) ObjectCloner.deepCopy(session);
		this.playerId = this.game.getCurrentPlayer().getSeatId();
		if (newRandomCards)
			distributeCardsForPlayers(availableCards);
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
		return new JassBoard(availableCards, game, false);
	}

	@Override
	public Board duplicateWithNewRandomCards() {
		return new JassBoard(availableCards, game, true);
	}

	@Override
	public ArrayList<Move> getMoves(CallLocation location) {
		ArrayList<Move> moves = new ArrayList<>();
		Round round = game.getCurrentRound();
		Player player = game.getCurrentPlayer();
		Set<Card> possibleCards = JassHelper.getPossibleCards(player.getCards(), game);


		// stechen wenn letzter spieler und stich gehört gegner TODO noch erweitern
		if (JassHelper.lastPlayer(round)) {
			Player stichOwner = round.getWinner();
			if (JassHelper.isOpponent(stichOwner, player)) {
				//System.out.println(possibleCards);
				Card winningCard = round.getWinningCard();
				Set<Card> cardsToRemove = EnumSet.noneOf(Card.class);
				for (Card card : possibleCards) {
					List<Card> cards = new LinkedList<>();
					cards.add(card);
					cards.add(winningCard);
					if(round.getMode().determineWinningCard(cards).equals(winningCard))
						cardsToRemove.add(card);
					/*
					if (game.getCurrentRound().getMode() != Mode.bottomUp()) {
						if (winningCard.isHigherThan(card))
							cardsToRemove.add(card);
					} else {
						if (!winningCard.isHigherThan(card))
							cardsToRemove.add(card);
					}
					*/
				}
				if (possibleCards.size() > cardsToRemove.size())
					possibleCards.removeAll(cardsToRemove);
				//System.out.println(possibleCards);
			}
		}



		for (Card card : possibleCards)
			moves.add(new CardMove(player, card));

		return moves;
	}

	// TODO exclude very bad moves

	// Wenn erster spieler am anfang des spiels und mindestens 2 trümpfe -> austrumpfen



	// wenn letzter spieler und nicht möglich nicht mit trumpf zu stechen, dann stechen


	// Wenn letzter Spieler und möglich mit nicht trumpf zu stechen, dann stechen.


	// Wenn obeabe oder undeufe: Bei Ausspielen von Partner tiefe Karte (tiefer als 10) von Gegenfarbe verwerfen wenn bei Farbe gut.


	/**
	 * Simulate game here
	 *
	 * @param move
	 */
	@Override
	public void makeMove(Move move) {
		// We can do that because we are only creating CardMoves
		CardMove cardMove = (CardMove) move;

		//System.out.println(game.getCurrentRound());

		Player player = game.getCurrentPlayer();

		//System.out.println(player);
		//System.out.println(cardMove.getPlayer());
		assert cardMove.getPlayer().equals(player);
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

		//System.out.println(Arrays.toString(score));

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
