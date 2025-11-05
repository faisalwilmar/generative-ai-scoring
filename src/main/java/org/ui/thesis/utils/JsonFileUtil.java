package org.ui.thesis.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class JsonFileUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Reads a JSON array from a file and deserializes it into a List of type T.
     * This handles generic type erasure using Jackson's TypeFactory.
     *
     * @param <T> The class type of the objects in the JSON array.
     * @param filePath The path to the input JSON file.
     * @param elementClass The Class object corresponding to type T (e.g., UserRecord.class).
     * @return A List of objects of type T, or an empty list if an error occurs.
     */
    public static <T> List<T> readJsonArrayFromFile(String filePath, Class<T> elementClass) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("Error: File not found at path: " + filePath);
            return Collections.emptyList();
        }

        try {
            // 1. Get the TypeFactory
            TypeFactory typeFactory = objectMapper.getTypeFactory();

            // 2. Create the CollectionType (List<T>)
            CollectionType listType = typeFactory.constructCollectionType(List.class, elementClass);

            // 3. Read the file content and map it to the defined type
            return objectMapper.readValue(file, listType);

        } catch (IOException e) {
            System.err.println("Error reading or parsing JSON file: " + filePath);
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Serializes a List of objects of type T into a JSON array and writes it to a file.
     *
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
            System.out.println("Successfully wrote data to file: " + filePath);
            return true;
        } catch (IOException e) {
            System.err.println("Error writing data to JSON file: " + filePath);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Serializes a List of objects of type T into a JSON array and writes it to a file.
     *
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
            System.out.println("Successfully wrote data to file: " + filePath);
            return true;
        } catch (IOException e) {
            System.err.println("Error writing data to JSON file: " + filePath);
            e.printStackTrace();
            return false;
        }
    }
}
