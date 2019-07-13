package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.zuehlke.jasschallenge.client.game.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * https://stackoverflow.com/questions/35701316/discrete-probability-distribution-in-java
 */
public class Distribution {
	private Map<Player, Double> probabilities = new HashMap<>();
	private boolean sampled;
	private Random random = new Random();

	Distribution(Map<Player, Double> probabilities, boolean sampled) {
		this.probabilities = new HashMap<>(probabilities);
		this.sampled = sampled;
		assert sumProbabilities() - 1.0 < 0.000001;
		assert sumProbabilities() - 1.0 > -0.000001;
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

		double deletedProbability = probabilities.remove(player);

		// Redistribute the remaining probability on the other players
		double probabilityToAdd = deletedProbability / probabilities.size();
		for (Map.Entry<Player, Double> entry: probabilities.entrySet()) {
			probabilities.put(entry.getKey(), entry.getValue() + probabilityToAdd);
		}

		assert !probabilities.keySet().isEmpty();
		assert sumProbabilities() - 1.0 < 0.000001;
		assert sumProbabilities() - 1.0 > -0.000001;

		return true;
	}

	public Player sample() {
		// TODO take this random as a parameter so we can configure the seed
		double threshold = random.nextDouble() * sumProbabilities();

		double probability = 0;
		for (Map.Entry<Player, Double> entry : probabilities.entrySet()) {
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

	private double sumProbabilities() {
		return probabilities.values().stream().mapToDouble(i -> i).sum();
	}

	public boolean isSampled() {
		return sampled;
	}

	public void setSampled(boolean sampled) {
		this.sampled = sampled;
	}

	public double[] getProbabilitiesInSeatIdOrder() {
		return probabilities.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey())
				.mapToDouble(Map.Entry::getValue)
				.toArray();
	}

	@Override
	public String toString() {
		return "Distribution{" +
				"probabilities=" + probabilities +
				", sampled=" + sampled +
				'}';
	}
}
