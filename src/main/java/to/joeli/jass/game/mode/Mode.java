package to.joeli.jass.game.mode;

import to.joeli.jass.client.game.Move;
import to.joeli.jass.game.Trumpf;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;

import java.util.*;

public abstract class Mode {

	public static Mode topDown() {
		return new TopDownMode();
	}

	public static Mode bottomUp() {
		return new BottomUpMode();
	}

	public static Mode trump(Color color) {
		return new TrumpfColorMode(color);
	}

	public static Mode shift() {
		return new ShiftMode();
	}

	public static Mode from(Trumpf trumpf, Color trumpfColor) {
		switch (trumpf) {
			case OBEABE:
				return topDown();
			case UNDEUFE:
				return bottomUp();
			case SCHIEBE:
				return shift();
			default:
				return trump(trumpfColor);
		}
	}

	public static Mode from(int code) {
		if (code >= 0 && code <= 3)
			return new TrumpfColorMode(Color.getColor(code));
		if (code == 4)
			return topDown();
		if (code == 5)
			return bottomUp();
		if (code == 10)
			return shift();
		throw new IllegalArgumentException("The code must either be between 0 and 5 or 10!");
	}

	public static List<Mode> trumpfModes() {
		List<Mode> modes = new LinkedList<>();
		modes.add(trump(Color.CLUBS));
		modes.add(trump(Color.DIAMONDS));
		modes.add(trump(Color.HEARTS));
		modes.add(trump(Color.SPADES));
		return modes;
	}

	public static List<Mode> noTrumpfModes() {
		List<Mode> modes = new LinkedList<>();
		modes.add(topDown());
		modes.add(bottomUp());
		return modes;
	}

	public static List<Mode> standardModes() {
		List<Mode> modes = trumpfModes();
		modes.addAll(noTrumpfModes());
		return modes;
	}

	public static List<Mode> allModes() {
		List<Mode> modes = standardModes();
		modes.add(Mode.shift());
		return modes;
	}

	public abstract int getCode(); // INFO: Used for neural networks (number between 0 and 7)

	public abstract int calculateRoundScore(int roundNumber, Set<Card> playedCards);

	public abstract Trumpf getTrumpfName();

	public abstract Color getTrumpfColor();

	public abstract int calculateScore(Set<Card> playedCards);

	public boolean isTrumpfMode() {
		return getTrumpfColor() != null;
	}


	public Card determineWinningCard(List<Card> cards) {
		return GeneralRules.determineWinnerCard(cards, createRankComparator(), Optional.ofNullable(getTrumpfColor())).orElse(null);
	}

	public Move determineWinningMove(List<Move> moves) {
		List<Card> cards = new ArrayList<>();
		for (Move move : moves) {
			Card playedCard = move.getPlayedCard();
			cards.add(playedCard);
		}
		Card winningCard = determineWinningCard(cards);
		for (Move move : moves) {
			if (winningCard == move.getPlayedCard()) {
				return move;
			}
		}
		return null;
	}

	public abstract boolean canPlayCard(Card card, Set<Card> alreadyPlayedCards, Color currentRoundColor, Set<Card> playerCards);

	public abstract int getFactor();

	protected abstract Comparator<Card> createRankComparator();

	@Override
	public boolean equals(Object obj) {
		try {
			Mode otherMode = (Mode) obj;
			if (!isTrumpfMode()) {
				if (otherMode.isTrumpfMode())
					return false;
				return (getTrumpfName().equals(otherMode.getTrumpfName()));
			}
			return (getTrumpfName().equals(otherMode.getTrumpfName()) && getTrumpfColor() == otherMode.getTrumpfColor());
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return hashCode(getTrumpfName()) + hashCode(getTrumpfColor());
	}

	private static int hashCode(Object o) {
		return o != null ? o.hashCode() : 0;
	}
}
