package to.joeli.jass.client.strategy.training;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.strategy.helpers.GameSessionBuilder;
import to.joeli.jass.client.strategy.helpers.ZeroMQClient;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;

import java.util.EnumSet;
import java.util.Set;

public class CardsEstimatorTest {

	@Before
	public void setUp() {
		ZeroMQClient.startServer();
	}

	@Test
	public void testCardsEstimatorPrediction() {
		Game diamondsGame = GameSessionBuilder.newSession().withStartedGame(Mode.trump(Color.DIAMONDS)).createGameSession().getCurrentGame();
		Set<Card> availableCards = diamondsGame.getCurrentPlayer().getCards();
		// Delete the cards of the players because we want to estimate them.
		diamondsGame.getPlayers().forEach(player -> player.setCards(EnumSet.noneOf(Card.class)));


		CardsEstimator network = new CardsEstimator(true);
		System.out.println(network.predictCardDistribution(diamondsGame, availableCards));
	}

	@After
	public void tearDown() {
		ZeroMQClient.stopServer();
	}

}