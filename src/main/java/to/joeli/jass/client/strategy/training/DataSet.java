package to.joeli.jass.client.strategy.training;

import java.util.List;

public abstract class DataSet {
	private static final String BASE_PATH = "src/main/resources/";

	public abstract List getFeatures();

	public abstract List getTargets();

	protected abstract String getNetworkTypePath();

	public String getPath(TrainMode trainMode) {
		return BASE_PATH + trainMode.getPath() + getNetworkTypePath();
	}

	public String getFeaturesPath(TrainMode trainMode) {
		return getPath(trainMode) + "features/";
	}

	public String getTargetsPath(TrainMode trainMode) {
		return getPath(trainMode) + "targets/";
	}
}
