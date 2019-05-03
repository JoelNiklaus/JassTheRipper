package com.zuehlke.jasschallenge.client.game;

import com.zuehlke.jasschallenge.game.mode.Mode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.zuehlke.jasschallenge.client.game.PlayingOrder.createOrder;
import static com.zuehlke.jasschallenge.client.game.PlayingOrder.createOrderStartingFromPlayer;

public class GameSession implements Serializable {

	private final List<Team> teams;
	private final PlayingOrder gameStartingPlayerOrder;
	private Game currentGame;
	private final Result result;

	public GameSession(List<Team> teams, List<Player> playersInPlayingOrder) {
		this.teams = teams;
		assert teams.size() == 2;

		this.gameStartingPlayerOrder = createOrder(playersInPlayingOrder);

		result = new Result(teams.get(0), teams.get(0));
	}

	/**
	 * Copy constructor for deep copy
	 *
	 * @param gameSession
	 */
	public GameSession(GameSession gameSession) {
		// INFO: Certain Objects (e.g. Players, Teams) are duplicated multiple times
		// -> we have different references! When we update one Player in the Playingorder, the corresponding Player in the Team will not be updated!
		this.teams = new ArrayList<>();
		for (Team team : gameSession.getTeams())
			this.teams.add(new Team(team));
		this.gameStartingPlayerOrder = new PlayingOrder(gameSession.getGameStartingPlayerOrder());
		if (gameSession.getCurrentGame() == null)
			this.currentGame = null;
		else
			this.currentGame = new Game(gameSession.getCurrentGame());
		this.result = new Result(gameSession.getResult());
	}

	public Round getCurrentRound() {
		if (currentGame == null) return null;

		return currentGame.getCurrentRound();
	}

	public List<Team> getTeams() {
		return teams;
	}

	public Team getTeamOfPlayer(Player player) {
		for (Team team : teams) {
			if (team.getPlayers().contains(player))
				return team;
		}
		return null;
	}

	public Team getOpponentTeamOfPlayer(Player player) {
		for (Team team : teams) {
			if (!team.getPlayers().contains(player))
				return team;
		}
		return null;
	}

	public Player getPartnerOfPlayer(Player player) {
		return gameStartingPlayerOrder.getPartnerOfPlayer(player);
	}

	public void startNewGame(Mode mode, boolean shifted) {
		updateResult();

		final PlayingOrder initialOrder = createOrderStartingFromPlayer(getPlayersInInitialPlayingOrder(), gameStartingPlayerOrder.getCurrentPlayer());
		gameStartingPlayerOrder.moveToNextPlayer();

		currentGame = Game.startGame(mode, initialOrder, teams, shifted);
	}

	public Round startNextRound() {
		return currentGame.startNextRound();
	}

	public List<Player> getPlayersInInitialPlayingOrder() {
		return gameStartingPlayerOrder.getPlayersInInitialPlayingOrder();
	}

	public PlayingOrder getGameStartingPlayerOrder() {
		return gameStartingPlayerOrder;
	}

	public Player getCurrentPlayer() {
		return gameStartingPlayerOrder.getCurrentPlayer();
	}

	public void makeMove(Move move) {
		currentGame.makeMove(move);
	}

	public Game getCurrentGame() {
		return currentGame;
	}

	public Result getResult() {
		return result;
	}

	private void updateResult() {
		if (currentGame == null) return;

		result.add(currentGame.getResult());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GameSession that = (GameSession) o;
		return Objects.equals(teams, that.teams) &&
				Objects.equals(gameStartingPlayerOrder, that.gameStartingPlayerOrder) &&
				Objects.equals(currentGame, that.currentGame) &&
				Objects.equals(result, that.result);
	}

	@Override
	public int hashCode() {
		return Objects.hash(teams, gameStartingPlayerOrder, currentGame, result);
	}

	@Override
	public String toString() {
		return "GameSession{" +
				"teams=" + teams +
				", gameStartingPlayerOrder=" + gameStartingPlayerOrder +
				", currentGame=" + currentGame +
				", result=" + result +
				'}';
	}
}
