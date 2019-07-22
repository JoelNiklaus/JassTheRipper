package to.joeli.jass.client.strategy.training.networks;

import org.junit.Test;
import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.strategy.helpers.CardKnowledgeBase;
import to.joeli.jass.client.strategy.helpers.Distribution;
import to.joeli.jass.client.strategy.helpers.GameSessionBuilder;
import to.joeli.jass.client.strategy.helpers.NeuralNetworkHelper;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class CardsEstimatorTest {


	@Test
	public void testCardsEstimatorPrediction() {
		Game diamondsGame = GameSessionBuilder.newSession().withStartedGame(Mode.trump(Color.DIAMONDS)).createGameSession().getCurrentGame();

		final float[][] scoreFeatures = NeuralNetworkHelper.getScoreFeatures(diamondsGame);
		for (int i = 0; i < 36; i++) {
			System.out.println(Card.values()[i] + ": " + Arrays.toString(scoreFeatures[37 + i]));
		}

		Set<Card> availableCards = EnumSet.copyOf(diamondsGame.getCurrentPlayer().getCards());
		// Delete the cards of the players because we want to estimate them.
		diamondsGame.getPlayers().forEach(player -> player.setCards(EnumSet.noneOf(Card.class)));

		final Map<Card, Distribution> cardKnowledge = CardKnowledgeBase.initCardKnowledge(diamondsGame, availableCards);
		final float[][] cardsFeatures = NeuralNetworkHelper.getCardsFeatures(diamondsGame, cardKnowledge);
		for (int i = 0; i < 36; i++) {
			System.out.println(Card.values()[i] + ": " + Arrays.toString(cardsFeatures[37 + i]));
		}


		CardsEstimator network = new CardsEstimator(true);
		network.loadModel(0);

		final Map<Card, Distribution> cardDistributionMap = network.predictCardDistribution(diamondsGame, availableCards);
		cardDistributionMap.forEach((card, distribution) -> System.out.println(card + ": " + Arrays.toString(distribution.getProbabilitiesInSeatIdOrder())));
	}
}