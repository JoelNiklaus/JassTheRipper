package com.zuehlke.jasschallenge.client.game;

import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.zuehlke.jasschallenge.client.game.PlayingOrder.createOrderStartingFromPlayer;

public class Game implements Serializable {
	public static final int LAST_ROUND_NUMBER = 8;
	private final Mode mode;
	private final PlayingOrder order;
	private Round currentRound;
	private final Result result;
	private final boolean shifted;

	private List<Round> previousRounds = new ArrayList<>();

	private Game(Mode mode, PlayingOrder order, List<Team> teams, boolean shifted) {
		this.mode = mode;
		this.order = order;

		this.currentRound = Round.createRound(mode, 0, order);
		this.result = new Result(teams.get(0), teams.get(1));
		this.shifted = shifted;
	}

	public static Game startGame(Mode mode, PlayingOrder order, List<Team> teams, boolean shifted) {

		return new Game(mode, order, teams, shifted);
	}

	public Round getCurrentRound() {
		return currentRound;
	}

	public Result getResult() {
		return result;
	}

	public Mode getCurrentRoundMode() {
		return getCurrentRound().getMode();
	}

	public Round startNextRound() {
		updateRoundResult();
		previousRounds.add(getCurrentRound());
		if (currentRound.isLastRound() && result.isMatch()) {
			result.updateWinningTeamScore(calculateMatchBonus());
		}
		this.currentRound = createNextRound();
		return currentRound;
	}

	public List<Round> getPreviousRounds() {
		return previousRounds;
	}

	public Set<Card> getAlreadyPlayedCards() {
		Set<Card> cards = currentRound.getPlayedCards();
		for (Round round : previousRounds) {
			cards.addAll(round.getPlayedCards());
		}
		return cards;
	}

	public boolean gameFinished() {
		return currentRound.getRoundNumber() == 9;
	}

	public Player getCurrentPlayer() {
		return currentRound.getCurrentPlayer();
	}

	public void makeMove(Move move) {
		getCurrentRound().makeMove(move);
	}

	private int calculateMatchBonus() {
		return currentRound.getMode().getFactor() * 100;
	}

	private void updateRoundResult() {
		final int lastScore = this.currentRound.calculateScore();
		final Player winner = this.currentRound.getWinner();

		result.updateTeamScore(winner, lastScore);
	}

	private Round createNextRound() {
		final PlayingOrder nextPlayingOrder = createOrderStartingFromPlayer(order.getPlayerInOrder(), currentRound.getWinner());
		final int nextRoundNumber = currentRound.getRoundNumber() + 1;
		return Round.createRound(mode, nextRoundNumber, nextPlayingOrder);
	}



	public boolean isShifted() {
		return shifted;
	}
}
