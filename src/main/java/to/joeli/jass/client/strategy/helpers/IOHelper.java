package to.joeli.jass.client.strategy.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.client.strategy.training.CardsDataSet;
import to.joeli.jass.client.strategy.training.DataSet;
import to.joeli.jass.client.strategy.training.ScoreDataSet;
import to.joeli.jass.client.strategy.training.TrainMode;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class IOHelper {

	public static final Logger logger = LoggerFactory.getLogger(IOHelper.class);

	/**
	 * Writes a given object to a file at the given path in CBOR and JSON format
	 *
	 * @param array
	 * @param path
	 * @throws IOException
	 */
	public static void write(Object array, String path) throws IOException {
		writeCBOR(array, path);
		writeJSON(array, path);
	}


	public static void writeCBOR(Object array, String path) throws IOException {
		FileUtils.writeByteArrayToFile(new File(path + ".cbor"), convertToCBOR(array));
	}

	private static void writeJSON(Object array, String path) throws IOException {
		new ObjectMapper().writeValue(new File(path + ".json"), array);
	}

	/**
	 * Converts an object to CBOR (https://cbor.io/)
	 *
	 * @param array
	 * @return
	 * @throws JsonProcessingException
	 */
	public static byte[] convertToCBOR(Object array) throws JsonProcessingException {
		return new ObjectMapper(new CBORFactory()).writeValueAsBytes(array);
	}

	/*
	public static List readCBOR(Object array, String path) throws IOException {
		List features = convertFromCBOR(FileUtils.readFileToByteArray(new File(path)));
		System.out.println(features);
		return features;
	}

	public static List convertFromCBOR(byte[] bytes) throws IOException {
		return new ObjectMapper(new CBORFactory()).readValue(bytes, List.class);
	}
	*/

	/**
	 * Creates the directory and all necessary subdirectories if they do not yet exist.
	 *
	 * @param directory
	 * @return
	 */
	private static boolean createIfNotExists(File directory) {
		if (directory.isDirectory())
			return true;
		return directory.mkdirs();
	}

	/**
	 * Saves multiple files into the subdirectories "features" and "targets".
	 * These files can then be loaded and concatenated again to form the big dataset.
	 * The reason for not storing just one big file is that we cannot hold such big arrays in memory (Java throws OutOfMemoryErrors)
	 */
	public static boolean saveData(CardsDataSet cardsDataSet, ScoreDataSet scoreDataSet, String episodeNumber, String name) {
		return saveDataSet(cardsDataSet, episodeNumber, name) && saveDataSet(scoreDataSet, episodeNumber, name);
	}

	private static boolean saveDataSet(DataSet dataSet, String episodeNumber, String name) {
		if (createIfNotExists(new File(dataSet.getFeaturesPath(episodeNumber)))
				&& createIfNotExists(new File(dataSet.getTargetsPath(episodeNumber)))) {
			try {
				write(dataSet.getFeatures(), dataSet.getFeaturesPath(episodeNumber) + name);
				write(dataSet.getTargets(), dataSet.getTargetsPath(episodeNumber) + name);
			} catch (IOException e) {
				e.printStackTrace();
				logger.error("Failed to save datasets to {}", dataSet.getPath(episodeNumber));
				return false;
			}
			logger.info("Saved datasets to {}", dataSet.getPath(episodeNumber));
			return true;
		}
		logger.error("Failed to save datasets to {}", dataSet.getPath(episodeNumber));
		return false;
	}
}
