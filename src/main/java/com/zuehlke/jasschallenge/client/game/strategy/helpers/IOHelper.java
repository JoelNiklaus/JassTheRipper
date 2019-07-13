package com.zuehlke.jasschallenge.client.game.strategy.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class IOHelper {

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
}
