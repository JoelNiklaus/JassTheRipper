package to.joeli.jass.client.game;

import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Game {
	public static final int LAST_ROUND_NUMBER = 8;
	private final Mode mode;
	private Round currentRound;
	private final Result result;
	private final boolean shifted;

	private List<Round> previousRounds = new ArrayList<>();

	private Game(Mode mode, PlayingOrder order, List<Team> teams, boolean shifted) {
		this.mode = mode;
		this.currentRound = Round.createRound(mode, 0, order);
		this.result = new Result(teams.get(0), teams.get(1));
		this.shifted = shifted;
	}

	/**
	 * Copy constructor for deep copy
	 *
	 * @param game
	 */
	public Game(Game game) {
		// INFO: Certain Objects (e.g. Players, Round) are duplicated multiple times
		// -> we have different references! When we update one Player in the Round, the corresponding Player in the Result will not be updated!
		this.mode = game.getCurrentRoundMode();
		this.currentRound = new Round(game.getCurrentRound());
		this.result = new Result(game.getResult());
		this.previousRounds = new ArrayList<>();
		for (Round previousRound : game.getPreviousRounds())
			this.previousRounds.add(new Round(previousRound));
		this.shifted = game.shifted;
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

	public boolean isShifted() {
		return shifted;
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
			cards.addAll(round.getPlayedCardsInOrder());
		}
		return cards;
	}

	public List<Card> getAlreadyPlayedCardsInOrder() {
		List<Card> cards = new ArrayList<>();
		for (Round round : previousRounds) {
			cards.addAll(round.getPlayedCardsInOrder());
		}
		cards.addAll(currentRound.getPlayedCardsInOrder());
		return cards;
	}

	public List<Move> getAlreadyPlayedMovesInOrder() {
		List<Move> moves = new ArrayList<>();
		for (Round round : previousRounds) {
			moves.addAll(round.getMoves());
		}
		moves.addAll(currentRound.getMoves());
		return moves;
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
		final PlayingOrder nextPlayingOrder = PlayingOrder.createOrderStartingFromPlayer(getOrder().getPlayersInInitialOrder(), currentRound.getWinner());
		final int nextRoundNumber = currentRound.getRoundNumber() + 1;
		return Round.createRound(mode, nextRoundNumber, nextPlayingOrder);
	}

	public Player getPartnerOfPlayer(Player player) {
		for (Player other : getOrder().getPlayersInInitialOrder()) {
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
		return getOrder().getPlayersInInitialOrder();
	}

	public List<Player> getPlayersBySeatId() {
		// Player implements Comparable and is compared using the seatId
		return getPlayers().stream().sorted().collect(Collectors.toList());
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
		return Objects.hash(mode, currentRound, result, previousRounds);
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
