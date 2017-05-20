package com.zuehlke.jasschallenge.client.game.strategy.mcts;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.JassHelper;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Board;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.CallLocation;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Move;
import com.zuehlke.jasschallenge.game.cards.Card;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.*;


/**
 * Created by joelniklaus on 06.05.17.
 */
public class JassBoard implements Board, Serializable {

	private final Set<Card> availableCards;
	private final Game game;


	/**
	 * Constructs a new Jassboard. If the flag is set, deals new random cards to the players.
	 *
	 * @param availableCards
	 * @param game
	 * @param newRandomCards
	 */
	public JassBoard(Set<Card> availableCards, Game game, boolean newRandomCards) throws Exception {
		this.availableCards = copy(availableCards);
		//this.availableCards = Collections.synchronizedSet((Set<Card>) DeepCopy.copy(availableCards));
		this.game = SerializationUtils.clone(game);
		//this.session = (GameSession) DeepCopy.copy(session);
		//this.session = (GameSession) ObjectCloner.deepCopy(session);
		if (newRandomCards)
			distributeCardsForPlayers(this.availableCards);
	}

	private Set<Card> copy(Set<Card> cards) {
		return Collections.synchronizedSet(EnumSet.copyOf(cards));
	}

	/**
	 * add randomized available Cards for the other players based on already played cards
	 *
	 * @param availableCards
	 */
	private void distributeCardsForPlayers(Set<Card> availableCards) throws Exception {
		final int playerId = this.game.getCurrentPlayer().getSeatId();
		final Round round = game.getCurrentRound();
		final PlayingOrder order = round.getPlayingOrder();
		Set<Card> remainingCards = getRemainingCards(availableCards);
		final double numberOfCards = remainingCards.size() / 3.0; // rounds down the number

		for (Player player : order.getPlayerInOrder()) {
			double numberOfCardsToAdd;
			final int tempPlayerId = player.getSeatId();
			Set<Card> cards;
			if (tempPlayerId != playerId) { // randomize cards for the other players
				//if (tempPlayerId > playerId) // if tempPlayer is seated after player add one card more
				if (round.hasPlayerAlreadyPlayed(player))
					numberOfCardsToAdd = Math.floor(numberOfCards);
				else
					numberOfCardsToAdd = Math.ceil(numberOfCards);

				cards = pickRandomSubSet(remainingCards, (int) numberOfCardsToAdd);


				if (!remainingCards.removeAll(cards))
					System.err.println("Could not remove picked cards from remaining cards");
				assert !remainingCards.containsAll(cards);
			} else
				cards = copy(availableCards);

			player.setCards(cards);
		}
		assert remainingCards.isEmpty();
	}

	public Set<Card> testPickRandomSubSet(Set<Card> cards, int numberOfCards) throws Exception {
		return pickRandomSubSet(cards, numberOfCards);
	}

	/**
	 * Picks a random sub set out of the given cards with the given size.
	 *
	 * @param cards
	 * @param numberOfCards
	 * @return
	 */
	private Set<Card> pickRandomSubSet(Set<Card> cards, int numberOfCards) throws Exception {
		assert (numberOfCards > 0 || numberOfCards <= 9);
		List<Card> listOfCards = new LinkedList<>(cards);
		assert numberOfCards <= listOfCards.size();
		Collections.shuffle(listOfCards);
		List<Card> randomSublist = listOfCards.subList(0, numberOfCards);
		Set<Card> randomSubSet = new HashSet<>(randomSublist);
		assert (cards.containsAll(randomSubSet));
		return randomSubSet;
	}

	/**
	 * Get the cards remaining to be split up on the other players.
	 * All cards - already played cards - available cards
	 *
	 * @param availableCards
	 * @return
	 */
	private Set<Card> getRemainingCards(Set<Card> availableCards) {
		Set<Card> cards = Collections.synchronizedSet(EnumSet.allOf(Card.class));
		assert cards.size() == 36;
		cards.removeAll(availableCards);
		Set<Card> alreadyPlayedCards = game.getAlreadyPlayedCards();
		Round round = game.getCurrentRound();
		assert alreadyPlayedCards.size() == round.getRoundNumber() * 4 + round.getPlayedCards().size();
		cards.removeAll(alreadyPlayedCards);
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
		ArrayList<Move> moves = new ArrayList<>();
		final Round round = game.getCurrentRound();
		final Player player = game.getCurrentPlayer();
		Set<Card> possibleCards = JassHelper.getPossibleCards(copy(player.getCards()), game);

		assert (possibleCards.size() > 0);

		possibleCards = JassHelper.refineCardsWithJassKnowledge(possibleCards, round, player);

		assert possibleCards.size() > 0;

		for (Card card : possibleCards)
			moves.add(new CardMove(player, card));
		assert (moves.size() > 0);

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
		final CardMove cardMove = (CardMove) move;

		assert cardMove != null;


		Player player = game.getCurrentPlayer();


		assert cardMove.getPlayer().equals(player);
		player.getCards().remove((cardMove).getPlayedCard());

		// TODO wrap in try block!
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
