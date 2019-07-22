package to.joeli.jass.client.strategy.training.data;

import com.google.common.collect.EvictingQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class ScoreDataSet extends DataSet {

	protected Queue<double[][]> features;
	protected Queue<Double> targets;


	public ScoreDataSet(int size) {
		features = EvictingQueue.create(size);
		targets = EvictingQueue.create(size);
	}

	public boolean addFeature(double[][] newFeature) {
		return features.add(newFeature);
	}

	public boolean addFeatures(List<double[][]> newFeatures) {
		return features.addAll(newFeatures);
	}


	public boolean addTarget(Double newTarget) {
		return targets.add(newTarget);
	}

	public boolean addTargets(List<Double> newTargets) {
		return targets.addAll(newTargets);
	}

	public List getFeatures() {
		return new ArrayList(features);
	}

	public List getTargets() {
		return new ArrayList(targets);
	}

	protected String getNetworkTypePath() {
		return "score/";
	}
}
