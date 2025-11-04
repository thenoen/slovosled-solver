package sk.thenoen.slovosledsolver.util;

import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;

public class CacheUtils {

	private static final Logger logger = org.slf4j.LoggerFactory.getLogger(CacheUtils.class);

	private static final String WORDS_CACHE_LOCATION = "/tmp/slovosled-words-cache.txt";

	public static void cacheWords(List<String> words) {
		logger.info("Caching words...");
		try (var writer = new BufferedWriter(new FileWriter(WORDS_CACHE_LOCATION, true))) {
			for (String word : words) {
				writer.write(word);
				writer.newLine();
			}
			writer.flush();
		} catch (Exception e) {
			logger.error("Error while caching words");
			throw new RuntimeException("Unable to cache words", e);
		}
		logger.info("Words cached");
	}

	public static List<String> readCachedWords() {
		logger.info("Reading cached words...");
		try (var reader = new java.io.BufferedReader(new java.io.FileReader(WORDS_CACHE_LOCATION))) {
			final List<String> words = reader.lines().toList();
			logger.info("Cached words read");
			return words;
		} catch (Exception e) {
			logger.warn("Unable to reach words cache", e);
			return List.of();
		}
	}

}
