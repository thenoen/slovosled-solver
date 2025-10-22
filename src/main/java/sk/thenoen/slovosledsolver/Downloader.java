package sk.thenoen.slovosledsolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class Downloader {

	private static final Logger logger = LoggerFactory.getLogger(Downloader.class);

	private static final String PAGE_CACHE_LOCATION = "/tmp/slovosled-page-cache.txt";

	public String[] retrieveHashes() {
		final String pageContent = retrievePageContent();
		return parseHashes(pageContent);
	}

	public List<String> retrieveGrid() {
		final String pageContent = retrievePageContent();
		return parseGrid(pageContent);
	}

	private static String retrievePageContent() {
		var pageCacheFile = new File(PAGE_CACHE_LOCATION);

		String pageContent = null;

		if (pageCacheFile.exists()) {
			pageContent = readPageFromCache(pageCacheFile);
		} else {
			pageContent = downloadPage();
			cachePageContent(pageCacheFile, pageContent);
		}
		return pageContent;
	}

	private static List<String> parseGrid(String content) {
		final Pattern pattern = Pattern.compile(".*?window.initialGrid(.*?)}];");
		final Matcher matcher = pattern.matcher(content);
		matcher.find();

		List<String> letters = new ArrayList<>(12);
		final Pattern letterPattern = Pattern.compile(".*?letter\":\"(.*?)\"");
		final Matcher letterMatcher = letterPattern.matcher(matcher.group(1));
		while (letterMatcher.find()) {
			letters.add(letterMatcher.group(1));
		}
		logger.info("Parsed grid:\t{}", Arrays.toString(letters.toArray()));

		List<String> grid = new ArrayList<>(letters.size());

		for (String letter : letters) {
			if (letter.length() > 1) {
				int codePoint = Integer.parseInt(letter.substring(2, 6), 16); // Decimal int: 9989
				char[] checkChar = Character.toChars(codePoint);
				String check = String.valueOf(checkChar);
				grid.add(check);
			} else {
				grid.add(letter);
			}
		}

		logger.info("Converted grid:\t{}", grid);

		return grid;
	}

	private static void cachePageContent(File pageCacheFile, String pageContent) {
		try (FileWriter fileWriter = new FileWriter(pageCacheFile, false)) {
			fileWriter.write(pageContent);
		} catch (Exception ex) {
			throw new RuntimeException("Unable to save page to cache", ex);
		}
	}

	private static String[] parseHashes(String content) {

		Pattern pattern = Pattern.compile(".*?window.validWords = \"\\[(.*?)\\]\";.*?");
		var matcher = pattern.matcher(content);
		matcher.find();
		String words = matcher.group(1);
		var hashes = words.replaceAll("\\\\u0022", "").split(", ");

		logger.info("Parsed {} hashes ({} ... {})", hashes.length, hashes[0], hashes[hashes.length - 1]);

		logger.info("Cashing downloaded hashes...");
		for (String hash : hashes) {
			logger.debug(hash);
		}
		return hashes;

	}

	private static String readPageFromCache(File fileCache) {
		try (BufferedReader reader = new BufferedReader(new FileReader(fileCache))) {
			return reader.lines()
						 .collect(Collectors.joining());

		} catch (Exception ex) {
			throw new RuntimeException("Unable to read page from cache", ex);
		}
	}

	private static String downloadPage() {
		logger.info("Downloading page slovosled.dennikn.sk ...");
		String content = null;
		URLConnection connection = null;
		try {
			connection = URL.of(URI.create("https://slovosled.dennikn.sk/"), null)
							.openConnection();

			Scanner scanner = new Scanner(connection.getInputStream());
			scanner.useDelimiter("\\Z");
			content = scanner.next();
			scanner.close();
		} catch (Exception ex) {
			throw new RuntimeException("Unable to download page", ex);
		}
		return content;
	}

}
