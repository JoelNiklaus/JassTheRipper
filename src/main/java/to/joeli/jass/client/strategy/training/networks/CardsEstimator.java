package to.joeli.jass.client.strategy.training.networks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tensorflow.Tensor;
import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.client.strategy.helpers.CardKnowledgeBase;
import to.joeli.jass.client.strategy.helpers.Distribution;
import to.joeli.jass.client.strategy.helpers.NeuralNetworkHelper;
import to.joeli.jass.client.strategy.training.NetworkType;
import to.joeli.jass.game.cards.Card;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CardsEstimator extends NeuralNetwork {

	public static final Logger logger = LoggerFactory.getLogger(CardsEstimator.class);

	public CardsEstimator(boolean trainable) {
		super(NetworkType.CARDS, trainable);
	}


	/**
	 * Predict the card distribution of the hidden cards of the other players.
	 * This should help generating determinizations of better quality.
	 *
	 * @return
	 */
	public Map<Card, Distribution> predictCardDistribution(Game game, Set<Card> availableCards) {
		Map<Card, Distribution> cardKnowledge = CardKnowledgeBase.initCardKnowledge(game, availableCards);

		final Tensor result = (Tensor) predict(NeuralNetworkHelper.getCardsFeatures(game, cardKnowledge));
		double[][][] res = new double[1][36][4];
		result.copyTo(res);
		final double[][] probabilities = res[0];

		final List<Player> players = game.getPlayersBySeatId();

		cardKnowledge = new HashMap<>();
		final Card[] cards = Card.values();
		for (int c = 0; c < cards.length; c++) {
			HashMap<Player, Double> playerProbabilities = new HashMap<>();
			for (int p = 0; p < players.size(); p++) {
				playerProbabilities.put(players.get(p), probabilities[c][p]);
			}
			cardKnowledge.put(cards[c], new Distribution(playerProbabilities));
		}

		return cardKnowledge;
	}
}
