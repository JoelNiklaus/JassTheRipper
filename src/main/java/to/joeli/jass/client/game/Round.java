package to.joeli.jass.client.game;

import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class Round {
	private final Mode mode;
	private final int roundNumber;
	private final PlayingOrder playingOrder;
	private final List<Move> moves = new ArrayList<>();

	public static Round createRound(Mode gameMode, int roundNumber, PlayingOrder playingOrder) {
		return new Round(gameMode, roundNumber, playingOrder);
	}

	private Round(Mode mode, int roundNumber, PlayingOrder playingOrder) {
		this.mode = mode;
		this.roundNumber = roundNumber;
		this.playingOrder = playingOrder;
	}

	/**
	 * Copy constructor for deep copy
	 *
	 * @param round
	 */
	public Round(Round round) {
		this.mode = round.getMode();
		this.roundNumber = round.getRoundNumber();
		this.playingOrder = new PlayingOrder(round.getPlayingOrder());
		for (Move move : round.getMoves())
			this.moves.add(new Move(move));

	}

	public void makeMove(Move move) {
		if (!move.getPlayer().equals(playingOrder.getCurrentPlayer()))
			throw new RuntimeException("It's not players " + move.getPlayer() + " turn. It's " + playingOrder.getCurrentPlayer() + " turn.");
		if (moves.size() == 4)
			throw new RuntimeException("Only four cards can be played in a round.");

		moves.add(move);
		// NOTE: If this method were called here it would be a bit simpler. But it breaks tests
		// move.getPlayer().onMoveMade(move);
		playingOrder.moveToNextPlayer();
	}

	public Card getCardOfPlayer(Player player) {
		for (Move move : moves) {
			if (move.getPlayer().equals(player))
				return move.getPlayedCard();
		}
		return null;
	}

	public int getRoundNumber() {
		return roundNumber;
	}

	public int calculateScore() {

		return mode.calculateRoundScore(roundNumber, getPlayedCards());
	}

	public Card getWinningCard() {
		return mode.determineWinningCard(getPlayedCards().stream().collect(Collectors.toList()));
		// TODO maybe not always returns a card ...
		//return moves.stream().filter(move -> move.getPlayer().equals(getWinner())).findFirst().get().getPlayedCard();
	}

	public Set<Card> getPlayedCards() {
		return moves.stream()
				.map(Move::getPlayedCard)
				.collect(toSet());
	}

	public List<Card> getPlayedCardsInOrder() {
		return moves.stream()
				.map(Move::getPlayedCard)
				.collect(toList());
	}

	public Color getRoundColor() {
		if (moves.isEmpty()) return null;

		return moves.get(0).getPlayedCard().getColor();
	}

	public Player getWinner() {
		final Move winningMove = mode.determineWinningMove(this.moves);
		if (winningMove == null) return null;

		return winningMove.getPlayer();
	}

	public boolean hasPlayerAlreadyPlayed(Player player) {
		for (Move move : moves) {
			if (move.getPlayer().equals(player))
				return true;
		}
		return false;
	}

	public Player getCurrentPlayer() {
		return getPlayingOrder().getCurrentPlayer();
	}

	public boolean roundFinished() {
		return getMoves().size() == 4;
	}

	public int numberOfPlayedCards() {
		return getPlayedCards().size();
	}

	public List<Move> getMoves() {
		return moves;
	}

	public PlayingOrder getPlayingOrder() {
		return playingOrder;
	}

	public Mode getMode() {
		return mode;
	}

	public boolean isLastRound() {
		return getRoundNumber() == Game.LAST_ROUND_NUMBER;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Round)) return false;

		Round round = (Round) o;

		if (roundNumber != round.roundNumber) return false;
		if (mode != null ? !mode.equals(round.mode) : round.mode != null) return false;
		if (playingOrder != null ? !playingOrder.equals(round.playingOrder) : round.playingOrder != null) return false;
		return moves != null ? moves.equals(round.moves) : round.moves == null;
	}

	@Override
	public int hashCode() {
		int result = mode != null ? mode.hashCode() : 0;
		result = 31 * result + roundNumber;
		result = 31 * result + (playingOrder != null ? playingOrder.hashCode() : 0);
		result = 31 * result + (moves != null ? moves.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Round{" +
				"mode=" + mode +
				", roundNumber=" + roundNumber +
				", playingOrder=" + playingOrder +
				", moves=" + moves +
				'}';
	}
}
