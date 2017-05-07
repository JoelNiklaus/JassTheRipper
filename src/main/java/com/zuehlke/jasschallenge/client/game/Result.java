package com.zuehlke.jasschallenge.client.game;

import java.io.Serializable;

public class Result implements Serializable {
	private final TeamScore teamAScore;
	private final TeamScore teamBScore;

	public Result(Team teamA, Team teamB) {
		this.teamAScore = new TeamScore(teamA);
		this.teamBScore = new TeamScore(teamB);
	}

	public int getTeamScore(Player player) {
		return getTeamScoreForPlayer(player).getScore();
	}

	public int getOpponentTeamScore(Player player) {
		return getOpponentTeamScoreForPlayer(player).getScore();
	}

	// BUG!!! For match you do not only have to make every point but also every stich!!!
	public boolean isMatch() {

		return teamAScore.getScore() == 0 || teamBScore.getScore() == 0;
	}

	void updateWinningTeamScore(int bonusScore) {

		final Team winningTeam = getWinningTeam();
		updateTeamScore(winningTeam.getPlayers().get(0), bonusScore);
	}

	void add(Result result) {

		getScoreForTeam(result.teamAScore.getTeam()).addScore(result.teamAScore.getScore());
		getScoreForTeam(result.teamBScore.getTeam()).addScore(result.teamBScore.getScore());
	}

	void updateTeamScore(Player winningPlayer, int lastScore) {

		final TeamScore teamScore = getTeamScoreForPlayer(winningPlayer);
		teamScore.addScore(lastScore);
	}

	private TeamScore getScoreForTeam(Team team) {

		if (team.equals(teamAScore.getTeam())) return teamAScore;
		else return teamBScore;
	}

	private Team getWinningTeam() {

		if (teamAScore.getScore() > teamBScore.getScore()) {
			return teamAScore.getTeam();
		} else {
			return teamBScore.getTeam();
		}
	}

	private TeamScore getTeamScoreForPlayer(Player player) {

		if (teamAScore.getTeam().isTeamOfPlayer(player)) return teamAScore;
		else return teamBScore;
	}

	private TeamScore getOpponentTeamScoreForPlayer(Player player) {

		if (teamAScore.getTeam().isTeamOfPlayer(player)) return teamBScore;
		else return teamAScore;
	}

	private static class TeamScore implements Serializable {
		private final Team team;
		private int score;

		public TeamScore(Team team) {
			this.team = team;
			this.score = 0;
		}

		void addScore(int score) {
			this.score += score;
		}

		public int getScore() {
			return score;
		}

		public Team getTeam() {
			return team;
		}
	}
}
