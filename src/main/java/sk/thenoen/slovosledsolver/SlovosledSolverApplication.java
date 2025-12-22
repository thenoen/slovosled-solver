package sk.thenoen.slovosledsolver;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import sk.thenoen.slovosledsolver.model.Bonus;
import sk.thenoen.slovosledsolver.model.Tile;

@SpringBootApplication
public class SlovosledSolverApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(SlovosledSolverApplication.class);
	public static final int MIN_NUMBER_OF_WORDS = 5;
	public static final int MAX_NUMBER_OF_WORDS = 50;
	//	public static final int MAX_NUMBER_OF_WORDS = 10;

	@Resource
	private PageParser pageParser;

	@Resource
	private WordsFinder wordsFinder;

	@Resource
	private GameGenerator gameGenerator;

	@Resource
	private GamePlayer gamePlayer;

	public static void main(String[] args) {
		SpringApplication.run(SlovosledSolverApplication.class, args);
	}

	@Override
	public void run(String... args) {
		logger.info("SlovosledSolverApplication started");

		final String[] hashes = pageParser.retrieveHashes();

		//todo: verify that this is not faster implementation
		final HexFormat hexFormat = HexFormat.of();
		//		final byte[] bytes = hexFormat.parseHex(hashes[0]);
		//		final String hex = hexFormat.formatHex(bytes);
		//		final List<byte[]> hashesAsBytes = Arrays.stream(hashes)
		//												 .map(hexFormat::parseHex)
		//												 .toList();

		//		final List<String> grid = pageParser.retrieveGrid();
		final List<Tile> tiles = pageParser.retrieveLetters();
		final List<String> grid = tiles.stream()
									   .map(l -> l.getLetter())
									   .toList();

		final Bonus bonus = pageParser.retrieveBonus();

		final List<String> words = wordsFinder.findWords(grid, Set.of(hashes));

		logger.info("Number of parsed hashes: {}", hashes.length);
		logger.info("Number of found words:   {}", words.size());

		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		final List<String> hashList = new ArrayList<>();
		Arrays.asList(hashes).forEach(h -> hashList.add(h));
		for (String w : words) {
			byte[] encodedHash = digest.digest(w.getBytes(StandardCharsets.UTF_8));
			final String hex = hexFormat.formatHex(encodedHash);
			hashList.remove(hex);
		}
		hashList.forEach(h -> logger.info("remaining: {}", h));

		final Map<Integer, List<String>> wordsByLength = words.stream().collect(Collectors.groupingBy(String::length));
		final List<Integer> wordLengths = wordsByLength.keySet()
													   .stream()
													   .sorted(Comparator.comparingInt(Integer::intValue).reversed())
													   .collect(Collectors.toList());

		wordsByLength.keySet().forEach(l -> logger.info("Length {}: {} words", l, wordsByLength.get(l).size()));

		List<String> selectedWords = new ArrayList<>();
		for (int i = 0; i < wordLengths.size(); i++) {
			if (selectedWords.size() < MIN_NUMBER_OF_WORDS &&
				selectedWords.size() + wordsByLength.get(wordLengths.get(i)).size() <= MAX_NUMBER_OF_WORDS) {
				final Integer wordsLength = wordLengths.get(i);
				final List<String> wordsWithLength = wordsByLength.get(wordsLength);
				selectedWords.addAll(wordsWithLength);
				logger.info("Selected {} words with length {} characters", wordsWithLength.size(), wordsLength);
			} else {
				break;
			}
		}

		logger.info("Selected {} words for games", selectedWords.size());

		Map<String, List<List<Integer>>> allPossibleWordSelectionCombinations = gameGenerator.generateAllPossibleWordSelectionCombinations(tiles,
																																		   selectedWords);

		gamePlayer.playGames(selectedWords, tiles, bonus, allPossibleWordSelectionCombinations);

		logger.info("SlovosledSolverApplication finished");
	}

}
