package com.zuehlke.jasschallenge.client.game;

import java.io.Serializable;

public class Result implements Serializable {
	private final TeamScore teamAScore;
	private final TeamScore teamBScore;

	public Result(Team teamA, Team teamB) {
		this.teamAScore = new TeamScore(teamA);
		this.teamBScore = new TeamScore(teamB);
	}

	/**
	 * Copy constructor for deep copy
	 *
	 * @param result
	 */
	public Result(Result result) {
		this.teamAScore = new TeamScore(result.getTeamAScore());
		this.teamBScore = new TeamScore(result.getTeamBScore());
	}

	public TeamScore getTeamAScore() {
		return teamAScore;
	}

	public TeamScore getTeamBScore() {
		return teamBScore;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Result)) return false;

		Result result = (Result) o;

		if (teamAScore != null ? !teamAScore.equals(result.teamAScore) : result.teamAScore != null) return false;
		return teamBScore != null ? teamBScore.equals(result.teamBScore) : result.teamBScore == null;
	}

	@Override
	public int hashCode() {
		int result = teamAScore != null ? teamAScore.hashCode() : 0;
		result = 31 * result + (teamBScore != null ? teamBScore.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Result{" +
				"teamAScore=" + teamAScore +
				", teamBScore=" + teamBScore +
				'}';
	}

	private static class TeamScore implements Serializable {
		private final Team team;
		private int score;

		public TeamScore(Team team) {
			this.team = team;
			this.score = 0;
		}

		/**
		 * Copy constructor for deep copy
		 *
		 * @param teamScore
		 */
		public TeamScore(TeamScore teamScore) {
			this.team = new Team(teamScore.getTeam());
			this.score = teamScore.getScore();
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

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof TeamScore)) return false;

			TeamScore teamScore = (TeamScore) o;

			if (score != teamScore.score) return false;
			return team != null ? team.equals(teamScore.team) : teamScore.team == null;
		}

		@Override
		public int hashCode() {
			int result = team != null ? team.hashCode() : 0;
			result = 31 * result + score;
			return result;
		}

		@Override
		public String toString() {
			return "TeamScore{" +
					"team=" + team +
					", score=" + score +
					'}';
		}
	}
}
