package to.joeli.jass.client.strategy.training.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class DataSet {
	public static final String BASE_PATH = "src/main/resources/";

	public abstract List getFeatures();

	public abstract List getTargets();

	protected abstract String getNetworkType();

	public static String zeroPadded(int number) {
		return String.format("%04d", number);
	}

	@NotNull
	public static String getEpisodePath(int episode) {
		return BASE_PATH + "episodes/" + zeroPadded(episode) + "/";
	}

	public String getNetworkTypePath(int episode) {
		return getEpisodePath(episode) + getNetworkType();
	}

	public String getDataSetTypePath(int episode, String dataSetType) {
		return getNetworkTypePath(episode) + dataSetType;
	}

	public String getFeaturesPath(int episode, String dataSetType) {
		return getDataSetTypePath(episode, dataSetType) + "features/";
	}

	public String getTargetsPath(int episode, String dataSetType) {
		return getDataSetTypePath(episode, dataSetType) + "targets/";
	}
}
