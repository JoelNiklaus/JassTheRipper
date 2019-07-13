package to.joeli.jass.client.strategy.mcts;


import to.joeli.jass.client.game.Player;
import to.joeli.jass.client.strategy.mcts.src.Move;
import to.joeli.jass.game.cards.Card;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class CardMove extends to.joeli.jass.client.game.Move implements Move {

	public CardMove(Player player, Card playedCard) {
		super(player, playedCard);
	}

	@Override
	public int compareTo(Move o) {
		CardMove other = (CardMove) o;
		return getPlayedCard().compareTo(other.getPlayedCard());
	}

	@Override
	public String toString() {
		return getPlayedCard().toString();
	}
}
