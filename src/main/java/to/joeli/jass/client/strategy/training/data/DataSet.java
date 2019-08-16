package to.joeli.jass.client.strategy.training.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class DataSet {
	public static final String BASE_PATH = "src/main/resources/";

	public abstract List getFeatures();

	public abstract List getTargets();

	protected abstract String getNetworkTypePath();

	public String getPath(int episode) {
		return getEpisodePath(episode) + getNetworkTypePath();
	}

	@NotNull
	public static String getEpisodePath(int episode) {
		return BASE_PATH + "episodes/" + zeroPadded(episode) + "/";
	}

	public static String zeroPadded(int number) {
		return String.format("%04d", number);
	}

	public String getFeaturesPath(int episode) {
		return getPath(episode) + "features/";
	}

	public String getTargetsPath(int episode) {
		return getPath(episode) + "targets/";
	}
}
