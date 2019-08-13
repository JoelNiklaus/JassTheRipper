package to.joeli.jass.client.strategy.helpers;

import to.joeli.jass.client.game.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * https://stackoverflow.com/questions/35701316/discrete-probability-distribution-in-java
 */
public class Distribution {
	final int SEED = 42;
	final double DELTA = 0.000001;

	private final Map<Player, Float> probabilities;
	private boolean sampled;
	private Random random = new Random(SEED);


	public Distribution(Map<Player, Float> probabilities) {
		this(probabilities, false);
	}

	Distribution(Map<Player, Float> probabilities, boolean sampled) {
		this.probabilities = new HashMap<>(probabilities);
		this.sampled = sampled;
		if (sumProbabilities() - 1.0 >= 0.000001) throw new AssertionError();
		if (sumProbabilities() - 1.0 <= -0.000001) throw new AssertionError();
	}


	/**
	 * Deletes the given player and distributes its probability part to the other players so that the total probability is 1 again.
	 *
	 * @param player
	 * @return true when the player is not in the list or when the operation is completed successfully
	 * false when there is only one player in the list
	 */
	public boolean deleteEventAndReBalance(Player player) {
		if (!probabilities.keySet().contains(player))
			return true;

		if (probabilities.size() == 1)
			return false;

		float deletedProbability = probabilities.remove(player);

		// Redistribute the remaining probability on the other players
		float probabilityToAdd = deletedProbability / probabilities.size();
		for (Map.Entry<Player, Float> entry : probabilities.entrySet()) {
			probabilities.put(entry.getKey(), entry.getValue() + probabilityToAdd);
		}

		if (probabilities.keySet().isEmpty()) throw new AssertionError();
		if (sumProbabilities() - 1.0 >= DELTA) throw new AssertionError();
		if (sumProbabilities() - 1.0 <= -DELTA) throw new AssertionError();

		return true;
	}

	/**
	 * Samples randomly from the probability distribution and returns a player.
	 *
	 * @return
	 */
	public Player sample() {
		// TODO take this random as a parameter so we can configure the seed
		float threshold = random.nextFloat() * sumProbabilities();

		float probability = 0;
		for (Map.Entry<Player, Float> entry : probabilities.entrySet()) {
			probability += entry.getValue();
			if (probability >= threshold)
				return entry.getKey();
		}
		throw new IllegalStateException("The probability sum (" + probability + ") is not greater than the random number chosen (" + threshold + ")");
	}

	public int getNumEvents() {
		return probabilities.keySet().size();
	}

	public boolean hasPlayer(Player player) {
		return probabilities.keySet().contains(player);
	}

	private float sumProbabilities() {
		return (float) probabilities.values().stream().mapToDouble(i -> i).sum();
	}

	public boolean isSampled() {
		return sampled;
	}

	public void setSampled(boolean sampled) {
		this.sampled = sampled;
	}

	/**
	 * Retrieves a float array from the probabilities hashmap for use in the neural networks
	 *
	 * @return
	 */
	public float[] getProbabilitiesInSeatIdOrder() {
		float[] result = new float[4];
		probabilities.forEach((player, probability) -> result[player.getSeatId()] = probability);
		return result;
	}

	@Override
	public String toString() {
		return "Distribution{" +
				"probabilities=" + probabilities +
				", sampled=" + sampled +
				'}';
	}
}
