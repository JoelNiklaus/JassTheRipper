package to.joeli.jass.client.strategy.helpers;

import to.joeli.jass.client.game.*;
import to.joeli.jass.client.strategy.mcts.CardMove;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;

public class PerfectInformationGameSolverTest {

	private Game obeAbeGame = GameSessionBuilder.newSession().withStartedGame(Mode.topDown()).createGameSession().getCurrentGame();
	private Game clubsGame = GameSessionBuilder.startedClubsGame();

	@Test
	public void testTakesObviousStich() {
		clubsGame.getPlayers().get(0).setCards(EnumSet.of(Card.HEART_TEN, Card.CLUB_SEVEN));
		clubsGame.getPlayers().get(2).setCards(EnumSet.of(Card.HEART_JACK, Card.DIAMOND_ACE));
		clubsGame.getPlayers().get(1).setCards(EnumSet.of(Card.HEART_KING, Card.CLUB_EIGHT));
		clubsGame.getPlayers().get(3).setCards(EnumSet.of(Card.HEART_ACE, Card.HEART_NINE));

		clubsGame.makeMove(new CardMove(clubsGame.getPlayers().get(0), Card.HEART_TEN));
		clubsGame.makeMove(new CardMove(clubsGame.getPlayers().get(1), Card.HEART_JACK));
		clubsGame.makeMove(new CardMove(clubsGame.getPlayers().get(2), Card.HEART_KING));

		final CardMove move = PerfectInformationGameSolver.runHeavyPlayout(clubsGame);
		assertEquals(Card.HEART_ACE, move.getPlayedCard());
	}
}