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
	private Round currentRound;
	private final Result result;

	private List<Round> previousRounds = new ArrayList<>();

	private Game(Mode mode, PlayingOrder order, List<Team> teams, boolean shifted) {
		this.mode = mode;
		this.currentRound = Round.createRound(mode, 0, order);
		this.result = new Result(teams.get(0), teams.get(1));
	}

	/**
	 * Copy constructor for deep copy
	 *
	 * @param game
	 */
	public Game(Game game) {
		this.mode = game.getCurrentRoundMode();
		this.currentRound = new Round(game.getCurrentRound());
		this.result = new Result(game.getResult());
		this.previousRounds = new ArrayList<>();
		for (Round previousRound : game.getPreviousRounds())
			this.previousRounds.add(new Round(previousRound));
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
		final PlayingOrder nextPlayingOrder = createOrderStartingFromPlayer(getOrder().getPlayersInInitialPlayingOrder(), currentRound.getWinner());
		final int nextRoundNumber = currentRound.getRoundNumber() + 1;
		return Round.createRound(mode, nextRoundNumber, nextPlayingOrder);
	}

	public Player getPartnerOfPlayer(Player player) {
		for (Player other : getOrder().getPlayersInInitialPlayingOrder()) {
			if (other.isPartner(player))
				return other;
		}
		return null;
	}

	public Mode getMode() {
		return mode;
	}

	public PlayingOrder getOrder() {
		return currentRound.getPlayingOrder();
	}

	public List<Player> getPlayers() {
		return getOrder().getPlayersInInitialPlayingOrder();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Game)) return false;

		Game game = (Game) o;

		if (!mode.equals(game.mode)) return false;
		if (!currentRound.equals(game.currentRound)) return false;
		if (!result.equals(game.result)) return false;
		return previousRounds.equals(game.previousRounds);
	}

	@Override
	public int hashCode() {
		int result = mode.hashCode();
		result = 31 * result + currentRound.hashCode();
		result = 31 * result + this.result.hashCode();
		result = 31 * result + previousRounds.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "Game{" +
				"mode=" + mode +
				", currentRound=" + currentRound +
				", result=" + result +
				//", previousRounds=" + previousRounds +
				'}';
	}
}
