package com.zuehlke.jasschallenge.client.game;

import java.io.Serializable;

public class TeamScore implements Serializable {
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

	public void resetScore() {
		score = 0;
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
		return score + "";
	}
}
