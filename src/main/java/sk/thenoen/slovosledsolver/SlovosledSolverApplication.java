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

import sk.thenoen.slovosledsolver.model.Game;
import sk.thenoen.slovosledsolver.model.Tile;

@SpringBootApplication
public class SlovosledSolverApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(SlovosledSolverApplication.class);

	@Resource
	private PageParser pageParser;

	@Resource
	private WordsFinder wordsFinder;

	@Resource
	private GameGenerator gameGenerator;

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

		final List<String> longestWords = words.stream()
											   .sorted(Comparator.comparing(String::length).reversed())
											   .filter(w -> w.length() > 4)
											   .limit(50)
											   .toList();

		logger.info("Longest words {}: {}", longestWords.size(), longestWords);

		List<Game> games = gameGenerator.generateAllPossibleGames(tiles, longestWords);


		Game maxGame = null;
		long progress = 0;

		logger.info("Number of games to play: {}", games.size());
		logger.info("Playing games ...");

		for (int i = 0; i < games.size(); i++) {
			final Game game = games.get(i);
			final long score = game.play();
			if (maxGame == null || score > maxGame.getScore()) {
				maxGame = game;
			}
			long newProgress = (i * 100) / games.size();
			if (newProgress != progress) {
				progress = newProgress;
				logger.info("Progress: {} %", progress);
			}
		}

		maxGame.printScore();

		logger.info("SlovosledSolverApplication finished");
	}
}
