package to.joeli.jass.client.strategy.helpers

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.cbor.CBORFactory
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import to.joeli.jass.client.strategy.training.data.CardsDataSet
import to.joeli.jass.client.strategy.training.data.DataSet
import to.joeli.jass.client.strategy.training.data.ScoreDataSet
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

object IOHelper {

    val logger: Logger = LoggerFactory.getLogger(IOHelper::class.java)

    fun getQuantifierFromFile(filename: String): Double {
        try {
            return java.lang.Double.parseDouble(String(Files.readAllBytes(Paths.get(DataSet.BASE_PATH + filename))))
        } catch (e: IOException) {
            logger.error("{}", e)
        }

        return java.lang.Double.MAX_VALUE
    }

    /**
     * Writes a given object to a file at the given path in CBOR and JSON format
     *
     * @param array
     * @param path
     * @throws IOException
     */
    @Throws(IOException::class)
    fun write(array: Any, path: String) {
        writeCBOR(array, path)
        // INFO: Only write to JSON when we want to debug. Otherwise it is a waste of resources
        // writeJSON(array, path)
    }


    @Throws(IOException::class)
    fun writeCBOR(array: Any, path: String) {
        FileUtils.writeByteArrayToFile(File("$path.cbor"), convertToCBOR(array))
    }

    @Throws(IOException::class)
    private fun writeJSON(array: Any, path: String) {
        ObjectMapper().writeValue(File("$path.json"), array)
    }

    /**
     * Converts an object to CBOR (https://cbor.io/)
     *
     * @param array
     * @return
     * @throws JsonProcessingException
     */
    @Throws(JsonProcessingException::class)
    fun convertToCBOR(array: Any): ByteArray {
        return ObjectMapper(CBORFactory()).writeValueAsBytes(array)
    }

    /**
     * Creates the directory and all necessary subdirectories if they do not yet exist.
     *
     * @param directory
     * @return
     */
    private fun createIfNotExists(directory: File): Boolean {
        return if (directory.isDirectory) true else directory.mkdirs()
    }

    /**
     * Saves multiple files into the subdirectories "features" and "targets".
     * These files can then be loaded and concatenated again to form the big dataset.
     * The reason for not storing just one big file is that we cannot hold such big arrays in memory (Java throws OutOfMemoryErrors)
     */
    fun saveData(cardsDataSet: CardsDataSet, scoreDataSet: ScoreDataSet, episode: Int, dataSetType: String, name: String): Boolean {
        return saveDataSet(cardsDataSet, episode, dataSetType, name) && saveDataSet(scoreDataSet, episode, dataSetType, name)
    }

    private fun saveDataSet(dataSet: DataSet, episode: Int, dataSetType: String, name: String): Boolean {
        if (createIfNotExists(File(dataSet.getFeaturesPath(episode, dataSetType))) && createIfNotExists(File(dataSet.getTargetsPath(episode, dataSetType)))) {
            try {
                write(dataSet.features, dataSet.getFeaturesPath(episode, dataSetType) + name)
                write(dataSet.targets, dataSet.getTargetsPath(episode, dataSetType) + name)
            } catch (e: IOException) {
                e.printStackTrace()
                logger.error("Failed to save datasets to {}", dataSet.getNetworkTypePath(episode))
                return false
            }

            logger.info("Saved datasets to {}", dataSet.getNetworkTypePath(episode))
            return true
        }
        logger.error("Failed to save datasets to {}", dataSet.getNetworkTypePath(episode))
        return false
    }
}
