package sk.thenoen.slovosledsolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Component
public class WordsFinder {

	private static final Logger logger = LoggerFactory.getLogger(WordsFinder.class);

	private static final HexFormat HEX_FORMAT = HexFormat.of();
	private static MessageDigest DIGEST;

	public WordsFinder() {
		try {
			DIGEST = MessageDigest.getInstance("SHA-256");
		} catch (Exception ex) {
			throw new RuntimeException("Unable to initialize MessageDigest", ex);
		}
	}

	public List<String> findWords(List<String> alphabet, List<String> hashes) {

		logger.info("Finding words ...");

		List<String> foundWords = new ArrayList<>(hashes.size());

		for (int i = 0; i < alphabet.size(); i++) {
			final ArrayList<String> newAlphabet = new ArrayList<>(alphabet);
			newAlphabet.remove(i);
			variations(List.of(alphabet.get(i)), newAlphabet, 1, foundWords, hashes);
		}

		logger.info("Found {} words", foundWords.size());
		return foundWords;
	}

	private static List<String> variations(List<String> prefix,
										   List<String> alphabet,
										   long depth,
										   List<String> foundWords,
										   List<String> hashes) {

		if (depth > 2 && depth <= 12) { //todo: <= 12
			String word = prefix.stream()
								.map(Object::toString)
								.reduce("", String::concat);

			if (!foundWords.contains(word)) {
				byte[] encodedHash = DIGEST.digest(word.getBytes(StandardCharsets.UTF_8));
				//				final String sha256 = bytesToHex(encodedHash);
				final String sha256 = HEX_FORMAT.formatHex(encodedHash);
				if (hashes.contains(sha256)) {
					//				if (containsBytes(hashes, encodedHash)) {
					logger.info("Found word: {} -> {}", word, bytesToHex(encodedHash));
					// WORD_HASHES.remove(sha256); // adding this will prevent finding of collisions
					foundWords.add(word);
				}
			}

		}

		for (int i = 0; i < alphabet.size(); i++) {
			final ArrayList<String> newAlphabet = new ArrayList<>(alphabet);
			newAlphabet.remove(i);
			var newPrefix = new ArrayList<>(prefix);
			newPrefix.add(alphabet.get(i));
			variations(newPrefix, newAlphabet, depth + 1, foundWords, hashes);
		}
		return foundWords;
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
