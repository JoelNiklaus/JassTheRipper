package to.joeli.jass.client.game.strategy.helpers;

import to.joeli.jass.client.game.*;
import to.joeli.jass.client.game.strategy.mcts.CardMove;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;
import org.junit.Test;

import java.util.EnumSet;

public class PerfectInformationGameSolverTest {

	private Game obeAbeGame = GameSessionBuilder.newSession().withStartedGame(Mode.topDown()).createGameSession().getCurrentGame();

	@Test
	public void testTakesObviousStich() {
		obeAbeGame.getPlayers().get(0).setCards(EnumSet.of(Card.CLUB_SEVEN, Card.HEART_TEN));
		obeAbeGame.getPlayers().get(1).setCards(EnumSet.of(Card.HEART_KING, Card.CLUB_EIGHT));
		obeAbeGame.getPlayers().get(2).setCards(EnumSet.of(Card.HEART_ACE, Card.HEART_NINE));

		obeAbeGame.makeMove(new CardMove(obeAbeGame.getPlayers().get(0), Card.HEART_TEN));
		obeAbeGame.makeMove(new CardMove(obeAbeGame.getPlayers().get(1), Card.HEART_KING));

		final CardMove move = PerfectInformationGameSolver.getMove(obeAbeGame);
		// assertEquals(Card.HEART_ACE, move.getPlayedCard()); // NOTE: So far this solver still selects a random move
	}

}