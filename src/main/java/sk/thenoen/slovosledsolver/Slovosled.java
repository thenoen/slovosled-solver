package sk.thenoen.slovosledsolver;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class Slovosled {

	private static final List<String> WORD_HASHES = new ArrayList<>();
	private static MessageDigest DIGEST = null;

	private static Set<String> foundWords = new HashSet<>();


	private static List<String> getHashes() {
		String hashesCacheLocation = "/tmp/slovosled-hashes-cache.txt";

		var file = new File(hashesCacheLocation);

		if (file.exists()) {
			System.out.println("Hashes cache file exists - loading...");
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				var hashes = reader.lines().toList();
				return hashes;
			} catch (Exception ex) {
				ex.printStackTrace();
				System.exit(1);
			}
		} else {

			String content = downloadPage();

			var hashes = parseHashes(content);
			try (FileWriter fileWriter = new FileWriter(hashesCacheLocation, false)) {
				for (String hash : hashes) {
					fileWriter.write(hash.toString() + "\n");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				System.exit(1);
			}
			return Arrays.asList(hashes);
		}
		throw new RuntimeException("Unable to load hashes");
	}

	private static String[] parseHashes(String content) {

		Pattern pattern = Pattern.compile(".*?window.validWords = \"\\[(.*?)\\]\";.*?");
		var matcher = pattern.matcher(content);
		matcher.find();
		String words = matcher.group(1);
		var hashes = words.replaceAll("\\\\u0022", "").split(", ");
		System.out.println("Cashing downloaded hashes...");
		for (String hash : hashes) {
			System.out.println(hash);
		}
		return hashes;

	}

	private static String downloadPage() {
		System.out.println("Downloading page slovosled.dennikn.sk ...");
		String content = null;
		URLConnection connection = null;
		try {
			// connection = new URL("https://slovosled.dennikn.sk/").openConnection();
			connection = URL.of(URI.create("https://slovosled.dennikn.sk/"), null).openConnection();

			Scanner scanner = new Scanner(connection.getInputStream());
			scanner.useDelimiter("\\Z");
			content = scanner.next();
			scanner.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return content;
	}

	static void variations(List<Character> alphabet) {
		// var alphabet = new ArrayList<>(List.of('a', 'b', 'c', 'd', 'e'));

		for (int i = 0; i < alphabet.size(); i++) {
			final ArrayList<Character> newAlphabet = new ArrayList<>(alphabet);
			newAlphabet.remove(i);
			variations(List.of(alphabet.get(i)), newAlphabet, 1);
		}

	}

	static void variations(List<Character> prefix, List<Character> alphabet, long depth) {

		if (depth > 2 && depth <= 12) { //todo: <= 12
			// System.out.println(prefix);
			String word = prefix.stream()
								.map(Object::toString)
								.reduce("", String::concat);
			// System.out.println(word);

			if (!foundWords.contains(word)) {
				byte[] encodedhash = DIGEST.digest(word.getBytes(StandardCharsets.UTF_8));
				String sha256 = bytesToHex(encodedhash);
				if (WORD_HASHES.contains(sha256)) {
					System.out.println("Found word: " + word + " -> " + sha256);
					// WORD_HASHES.remove(sha256); // adding this will prevent finding of collisions
					foundWords.add(word);
				}
			}

		}

		for (int i = 0; i < alphabet.size(); i++) {
			final ArrayList<Character> newAlphabet = new ArrayList<>(alphabet);
			newAlphabet.remove(i);
			var newPrefix = new ArrayList<Character>(prefix);
			newPrefix.add(alphabet.get(i));
			variations(newPrefix, newAlphabet, depth + 1);
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

	static List<Character> parseGrid() {
		// String input = " <script>\n" +
		// " window.initialGrid =
		// [{\"letter\":\"I\",\"value\":1,\"usageCount\":0,\"selected\":false},{\"letter\":\"S\",\"value\":1,\"usageCount\":0,\"selected\":false},{\"letter\":\"T\",\"value\":1,\"usageCount\":0,\"selected\":false},{\"letter\":\"O\",\"value\":1,\"usageCount\":0,\"selected\":false},{\"letter\":\"U\",\"value\":1,\"usageCount\":0,\"selected\":false},{\"letter\":\"E\",\"value\":1,\"usageCount\":0,\"selected\":false},{\"letter\":\"B\",\"value\":1,\"usageCount\":0,\"selected\":false},{\"letter\":\"\\u00dd\",\"value\":1,\"usageCount\":0,\"selected\":false},{\"letter\":\"J\",\"value\":1,\"usageCount\":0,\"selected\":false},{\"letter\":\"K\",\"value\":1,\"usageCount\":0,\"selected\":false},{\"letter\":\"M\",\"value\":1,\"usageCount\":0,\"selected\":false},{\"letter\":\"I\",\"value\":1,\"usageCount\":0,\"selected\":false}];\n"
		// +
		// " window.initialHighscore = 709;\n" +
		// " window.bonus = {\"pattern\":\"3301\",\"value\":15,\"text\":\"za slovo
		// za\\u010d\\u00ednaj\\u00face na spoluhl\\u00e1sku\",\"index\":4};\n" +
		// " window.validWords =
		// \"[\\u0022840fd5faf05fb61bb96b8cae993d499c728409127319df820a484bebe7114ca3\\u0022,
		// \\u0022d9fb2fd8a59a8b7e4e490ac4238b5309eede272e2ea98dda07eee84ac476c6f5\\u0022,
		// \\u0022a9d3fdcdc86aa0c3f5e565165344ace621d16bb8c6278f4b807532c733769041\\u0022,
		// \\u002239f83e059c5a91edcab5259e5932d4a574c2c380d67c34b5939d20b034046cc0\\u0022,
		// \\";

		System.out.println("Getting grid letters...");

		String gridCacheLocation = "/tmp/slovosled-grid-cache.txt";
		var file = new File(gridCacheLocation);

		if (file.exists()) {
			System.out.println("Grid cache file exists - loading...");
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				var grid = reader.lines().toList().stream().map(line -> line.charAt(0)).toList();
				return grid;
			} catch (Exception ex) {
				ex.printStackTrace();
				System.exit(1);
			}
		} else {

			String content = downloadPage();

			final Pattern pattern = Pattern.compile(".*?window.initialGrid(.*?)}];");
			final Matcher matcher = pattern.matcher(content);
			matcher.find();
			matcher.groupCount();

			List<String> letters = new ArrayList<>(12);
			final Pattern letterPattern = Pattern.compile(".*?letter\":\"(.*?)\"");
			final Matcher letterMatcher = letterPattern.matcher(matcher.group(1));
			while (letterMatcher.find()) {
				letters.add(letterMatcher.group(1));
			}
			System.out.println(letters);

			List<Character> grid = new ArrayList<>(letters.size());

			for (String letter : letters) {
				if (letter.length() > 1) {
					int codePoint = Integer.parseInt(letter.substring(2, 6), 16); // Decimal int: 9989
					char[] checkChar = Character.toChars(codePoint);
					String check = String.valueOf(checkChar);
					grid.add(check.charAt(0));
				} else {
					grid.add(letter.charAt(0));
				}
			}

			System.out.println(grid);

			try (FileWriter fileWriter = new FileWriter(gridCacheLocation, false)) {
				for (Character letter : grid) {
					fileWriter.write(letter + "\n");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				System.exit(1);
			}

			return grid;
		}
		throw new RuntimeException("Unable to load grid");
	}

}
