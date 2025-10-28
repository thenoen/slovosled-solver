package sk.thenoen.slovosledsolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import sk.thenoen.slovosledsolver.model.Letter;

@Component
public class PageParser {

	private static final Logger logger = LoggerFactory.getLogger(PageParser.class);

	private PageDownloader pageDownloader;
	private ObjectMapper objectMapper;

	public PageParser(PageDownloader pageDownloader,
					  ObjectMapper objectMapper) {
		this.pageDownloader = pageDownloader;
		this.objectMapper = objectMapper;
	}

	public String[] retrieveHashes() {
		final String pageContent = pageDownloader.retrievePageContent();
		// todo: here should be caching
		return parseHashes(pageContent);
	}

	public List<String> retrieveGrid() {
		final String pageContent = pageDownloader.retrievePageContent();
		// todo: here should be caching
		return parseGrid(pageContent);
	}

	public List<Letter> retrieveLetters() {

		final String pageContent = pageDownloader.retrievePageContent();

		final Pattern pattern = Pattern.compile(".*?window.initialGrid = (.*?);");
		final Matcher matcher = pattern.matcher(pageContent);
		matcher.find();

		final String group = matcher.group(1);

		Letter[] letters;
		try {
			 letters = objectMapper.readerForArrayOf(Letter.class).readValue(group);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

//		final ArrayList<Letter> letters = new ArrayList<>();


		return List.of(letters);
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

}
