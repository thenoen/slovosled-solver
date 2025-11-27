package sk.thenoen.slovosledsolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import sk.thenoen.slovosledsolver.util.CacheUtils;

@Component
public class WordsFinder {

	private static final Logger logger = LoggerFactory.getLogger(WordsFinder.class);

	private static final HexFormat HEX_FORMAT = HexFormat.of();
	private static List<Future<List<String>>> futures = new ArrayList<>();

	private static ThreadPoolTaskExecutor taskExecutor;
	private static ThreadLocal<MessageDigest> threadLocalDigest = ThreadLocal.withInitial(WordsFinder::createMessageDigest);

	public WordsFinder() {
		taskExecutor = new ThreadPoolTaskExecutorBuilder()
				.corePoolSize(15)
				.maxPoolSize(100)
				.queueCapacity(1_000_000_000)
				.build();
		taskExecutor.initialize();
	}

	private static MessageDigest createMessageDigest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (Exception ex) {
			throw new RuntimeException("Unable to initialize MessageDigest", ex);
		}
	}

	private void shutdown() {

		long maxTasks = taskExecutor.getQueueSize();
		long progress = 0;

		do {
			long currentTasks = taskExecutor.getQueueSize();
			long newProgress = (currentTasks * 100) / maxTasks;

			if (newProgress != progress) {
				logger.info("Waiting for tasks to finish ... {}% \t Active: {},\tQueue: {}",
							newProgress,
							taskExecutor.getActiveCount(),
							taskExecutor.getQueueSize());
				progress = newProgress;
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		} while (taskExecutor.getActiveCount() > 0);

		for (Future<List<String>> future : futures) {
			try {
				future.get();
			} catch (Exception ex) {
				logger.error("Error while waiting for task to finish", ex);
			}
		}

		taskExecutor.shutdown();
	}

	public List<String> findWords(List<String> alphabet, Set<String> hashes) {

		logger.info("Finding words ...");

		try {
			final List<String> strings = CacheUtils.readCachedWords();
			if (!strings.isEmpty()) {
				return strings;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		List<String> foundWords = Collections.synchronizedList(new ArrayList<>(hashes.size()));

		for (int i = 0; i < alphabet.size(); i++) {
			final ArrayList<String> newAlphabet = new ArrayList<>(alphabet);
			newAlphabet.remove(i);
			variations(List.of(alphabet.get(i)), newAlphabet, 1, foundWords, hashes);
		}

		shutdown();

		final Set<String> uniqueWords = HashSet.newHashSet(hashes.size());
		uniqueWords.addAll(foundWords);

		logger.info("Finding words finished");
		final List<String> uniqueWordList = uniqueWords.stream().toList();
		CacheUtils.cacheWords(uniqueWordList);
		return uniqueWordList;
	}

	private static void variations(List<String> prefix,
								   List<String> alphabet,
								   long depth,
								   List<String> foundWords,
								   Set<String> hashes) {

		if (depth > 2 && depth <= 12) {
			final StringBuilder stringBuilder = new StringBuilder();
			prefix.forEach(stringBuilder::append);
			String word = stringBuilder.toString();

			byte[] encodedHash = threadLocalDigest.get().digest(word.getBytes(StandardCharsets.UTF_8));
			final String sha256 = HEX_FORMAT.formatHex(encodedHash);
			if (hashes.contains(sha256)) {
				logger.debug("Found word: {} -> {}", word, bytesToHex(encodedHash));
				// WORD_HASHES.remove(sha256); // adding this will prevent finding of collisions
				foundWords.add(word);
			}
		}

		if (depth % 6 == 0) { // This seems to be ideal for 12 CPU cores (5 - too many tasks for Integer type, 7 too few tasks for 12 CPU cores)
			for (int i = 0; i < alphabet.size(); i++) {

				final ArrayList<String> newAlphabet = new ArrayList<>(alphabet);
				newAlphabet.remove(i);
				var newPrefix = new ArrayList<>(prefix);
				newPrefix.add(alphabet.get(i));

				final Future<List<String>> future = taskExecutor.submit(() -> {
					List<String> newFoundWords = new ArrayList<>();
					variations(newPrefix, newAlphabet, depth + 1, foundWords, hashes);
					return newFoundWords;
				});
				futures.add(future);
			}
			return;
		}

		for (int i = 0; i < alphabet.size(); i++) {
			final ArrayList<String> newAlphabet = new ArrayList<>(alphabet);
			newAlphabet.remove(i);
			var newPrefix = new ArrayList<>(prefix);
			newPrefix.add(alphabet.get(i));
			variations(newPrefix, newAlphabet, depth + 1, foundWords, hashes);
		}
	}

	private static String bytesToHex(byte[] hash) {
		StringBuilder hexString = new StringBuilder(2 * hash.length);
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xff & hash[i]);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}

	private static boolean containsBytes(List<byte[]> hashes, byte[] hash) {
		for (byte[] h : hashes) {
			if (MessageDigest.isEqual(h, hash)) {
				return true;
			}
		}
		return false;
	}

}
