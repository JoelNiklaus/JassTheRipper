package to.joeli.jass.client.strategy.training.data;

import java.util.List;

public abstract class DataSet {
	public static final String BASE_PATH = "src/main/resources/";

	public abstract List getFeatures();

	public abstract List getTargets();

	protected abstract String getNetworkTypePath();

	public String getPath(String episodeNumber) {
		return BASE_PATH + episodeNumber + "/" + getNetworkTypePath();
	}

	public String getFeaturesPath(String episodeNumber) {
		return getPath(episodeNumber) + "features/";
	}

	public String getTargetsPath(String episodeNumber) {
		return getPath(episodeNumber) + "targets/";
	}
}
