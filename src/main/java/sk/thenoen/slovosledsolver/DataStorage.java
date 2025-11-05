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
import java.util.stream.Collectors;

@Component
public class DataStorage {

	private static final Logger logger = LoggerFactory.getLogger(DataStorage.class);
	private static final List<List<String>> WORD_COMBINATIONS_CACHE = new ArrayList<>();
	private static final List<List<Short>> WORD_INDEX_COMBINATIONS_CACHE = new ArrayList<>();
	public static final int MB = 1024 * 1024;

	private final String wordCombinationsCacheLocation;
	private final String wordIndexCombinationsCacheLocation;

	public DataStorage(@Value("${data.storage.location}") String dataStorageLocation) {
		wordCombinationsCacheLocation = dataStorageLocation + "/word-combinations-cache.txt";
		initWordCombinationCacheFile(wordCombinationsCacheLocation);

		wordIndexCombinationsCacheLocation = dataStorageLocation + "/word-index-combinations-cache.txt";
		initWordCombinationCacheFile(wordIndexCombinationsCacheLocation);

	}

	private void initWordCombinationCacheFile(String location) {
		final File wordCombinationsFile = new File(location);
		if (wordCombinationsFile.exists()) {
			wordCombinationsFile.delete(); //todo: for tuning purposes, eventually replace by skipping of generating of the list, maybe do some validation of size, fix tests
			logger.info("Word combinations cache file exists - deleting...");
		} else {
			logger.info("Word combinations cache file does not exist - creating... {}", wordCombinationsFile);
			wordCombinationsFile.getParentFile().mkdirs();
		}
	}

	public synchronized void storeWordCombination(List<String> wordCombination) {
		WORD_COMBINATIONS_CACHE.add(wordCombination);

		if (WORD_COMBINATIONS_CACHE.size() > 1_000_000) {
			flushCacheToDisk();
		}
	}

	public synchronized void storeWordIndexCombination(List<Short> wordCombination) {
		WORD_INDEX_COMBINATIONS_CACHE.add(wordCombination);

		if (WORD_INDEX_COMBINATIONS_CACHE.size() > 1_000_000) {
			flushIndexCacheToDisk();
		}
	}

	public void flushCacheToDisk() {

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

	public void flushIndexCacheToDisk() {

		try (var fileWriter = new FileWriter(wordIndexCombinationsCacheLocation, true);
			 var bufferedWriter = new BufferedWriter(fileWriter, 100 * MB)) {
			for (List<Short> wordCombination : WORD_INDEX_COMBINATIONS_CACHE) {

				bufferedWriter.write(wordCombination.stream()
													.map(String::valueOf)
													.collect(Collectors.joining(",")));
				bufferedWriter.newLine();
			}
			bufferedWriter.flush();
			WORD_INDEX_COMBINATIONS_CACHE.clear();
		} catch (Exception e) {
			throw new RuntimeException("Unable to store word combinations to disk", e);
		}

	}
}
