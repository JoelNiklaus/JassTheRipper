package to.joeli.jass.client.strategy.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.client.strategy.training.Arena;
import to.joeli.jass.client.strategy.training.DataSet;
import to.joeli.jass.client.strategy.training.TrainMode;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class IOHelper {

	public static final Logger logger = LoggerFactory.getLogger(IOHelper.class);


	public static void writeCBOR(Object array, String path) throws IOException {
		CBORFactory factory = new CBORFactory();
		ObjectMapper mapper = new ObjectMapper(factory);
		byte[] bytes = mapper.writeValueAsBytes(array);
		FileUtils.writeByteArrayToFile(new File(path), bytes);
		new ObjectMapper().writeValue(new File(path + ".json"), array);
	}

	public static void readCBOR(Object array, String path) throws IOException {
		CBORFactory factory = new CBORFactory();
		ObjectMapper mapper = new ObjectMapper(factory);
		byte[] bytes = FileUtils.readFileToByteArray(new File(path));
		List features = mapper.readValue(bytes, List.class);
		System.out.println(features);
	}

	private static boolean createIfNotExists(File directory) {
		if (directory.isDirectory())
			return true;
		return directory.mkdirs();
	}

	/**
	 * Saves multiple files into the subdirectories "features" and "labels".
	 * These files can then be loaded and concatenated again to form the big dataset.
	 * The reason for not storing just one big file is that we cannot hold such big arrays in memory (Java throws OutOfMemoryErrors)
	 *
	 * @param dataSet
	 */
	public static boolean saveData(DataSet dataSet, TrainMode trainMode, String name) {
		String basePath = Arena.DATASETS_BASE_PATH + trainMode.getPath();
		String featuresDir = basePath + "features/";
		String cardsFeaturesDir = featuresDir + "cards/";
		String scoreFeaturesDir = featuresDir + "score/";
		String targetsDir = basePath + "targets/";
		String cardsTargetsDir = targetsDir + "cards/";
		String scoreTargetsDir = targetsDir + "score/";
		if (createIfNotExists(new File(featuresDir))
				&& createIfNotExists(new File(cardsTargetsDir))
				&& createIfNotExists(new File(scoreTargetsDir))) {
			try {
				String fileName = name + ".cbor";

				writeCBOR(dataSet.getCardsFeatures(), cardsFeaturesDir + fileName);
				writeCBOR(dataSet.getScoreFeatures(), scoreFeaturesDir + fileName);
				writeCBOR(dataSet.getCardsTargets(), cardsTargetsDir + fileName);
				writeCBOR(dataSet.getScoreTargets(), scoreTargetsDir + fileName);
			} catch (IOException e) {
				e.printStackTrace();
			}
			logger.info("Saved datasets to {}", Arena.DATASETS_BASE_PATH);
			return true;
		} else {
			logger.error("Failed to save datasets to {}", Arena.DATASETS_BASE_PATH);
			return false;
		}
	}
}
