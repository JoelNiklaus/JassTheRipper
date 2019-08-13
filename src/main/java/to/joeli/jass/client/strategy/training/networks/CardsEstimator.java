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

import java.util.*;

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
		float[][][] res = new float[1][36][4];
		result.copyTo(res);
		final float[][] probabilities = res[0];

		final List<Player> players = game.getPlayersBySeatId();

		cardKnowledge = new HashMap<>();
		final Card[] cards = Card.values();
		for (int c = 0; c < cards.length; c++) {
			HashMap<Player, Float> playerProbabilities = new HashMap<>();
			List<Player> playersWithZeroProbabilities = new ArrayList<>();
			for (int p = 0; p < players.size(); p++) {
				final Player player = players.get(p);
				if (!cardKnowledge.get(cards[c]).hasPlayer(player))
					playersWithZeroProbabilities.add(player);
				playerProbabilities.put(player, probabilities[c][p]);
			}
			final Distribution distribution = new Distribution(playerProbabilities);
			// When we know already for sure that one player cannot have a card we redistribute this probability to the remaining players
			playersWithZeroProbabilities.forEach(distribution::deleteEventAndReBalance);
			cardKnowledge.put(cards[c], distribution);
		}

		return cardKnowledge;
	}
}
