package com.zuehlke.jasschallenge.client.game;

import com.zuehlke.jasschallenge.game.mode.Mode;

import java.io.Serializable;
import java.util.List;

import static com.zuehlke.jasschallenge.client.game.PlayingOrder.createOrder;
import static com.zuehlke.jasschallenge.client.game.PlayingOrder.createOrderStartingFromPlayer;

public class GameSession implements Serializable {

	private final List<Team> teams;
	private final List<Player> playersInPlayingOrder;
	private final PlayingOrder gameStartingPlayerOrder;
	private Game currentGame;
	private final Result result;

	public GameSession(List<Team> teams, List<Player> playersInPlayingOrder) {
		this.teams = teams;
		assert teams.size() == 2;

		this.playersInPlayingOrder = playersInPlayingOrder;
		this.gameStartingPlayerOrder = createOrder(playersInPlayingOrder);

		result = new Result(teams.get(0), teams.get(0));
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
		List<Player> players = getTeamOfPlayer(player).getPlayers();
		if (players.get(0) == player)
			return players.get(1);
		return players.get(0);
	}

	public void startNewGame(Mode mode, boolean shifted) {

		updateResult();

		final PlayingOrder initialOrder = createOrderStartingFromPlayer(playersInPlayingOrder, gameStartingPlayerOrder.getCurrentPlayer());
		gameStartingPlayerOrder.moveToNextPlayer();

		currentGame = Game.startGame(mode, initialOrder, teams, shifted);
	}

	public Round startNextRound() {

		return currentGame.startNextRound();
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
}
