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
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

@Component
public class WordsFinder {

	private static final Logger logger = LoggerFactory.getLogger(WordsFinder.class);

	private static final HexFormat HEX_FORMAT = HexFormat.of();
	private static MessageDigest DIGEST;
	private static List<Future<List<String>>> futures = new ArrayList<>();

	private static ThreadPoolTaskExecutor taskExecutor;

	public WordsFinder() {
		DIGEST = createMessageDigest();

		taskExecutor = new ThreadPoolTaskExecutorBuilder()
				.corePoolSize(100)
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
		for (Future<List<String>> future : futures) {
			try {
				future.get();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		taskExecutor.shutdown();
	}

	public List<String> findWords(List<String> alphabet, List<String> hashes) {

		logger.info("Finding words ...");

		List<String> foundWords = Collections.synchronizedList(new ArrayList<>(hashes.size()));

		for (int i = 0; i < alphabet.size(); i++) {
			final ArrayList<String> newAlphabet = new ArrayList<>(alphabet);
			newAlphabet.remove(i);
			variations(List.of(alphabet.get(i)), newAlphabet, 1, foundWords, hashes);
		}

		shutdown();
		logger.info("Found {} words", foundWords.size());
		return foundWords;
	}

	private static void variations(List<String> prefix,
										   List<String> alphabet,
										   long depth,
										   List<String> foundWords,
										   List<String> hashes) {

		//		if (depth > 2 && depth <= 12) { //todo: <= 12
		if (depth > 0 && depth <= 12) {
			String word = prefix.stream()
								.map(Object::toString)
								.reduce("", String::concat);

//			if (!foundWords.contains(word)) {
				byte[] encodedHash = createMessageDigest().digest(word.getBytes(StandardCharsets.UTF_8));
				//				final String sha256 = bytesToHex(encodedHash);
				final String sha256 = HEX_FORMAT.formatHex(encodedHash);
				if (hashes.contains(sha256)) {
					//				if (containsBytes(hashes, encodedHash)) {
					logger.info("Found word: {} -> {}", word, bytesToHex(encodedHash));
					// WORD_HASHES.remove(sha256); // adding this will prevent finding of collisions
					foundWords.add(word);
				}
//			}

		}

		if (depth % 6 == 0) {
//			List<Future<List<String>>> futures = new ArrayList<>();
			for (int i = 0; i < alphabet.size(); i++) {

				final ArrayList<String> newAlphabet = new ArrayList<>(alphabet);
				newAlphabet.remove(i);
				var newPrefix = new ArrayList<>(prefix);
				newPrefix.add(alphabet.get(i));

				final Future<List<String>> future = taskExecutor.submit(() -> {
//					List<String> newFoundWords = Collections.synchronizedList(new ArrayList<>());
					List<String> newFoundWords = new ArrayList<>();
					variations(newPrefix, newAlphabet, depth + 1, foundWords, hashes);
					return newFoundWords;
				});
				futures.add(future);
			}

//			for (Future<List<String>> future : futures) {
//				try {
//					future.get();
//					foundWords.addAll(future.get());
//				} catch (Exception ex) {
//					throw new RuntimeException(ex);
//				}
//			}
//			futures.clear();
//			return foundWords;
			return;
		}

		for (int i = 0; i < alphabet.size(); i++) {
			final ArrayList<String> newAlphabet = new ArrayList<>(alphabet);
			newAlphabet.remove(i);
			var newPrefix = new ArrayList<>(prefix);
			newPrefix.add(alphabet.get(i));
			variations(newPrefix, newAlphabet, depth + 1, foundWords, hashes);
		}
//		return foundWords;
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
