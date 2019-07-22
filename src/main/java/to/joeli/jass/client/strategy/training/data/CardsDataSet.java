package to.joeli.jass.client.strategy.training.data;

import com.google.common.collect.EvictingQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class CardsDataSet extends DataSet {

	protected Queue<double[][]> features;
	protected Queue<int[][]> targets;

	public CardsDataSet(int size) {
		features = EvictingQueue.create(size);
		targets = EvictingQueue.create(size);
	}

	public boolean addFeature(double[][] newFeature) {
		return features.add(newFeature);
	}

	public boolean addFeatures(List<double[][]> newFeatures) {
		return features.addAll(newFeatures);
	}

	public boolean addTarget(int[][] newTarget) {
		return targets.add(newTarget);
	}

	public boolean addTargets(List<int[][]> newTargets) {
		return targets.addAll(newTargets);
	}

	public List getFeatures() {
		return new ArrayList(features);
	}

	public List getTargets() {
		return new ArrayList(targets);
	}

	protected String getNetworkTypePath() {
		return "cards/";
	}
}
