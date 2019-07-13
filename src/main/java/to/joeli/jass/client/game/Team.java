package to.joeli.jass.client.game;

import java.util.ArrayList;
import java.util.List;

public class Team {
	private final String teamName;
	private final List<Player> players;

	public Team(String name, List<Player> players) {
		this.teamName = name;
		this.players = players;
	}

	/**
	 * Copy constructor for deep copy
	 *
	 * @param team
	 */
	public Team(Team team) {
		this.teamName = team.getTeamName();
		this.players = new ArrayList<>();
		for (Player player : team.getPlayers())
			this.players.add(new Player(player));
	}

	public String getTeamName() {
		return teamName;
	}

	public List<Player> getPlayers() {
		return players;
	}

	public boolean isTeamOfPlayer(Player player) {
		return players.contains(player);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Team team = (Team) o;

		if (teamName != null ? !teamName.equals(team.teamName) : team.teamName != null) return false;
		return !(players != null ? !players.equals(team.players) : team.players != null);

	}

	@Override
	public int hashCode() {
		int result = teamName != null ? teamName.hashCode() : 0;
		result = 31 * result + (players != null ? players.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Team{" +
				"teamName='" + teamName + '\'' +
				", players=" + players +
				'}';
	}
}
