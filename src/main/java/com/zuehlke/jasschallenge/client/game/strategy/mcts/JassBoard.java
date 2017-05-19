package com.zuehlke.jasschallenge.client.game.strategy.mcts;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.JassHelper;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Board;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.CallLocation;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.Move;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;


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
	public JassBoard(Set<Card> availableCards, Game game, boolean newRandomCards) {
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
	private void distributeCardsForPlayers(Set<Card> availableCards) {
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

	public Set<Card> testPickRandomSubSet(Set<Card> cards, int numberOfCards) {
		return pickRandomSubSet(cards, numberOfCards);
	}

	/**
	 * Picks a random sub set out of the given cards with the given size.
	 *
	 * @param cards
	 * @param numberOfCards
	 * @return
	 */
	private Set<Card> pickRandomSubSet(Set<Card> cards, int numberOfCards) {
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
	public Board duplicate() {
		return new JassBoard(availableCards, game, false);
	}

	@Override
	public Board duplicateWithNewRandomCards() {
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

		possibleCards = refineMovesWithJassKnowledge(possibleCards, round, player);

		assert possibleCards.size() > 0;

		for (Card card : possibleCards)
			moves.add(new CardMove(player, card));
		assert (moves.size() > 0);

		return moves;
	}


	/**
	 * Reduces the set of the possible cards which can be played in a move to the sensible cards.
	 * This is done by expert jass knowledge. It is done here so that all the players play as intelligently as possible
	 * and therfore the simulation gets the most realistic outcome.
	 *
	 * @param possibleCards
	 * @param round
	 * @param player
	 * @return
	 */
	public Set<Card> refineMovesWithJassKnowledge(Set<Card> possibleCards, Round round, Player player) {
		Set<Card> trumps = JassHelper.getTrumps(player.getCards(), round.getMode());

		/**
		 * STECHEN (als letzter Spieler)
		 */
		// wenn letzter Spieler und Stich gehört Gegner
		if (JassHelper.lastPlayer(round) && JassHelper.isOpponent(round.getWinner(), player)) {
			int stichValue = round.calculateScore();
			Set<Card> roundWinningCards = getRoundWinningCards(possibleCards, round);

			// wenn möglich mit nicht trumpf zu stechen
			Set<Card> notTrumpsOfRoundWinningCards = JassHelper.getNotTrumps(roundWinningCards, round.getMode());
			if (!notTrumpsOfRoundWinningCards.isEmpty())
				return notTrumpsOfRoundWinningCards;

			// wenn möglich mit trumpf zu stechen und stich hat mindestens 10 punkte
			Set<Card> trumpsOfRoundWinningCards = JassHelper.getTrumps(roundWinningCards, round.getMode());
			if (!trumpsOfRoundWinningCards.isEmpty() && round.calculateScore() > 10)
				return trumpsOfRoundWinningCards;
		}

		/**
		 * AUSTRUMPFEN
		 */
		// Wenn erster spieler am anfang des spiels (erste beide runden) und mindestens 2 trümpfe
		if (JassHelper.startingPlayer(round) && round.getRoundNumber() <= 1 && trumps.size() >= 2)
			return trumps;


		// Wenn erster spieler
		if (JassHelper.startingPlayer(round)) {
			/**
			 * ANZIEHEN (NACHRICHT SENDEN)
			 */
			// look for a not trumpf color where i have a king or queen but someone else has the ace
			// -> play small card so king or queen gets bock

			/**
			 * ANZIEHEN (NACHRICHT EMPFANGEN)
			 */
			// if my partner played anziehen in one of the previous rounds, play this color


			/**
			 * VERWERFEN (NACHRICHT EMPFANGEN)
			 */
			// if my partner played verwerfen in one of the previous rounds, play opposite color
		}


		// wenn partner schon gespielt hat
		if (JassHelper.hasPartnerAlreadyPlayed(round)) {
			Card cardOfPartner = JassHelper.getCardOfPartner(round);
			// wenn partner den stich macht bis jetzt
			if (round.getWinningCard().equals(cardOfPartner)) {
				/**
				 * SCHMIEREN
				 */
				// wenn ich noch angeben kann
				if (JassHelper.isAngebenPossible(possibleCards, cardOfPartner)) {
					Set<Card> schmierCards = JassHelper.getSchmierCards(possibleCards, cardOfPartner, round.getMode());
					// wenn letzter spieler einfach schmieren
					if (JassHelper.lastPlayer(round))
						return schmierCards;
						// TODO wenn zweitletzter spieler prüfen ob letzer spieler noch stechen kann
					else {
						assert JassHelper.thirdPlayer(round);
						// TODO to change
						return schmierCards;
					}
				}
				/**
				 * VERWERFEN
				 */
				// wenn nicht -> (Gegenfarbe von Farbe wo ich gut bin)
				else {
					if (round.getMode().equals(Mode.bottomUp())) {


					} else {
						if (round.getMode().equals(Mode.topDown())) {

						} else {

						}
					}

				}
			}
		}

		// TODO für jeden Spieler Karteneinschätzung machen!!!

		// TODO geschätzte karten von partner anpassen, wenn verwerfen von ihm erkannt wurde!

		return possibleCards;
	}

	/**
	 * Get all of my cards which can win the round.
	 *
	 * @param possibleCards
	 * @param round
	 * @return
	 */
	private Set<Card> getRoundWinningCards(Set<Card> possibleCards, Round round) {
		Set<Card> remainingCards = new HashSet<>(possibleCards);
		Card winningCard = round.getWinningCard();
		Set<Card> cardsToRemove = EnumSet.noneOf(Card.class);
		for (Card card : remainingCards) {
			List<Card> cards = new LinkedList<>();
			cards.add(card);
			cards.add(winningCard);
			if (round.getMode().determineWinningCard(cards).equals(winningCard))
				cardsToRemove.add(card);
		}
		if (remainingCards.size() > cardsToRemove.size())
			remainingCards.removeAll(cardsToRemove);
		return remainingCards;
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
