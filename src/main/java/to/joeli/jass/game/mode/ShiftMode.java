package to.joeli.jass.game.mode;

import to.joeli.jass.client.game.Move;
import to.joeli.jass.game.Trumpf;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static java.lang.String.valueOf;

public class ShiftMode extends Mode {

	@Override
	public int getCode() {
		// This should not be called from the neural network data generation part!
		return 10;
	}

	@Override
	public int calculateRoundScore(int roundNumber, Set<Card> playedCards) {
		return 0;
	}

	@Override
	public Trumpf getTrumpfName() {
		return Trumpf.SCHIEBE;
	}

	@Override
	public Color getTrumpfColor() {
		return null;
	}

	@Override
	public int calculateScore(Set<Card> playedCards) {
		return 0;
	}

	@Override
	public Move determineWinningMove(List<Move> moves) {
		return null;
	}

	@Override
	public boolean canPlayCard(Card card, Set<Card> alreadyPlayedCards, Color currentRoundColor, Set<Card> playerCards) {
		return false;
	}

	@Override
	public int getFactor() {
		return 0;
	}

	@Override
	public Comparator<Card> createRankComparator() {
		return null;
	}

	@Override
	public String toString() {
		return valueOf(getTrumpfName());
	}
}
