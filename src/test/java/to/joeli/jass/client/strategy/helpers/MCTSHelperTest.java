package to.joeli.jass.client.strategy.helpers;

import to.joeli.jass.client.game.*;
import to.joeli.jass.client.strategy.config.MCTSConfig;
import to.joeli.jass.client.strategy.config.RunMode;
import to.joeli.jass.client.strategy.config.StrengthLevel;
import to.joeli.jass.client.strategy.exceptions.MCTSException;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class MCTSHelperTest {

	@Test
	public void testExecutorServiceShutsDownCorrectly() throws MCTSException {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		final MCTSConfig mctsConfig = new MCTSConfig(StrengthLevel.FAST, StrengthLevel.FAST_TEST);
		mctsConfig.setRunMode(RunMode.RUNS);
		MCTSHelper mctsHelper = new MCTSHelper(mctsConfig);
		final Set<Card> cards = gameSession.getCurrentGame().getCurrentPlayer().getCards();

		mctsHelper.predictMove(cards, gameSession, false, false);

		assertFalse(mctsHelper.isShutDown());

		mctsHelper.shutDown();

		assertTrue(mctsHelper.isShutDown());
	}

	@Test
	public void testMCTSTrumpf() throws MCTSException {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.createGameSession();

		final MCTSConfig mctsConfig = new MCTSConfig(StrengthLevel.FAST, StrengthLevel.FAST_TEST);
		mctsConfig.setRunMode(RunMode.RUNS);
		MCTSHelper mctsHelper = new MCTSHelper(mctsConfig);
		final Set<Card> cards = gameSession.getTrumpfSelectingPlayer().getCards();

		mctsHelper.predictMove(cards, gameSession, true, false);
	}

	@Test
	public void testMCTSStart() throws MCTSException {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();


		final MCTSConfig mctsConfig = new MCTSConfig(StrengthLevel.FAST, StrengthLevel.FAST_TEST);
		mctsConfig.setRunMode(RunMode.RUNS);
		MCTSHelper mctsHelper = new MCTSHelper(mctsConfig);
		final Set<Card> cards = gameSession.getCurrentGame().getCurrentPlayer().getCards();

		mctsHelper.predictMove(cards, gameSession, false, false);
	}

	@Test
	public void testMCTSDuringFirstRound() throws MCTSException {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		PlayingOrder order = gameSession.getCurrentRound().getPlayingOrder();

		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_NINE));

		final MCTSConfig mctsConfig = new MCTSConfig(StrengthLevel.FAST, StrengthLevel.FAST_TEST);
		mctsConfig.setRunMode(RunMode.RUNS);
		MCTSHelper mctsHelper = new MCTSHelper(mctsConfig);
		final Set<Card> cards = gameSession.getCurrentGame().getCurrentPlayer().getCards();

		mctsHelper.predictMove(cards, gameSession, false, false);
	}

	@Test
	public void testMCTSDuringSecondRound() throws MCTSException {
		final GameSession gameSession = GameSessionBuilder.newSession()
				.withStartedGame(Mode.bottomUp())
				.createGameSession();

		PlayingOrder order = gameSession.getCurrentRound().getPlayingOrder();

		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_NINE));
		order.moveToNextPlayer();
		Player player = order.getCurrentPlayer();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_SEVEN));
		order.moveToNextPlayer();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_KING));
		order.moveToNextPlayer();
		gameSession.makeMove(new Move(order.getCurrentPlayer(), Card.CLUB_ACE));
		order.moveToNextPlayer();

		gameSession.startNextRound();

		gameSession.makeMove(new Move(player, Card.HEART_ACE));

		final MCTSConfig mctsConfig = new MCTSConfig(StrengthLevel.FAST, StrengthLevel.FAST_TEST);
		mctsConfig.setRunMode(RunMode.RUNS);
		MCTSHelper mctsHelper = new MCTSHelper(mctsConfig);
		final Set<Card> cards = gameSession.getCurrentGame().getCurrentPlayer().getCards();

		mctsHelper.predictMove(cards, gameSession, false, false);
	}

}