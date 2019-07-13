package to.joeli.jass.client.game.strategy.helpers;

import to.joeli.jass.client.game.Move;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;

import java.util.List;
import java.util.stream.Collectors;

public class DataAugmentationHelper {

	static Card getRespectiveCard(Card card, List<Color> colors) {
		if (colors != null) {
			Color newCardColor = colors.get(card.getColor().getValue());
			card = Card.getCard(newCardColor, card.getValue());
		}
		return card;
	}

	static Mode getRespectiveMode(Mode mode, List<Color> colors) {
		if (colors != null) {
			Color newTrumpfColor = colors.get(mode.getTrumpfColor().getValue());
			mode = Mode.trump(newTrumpfColor);
		}
		return mode;
	}

	static List<Move> getRespectiveMoves(List<Move> moves, List<Color> colors) {
		return moves.stream().
				map(move -> {
					final Card respectiveCard = getRespectiveCard(move.getPlayedCard(), colors);
					return new Move(move.getPlayer(), respectiveCard);
				})
				.collect(Collectors.toList());
	}
}
