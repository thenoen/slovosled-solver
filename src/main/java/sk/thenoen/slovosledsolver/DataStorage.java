package sk.thenoen.slovosledsolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataStorage {

	private static final Logger logger = LoggerFactory.getLogger(DataStorage.class);
	private static final List<List<String>> WORD_COMBINATIONS_CACHE = new ArrayList<>();
	public static final int MB = 1024 * 1024;

	private final String wordCombinationsCacheLocation;

	public DataStorage(@Value("${data.storage.location}") String dataStorageLocation) {
		wordCombinationsCacheLocation = dataStorageLocation + "/word-combinations-cache.txt";
		final File wordCombinationsFile = new File(wordCombinationsCacheLocation);
		if (wordCombinationsFile.exists()) {
			wordCombinationsFile.delete(); //todo: for tuning purposes, eventually replace by skipping of generating of the list, maybe do some validation of size, fix tests
			logger.info("Word combinations cache file exists - deleting...");
		} else {
			logger.info("Word combinations cache file does not exist - creating... {}", wordCombinationsCacheLocation);
			wordCombinationsFile.getParentFile().mkdirs();
		}
	}

	public synchronized void storeWordCombination(List<String> wordCombination) {
		WORD_COMBINATIONS_CACHE.add(wordCombination);

		if (WORD_COMBINATIONS_CACHE.size() > 1_000_000) {
			flushCacheToDisk();
		}
	}

	private void flushCacheToDisk() {

		try (var fileWriter = new FileWriter(wordCombinationsCacheLocation, true);
			 var bufferedWriter = new BufferedWriter(fileWriter, 100 * MB)) {
			for (List<String> wordCombination : WORD_COMBINATIONS_CACHE) {
				bufferedWriter.write(String.join(",", wordCombination));
				bufferedWriter.newLine();
			}
			bufferedWriter.flush();
			WORD_COMBINATIONS_CACHE.clear();
		} catch (Exception e) {
			throw new RuntimeException("Unable to store word combinations to disk", e);
		}

	}
}
