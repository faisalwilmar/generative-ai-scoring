package org.ui.thesis.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class TextFileUtil {

    /**
     * Reads all lines from a specified text file.
     * * @param filePath The path to the text file.
     * @return A List of strings, where each string is a line from the file.
     */
    public static List<String> readLines(String filePath) {
        Path path = Paths.get(filePath);
        try {
            // Files.readAllLines is the simplest way to read a small-to-medium file into a List<String>.
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("ERROR: Could not read file: {}", filePath);
            log.error(Arrays.toString(e.getStackTrace()));
            return List.of();
        }
    }

    /**
     * Writes a list of strings (lines) to a specified text file.
     * Overwrites the file if it already exists.
     * * @param lines The list of strings to write, where each string will be a new line.
     * @param filePath The path to the text file.
     */
    public static void writeLines(List<String> lines, String filePath) {
        Path path = Paths.get(filePath);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
            log.info("SUCCESS: Successfully wrote {} lines to {}", lines.size(), filePath);
        } catch (IOException e) {
            log.error("ERROR: Could not write file: {}", filePath);
            log.error(Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * Reads the entire content of a file into a single String.
     * @param filePath The path to the text file.
     * @return The entire file content as one String, or an empty String on error.
     */
    public static String readAllText(String filePath) {
        Path path = Paths.get(filePath);
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("ERROR: Could not read entire file content: {}", filePath);
            log.error(Arrays.toString(e.getStackTrace()));
            return "";
        }
    }

    /**
     * Writes a single String of content entirely to a file.
     * Overwrites the file if it already exists.
     * @param content The single String containing all text to write.
     * @param filePath The path to the text file.
     */
    public static void writeAllText(String content, String filePath) {
        Path path = Paths.get(filePath);
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8);
            log.info("SUCCESS: Successfully wrote full content to {}", filePath);
        } catch (IOException e) {
            log.error("ERROR: Could not write full content to file: {}", filePath);
            log.error(Arrays.toString(e.getStackTrace()));
        }
    }
}
