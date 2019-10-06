package to.joeli.jass.client.strategy.helpers;

import org.junit.Test;
import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.strategy.mcts.CardMove;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static to.joeli.jass.game.cards.Card.*;

public class PerfectInformationGameSolverTest {

	private Game obeAbeGame = GameSessionBuilder.newSession().withStartedGame(Mode.topDown()).createGameSession().getCurrentGame();
	private Game clubsGame = GameSessionBuilder.startedClubsGame();

	@Test
	public void testLeadingPlayerAdvisableCards() {
		final Set<Card> possibleCards = CardSelectionHelper.getCardsPossibleToPlay(clubsGame.getCurrentPlayer().getCards(), clubsGame);
		final Set<Card> advisableCards = PerfectInformationGameSolver.getAdvisableCards(clubsGame, possibleCards);

		assertEquals(EnumSet.of(CLUB_QUEEN, CLUB_ACE, HEART_SIX, HEART_JACK, SPADE_TEN, SPADE_KING), advisableCards);
	}

	@Test
	public void testLastPlayerTakesObviousStich() {
		clubsGame.getPlayers().get(0).setCards(EnumSet.of(Card.HEART_TEN, Card.CLUB_SEVEN));
		clubsGame.getPlayers().get(2).setCards(EnumSet.of(Card.HEART_JACK, Card.DIAMOND_ACE));
		clubsGame.getPlayers().get(1).setCards(EnumSet.of(Card.HEART_KING, Card.CLUB_EIGHT));
		clubsGame.getPlayers().get(3).setCards(EnumSet.of(Card.HEART_ACE, Card.CLUB_NINE));

		clubsGame.makeMove(new CardMove(clubsGame.getPlayers().get(0), Card.HEART_TEN));
		clubsGame.makeMove(new CardMove(clubsGame.getPlayers().get(1), Card.HEART_JACK));
		clubsGame.makeMove(new CardMove(clubsGame.getPlayers().get(2), Card.HEART_KING));

		for (int i = 0; i < 10; i++) {
			final CardMove move = PerfectInformationGameSolver.runHeavyPlayout(clubsGame);
			assertEquals(Card.HEART_ACE, move.getPlayedCard());
		}
	}

	@Test
	public void testLastPlayerSchmiersIfPossible() {
		clubsGame.getPlayers().get(0).setCards(EnumSet.of(Card.HEART_SEVEN, Card.CLUB_SEVEN));
		clubsGame.getPlayers().get(2).setCards(EnumSet.of(Card.HEART_KING, Card.DIAMOND_ACE));
		clubsGame.getPlayers().get(1).setCards(EnumSet.of(Card.HEART_JACK, Card.CLUB_EIGHT));
		clubsGame.getPlayers().get(3).setCards(EnumSet.of(Card.HEART_TEN, Card.CLUB_NINE));

		clubsGame.makeMove(new CardMove(clubsGame.getPlayers().get(0), Card.HEART_SEVEN));
		clubsGame.makeMove(new CardMove(clubsGame.getPlayers().get(1), Card.HEART_KING));
		clubsGame.makeMove(new CardMove(clubsGame.getPlayers().get(2), Card.HEART_JACK));

		for (int i = 0; i < 10; i++) {
			final CardMove move = PerfectInformationGameSolver.runHeavyPlayout(clubsGame);
			assertEquals(Card.HEART_TEN, move.getPlayedCard());
		}
	}

	@Test
	public void testLastPlayerVerwirftIfNoBetterOption() {
		clubsGame.getPlayers().get(0).setCards(EnumSet.of(Card.HEART_TEN, Card.CLUB_SEVEN));
		clubsGame.getPlayers().get(2).setCards(EnumSet.of(Card.HEART_JACK, Card.DIAMOND_ACE));
		clubsGame.getPlayers().get(1).setCards(EnumSet.of(Card.HEART_KING, Card.CLUB_EIGHT));
		clubsGame.getPlayers().get(3).setCards(EnumSet.of(Card.DIAMOND_NINE, Card.DIAMOND_TEN));

		clubsGame.makeMove(new CardMove(clubsGame.getPlayers().get(0), Card.HEART_TEN));
		clubsGame.makeMove(new CardMove(clubsGame.getPlayers().get(1), Card.HEART_JACK));
		clubsGame.makeMove(new CardMove(clubsGame.getPlayers().get(2), Card.HEART_KING));

		for (int i = 0; i < 10; i++) {
			final CardMove move = PerfectInformationGameSolver.runHeavyPlayout(clubsGame);
			assertEquals(Card.DIAMOND_NINE, move.getPlayedCard());
		}
	}
}