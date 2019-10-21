package to.joeli.jass.client.game;

import to.joeli.jass.client.strategy.config.Config;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import static to.joeli.jass.client.game.PlayingOrder.createOrder;
import static to.joeli.jass.client.game.PlayingOrder.createOrderStartingFromPlayer;

public class GameSession {

	public static final boolean MATCH_BONUS_ENABLED = false;

	private final List<Team> teams;
	private final PlayingOrder gameStartingPlayingOrder;
	private Game currentGame;
	private final Result result;

	public GameSession(List<Team> teams, List<Player> playersInPlayingOrder) {
		this.teams = teams;
		if (teams.size() != 2) throw new AssertionError();

		this.gameStartingPlayingOrder = createOrder(playersInPlayingOrder);

		result = new Result(teams.get(0), teams.get(0));
	}

	/**
	 * Copy constructor for deep copy
	 *
	 * @param gameSession
	 */
	public GameSession(GameSession gameSession) {
		// INFO: Certain Objects (e.g. Players, Teams) are duplicated multiple times
		// -> we have different references! When we update one Player in the PlayingOrder, the corresponding Player in the Team will not be updated!
		this.teams = new ArrayList<>();
		for (Team team : gameSession.getTeams())
			this.teams.add(new Team(team));
		this.gameStartingPlayingOrder = new PlayingOrder(gameSession.getGameStartingPlayingOrder());
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

	public List<Player> getPlayersOfTeam(int teamIndex) {
		return teams.get(teamIndex).getPlayers();
	}

	public void setConfigs(Config[] configs) {
		getPlayersOfTeam(0).forEach(player -> player.setConfig(configs[0]));
		getPlayersOfTeam(1).forEach(player -> player.setConfig(configs[1]));
	}

	public Player getPartnerOfPlayer(Player player) {
		return gameStartingPlayingOrder.getPartnerOfPlayer(player);
	}

	public void startNewGame(Mode mode, boolean shifted) {
		updateResult();

		final PlayingOrder initialOrder = createOrderStartingFromPlayer(getPlayersInInitialPlayingOrder(), gameStartingPlayingOrder.getCurrentPlayer());
		gameStartingPlayingOrder.moveToNextPlayer();

		currentGame = Game.startGame(mode, initialOrder, teams, shifted);
	}

	public Round startNextRound() {
		return currentGame.startNextRound();
	}

	public List<Player> getPlayersInInitialPlayingOrder() {
		return gameStartingPlayingOrder.getPlayersInInitialOrder();
	}

	public PlayingOrder getGameStartingPlayingOrder() {
		return gameStartingPlayingOrder;
	}

	public Player getTrumpfSelectingPlayer() {
		return gameStartingPlayingOrder.getCurrentPlayer();
	}

	public Player getCurrentPlayer() {
		return currentGame.getCurrentPlayer();
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

	public void updateResult() {
		if (currentGame == null) return;

		result.add(currentGame.getResult());
	}

	/**
	 * Used in training simulations to reset
	 */
	public void resetResult() {
		currentGame = null;
		result.resetScores();
	}

	public void dealCards(List<Card> cards) {
		int startIndex = 0;
		for (Player player : gameStartingPlayingOrder.getPlayersInInitialOrder()) {
			player.setCards(EnumSet.copyOf(cards.subList(startIndex, startIndex + 9)));
			startIndex += 9;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GameSession that = (GameSession) o;
		return Objects.equals(teams, that.teams) &&
				Objects.equals(gameStartingPlayingOrder, that.gameStartingPlayingOrder) &&
				Objects.equals(currentGame, that.currentGame) &&
				Objects.equals(result, that.result);
	}

	@Override
	public int hashCode() {
		return Objects.hash(teams, gameStartingPlayingOrder, currentGame, result);
	}

	@Override
	public String toString() {
		return "GameSession{" +
				"teams=" + teams +
				", gameStartingPlayingOrder=" + gameStartingPlayingOrder +
				", currentGame=" + currentGame +
				", result=" + result +
				'}';
	}
}
