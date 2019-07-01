package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * https://stackoverflow.com/questions/35701316/discrete-probability-distribution-in-java
 *
 * @param <T>
 */
public class Distribution<T> {
	private List<Double> probabilities = new ArrayList<>();
	private List<T> events = new ArrayList<>();
	private double sumProbabilities;
	private Random random = new Random();

	Distribution(Map<T, Double> probabilities) {
		for (Map.Entry<T, Double> entry : probabilities.entrySet()) {
			sumProbabilities += entry.getValue();
			events.add(entry.getKey());
			this.probabilities.add(entry.getValue());
		}
		assert sumProbabilities - 1.0 < 0.000001;
		assert sumProbabilities - 1.0 > -0.000001;
	}

	/**
	 * Deletes the given event and distributes its probability part to the other events so that the total probability is 1 again.
	 *
	 * @param event
	 * @return true when the event is not in the list or when the operation is completed successfully
	 * false when there is only one event in the list
	 */
	public boolean deleteEventAndReBalance(T event) {
		if (!events.contains(event))
			return true;

		if (events.size() == 1)
			return false;

		int index = events.indexOf(event);
		events.remove(event);

		double deletedProbability = probabilities.get(index);
		probabilities.remove(index);
		double probabilityToAdd = deletedProbability / probabilities.size();

		for (int i = 0; i < probabilities.size(); i++) {
			probabilities.set(i, probabilities.get(i) + probabilityToAdd);
		}

		assert !events.isEmpty();
		assert sumProbabilities - 1.0 < 0.000001;
		assert sumProbabilities - 1.0 > -0.000001;

		return true;
	}

	public T sample() {
		double probability = random.nextDouble() * sumProbabilities;
		int i;
		for (i = 0; probability > 0; i++) {
			probability -= probabilities.get(i);
		}
		return events.get(i - 1);
	}

	public int getNumEvents() {
		return events.size();
	}

	public boolean hasEvent(T event) {
		return events.contains(event);
	}
}
