package org.ui.thesis.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
public class JsonFileUtil {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Reads a JSON array from a file and deserializes it into a List of type T. This
	 * handles generic type erasure using Jackson's TypeFactory.
	 * @param <T> The class type of the objects in the JSON array.
	 * @param filePath The path to the input JSON file.
	 * @param elementClass The Class object corresponding to type T (e.g.,
	 * UserRecord.class).
	 * @return A List of objects of type T, or an empty list if an error occurs.
	 */
	public static <T> List<T> readJsonArrayFromFile(String filePath, Class<T> elementClass) {
		File file = new File(filePath);
		if (!file.exists()) {
			log.error("Error: File not found at path: {}", filePath);
			return Collections.emptyList();
		}

		try {
			TypeFactory typeFactory = objectMapper.getTypeFactory();

			CollectionType listType = typeFactory.constructCollectionType(List.class, elementClass);

			return objectMapper.readValue(file, listType);

		}
		catch (IOException e) {
			log.error("Error reading or parsing JSON file: {}", filePath);
			log.error(Arrays.toString(e.getStackTrace()));
			return Collections.emptyList();
		}
	}

	/**
	 * Serializes a List of objects of type T into a JSON array and writes it to a file.
	 * @param <T> The class type of the objects in the list.
	 * @param dataList The List of objects to be written.
	 * @param filePath The path to the output JSON file.
	 * @return true if the write operation was successful, false otherwise.
	 */
	public static <T> boolean writeJsonArrayToFile(List<T> dataList, String filePath) {
		try {
			File file = new File(filePath);

			// Write the list directly. Jackson will serialize it as a JSON array.
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, dataList);
			log.info("Successfully wrote data to file: {}", filePath);
			return true;
		}
		catch (IOException e) {
			log.error("Error writing data to JSON file: {}", filePath);
			log.error(Arrays.toString(e.getStackTrace()));
			return false;
		}
	}

	/**
	 * Serializes a List of objects of type T into a JSON array and writes it to a file.
	 * @param <T> The class type of the objects in the list.
	 * @param data The Object to be written.
	 * @param filePath The path to the output JSON file.
	 * @return true if the write operation was successful, false otherwise.
	 */
	public static <T> boolean writeObjectToFile(Object data, String filePath) {
		try {
			File file = new File(filePath);

			// Write the list directly. Jackson will serialize it as a JSON array.
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
			log.info("Successfully wrote data to file: {}", filePath);
			return true;
		}
		catch (IOException e) {
			log.error("Error writing data to JSON file: {}", filePath);
			log.error(Arrays.toString(e.getStackTrace()));
			return false;
		}
	}

	/**
	 * Reads a JSON file into a complex generic structure defined by a TypeReference. This
	 * is the most flexible reading method for nested types (Maps, Lists of Maps, etc.).
	 * @param <T> The target complex type.
	 * @param filePath The path to the input JSON file.
	 * @param typeRef The TypeReference defining the full generic structure (e.g., new
	 * TypeReference<Map<String, List<AnswerScoreDto>>>() {}).
	 * @return The deserialized object of type T, or null on error.
	 */
	public static <T> T readJsonByReference(String filePath, TypeReference<T> typeRef) {
		File file = new File(filePath);
		if (!file.exists()) {
			log.info("Error: File not found at path: {}", filePath);
			return null;
		}

		try {
			return objectMapper.readValue(file, typeRef);
		}
		catch (IOException e) {
			log.error("Error reading or parsing complex JSON file: {}", filePath);
			log.error(Arrays.toString(e.getStackTrace()));
			return null;
		}
	}

	/**
	 * Serializes a complex generic object T into a JSON file.
	 * @param <T> The complex object type.
	 * @param dataObject The complex object to write (e.g., a Map).
	 * @param filePath The path to the output JSON file.
	 * @return true if the write operation was successful, false otherwise.
	 */
	public static <T> boolean writeJsonByReference(T dataObject, String filePath) {
		try {
			File file = new File(filePath);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, dataObject);
			log.info("Successfully wrote complex data to file: {}", filePath);
			return true;
		}
		catch (IOException e) {
			log.error("Error writing complex data to JSON file: {}", filePath);
			log.error(Arrays.toString(e.getStackTrace()));
			return false;
		}
	}

}
