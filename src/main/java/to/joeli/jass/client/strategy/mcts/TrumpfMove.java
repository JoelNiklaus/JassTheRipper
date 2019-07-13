package to.joeli.jass.client.strategy.mcts;


import to.joeli.jass.client.game.Player;
import to.joeli.jass.client.strategy.mcts.src.Move;
import to.joeli.jass.game.mode.Mode;

import java.util.Objects;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class TrumpfMove implements Move {

	private final Player player;
	private final Mode chosenTrumpf;

	public TrumpfMove(Player player, Mode chosenTrumpf) {
		this.player = player;
		this.chosenTrumpf = chosenTrumpf;
	}

	public Player getPlayer() {
		return player;
	}

	public Mode getChosenTrumpf() {
		return chosenTrumpf;
	}

	@Override
	public int compareTo(Move o) {
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TrumpfMove that = (TrumpfMove) o;
		return Objects.equals(player, that.player) &&
				Objects.equals(chosenTrumpf, that.chosenTrumpf);
	}

	@Override
	public int hashCode() {
		return chosenTrumpf.hashCode();
	}

	@Override
	public String toString() {
		return getChosenTrumpf().toString();
	}
}
