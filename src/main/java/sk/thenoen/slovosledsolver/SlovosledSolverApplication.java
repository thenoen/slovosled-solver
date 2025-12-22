package sk.thenoen.slovosledsolver;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import sk.thenoen.slovosledsolver.model.Bonus;
import sk.thenoen.slovosledsolver.model.Game;
import sk.thenoen.slovosledsolver.model.Tile;

@SpringBootApplication
public class SlovosledSolverApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(SlovosledSolverApplication.class);
	public static final int MIN_NUMBER_OF_WORDS = 5;
	public static final int MAX_NUMBER_OF_WORDS = 50;
	//	public static final int MAX_NUMBER_OF_WORDS = 10;

	@Value("${slovosled.submit-best-score:false}")
	private boolean submitBestScore;

	@Resource
	private PageParser pageParser;

	@Resource
	private WordsFinder wordsFinder;

	@Resource
	private GameGenerator gameGenerator;

	@Resource
	private DataStorage dataStorage;

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

		playGames(selectedWords, tiles, bonus, allPossibleWordSelectionCombinations);

		logger.info("SlovosledSolverApplication finished");
	}

	public void playGames(List<String> words,
						  List<Tile> tiles,
						  Bonus bonus,
						  Map<String, List<List<Integer>>> allPossibleWordsSelections) {

		long wordIndexCombinationCount = 1;
		for (int i = 0; i < 5; i++) {
			wordIndexCombinationCount *= words.size() - i;
		}

		long bestScore = 0;
		Game bestGame = null;
		boolean firstGameSubmitted = false;
		final long initialHighScore = pageParser.getInitialHighScore();
		logger.info("Initial high score: {}", initialHighScore);
		long currentIndex = 0;
		long progress = -1;
		final Stream<String> wordIndexCombinationStream = dataStorage.readWordIndexCombinationsFromDisk();
		final Iterator<String> iterator = wordIndexCombinationStream.iterator();
		while (iterator.hasNext()) {
			final String wordIndexCombination = iterator.next();
			final List<Short> indicesOfSelectedWords = Arrays.stream(wordIndexCombination.split(","))
															 .map(Short::parseShort)
															 .toList();
			final List<List<List<Integer>>> wordSelectionCombinations = generateWordSelectionCombinations(0,
																										  words,
																										  indicesOfSelectedWords,
																										  new ArrayList<>(new ArrayList<>()),
																										  allPossibleWordsSelections);

			currentIndex++;
			for (List<List<Integer>> wordSelectionCombination : wordSelectionCombinations) {
				final List<String> selectedWords = indicesOfSelectedWords.stream()
																		 .map(words::get)
																		 .toList();
				final Game game = new Game(tiles, bonus, selectedWords, wordSelectionCombination);
				final long score = game.play();

				if (score > bestScore) {
					bestScore = score;
					bestGame = game;
					//					logger.info("Found best score: {} for word combination {}", bestScore, selectedWords);
					logger.info("Found best score: {} for word combination:", bestScore);
					for (int i = 0; i < selectedWords.size(); i++) {
						logger.info("\t{}: {}",
									selectedWords.get(i).replaceAll("[A-Ž]", "*"),
								//									selectedWords.get(i),
									wordSelectionCombination.get(i));
					}
					logger.info("---");
					if (submitBestScore && !firstGameSubmitted && bestScore > initialHighScore) {
						firstGameSubmitted = true;
						submitGameSafely(bestGame);
					}
				}
			}

			long newProgress = (currentIndex * 100) / wordIndexCombinationCount;
			if (newProgress != progress || progress == -1) {
				logger.info("Progress: {} %", newProgress);
				progress = newProgress;
			}
		}
	}

	private long encodeWordIndexes(List<Integer> wordSelection) {
		String indexPart = wordSelection.stream()
										.map(Integer::toHexString)
										.collect(Collectors.joining());
		String lenDigit = Integer.toHexString(wordSelection.size() - 1);
		String full = indexPart + lenDigit;

		return Long.parseLong(full, 12);
	}

	private void submitGameSafely(Game game) {
		try {
			logger.info("Submitting game with score {}", game.getScore());
			submitGame(game);
			logger.info("Game submitted successfully\n\n");
		} catch (IOException | InterruptedException e) {
			logger.error("Unable to submit game", e);
		}
	}

	private void submitGame(Game game) throws IOException, InterruptedException {
		final List<Long> words = game.getWordSelections()
									 .stream()
									 .map(this::encodeWordIndexes)
									 .toList();
		final long score = game.getScore();

		final Payload payload = new Payload(score, words);

		final String csrfToken = pageParser.parseCsrfToken();

		ObjectMapper objectMapper = new ObjectMapper();
		String requestBody = objectMapper.writeValueAsString(payload);

		HttpRequest request = HttpRequest.newBuilder(URI.create("https://slovosled.dennikn.sk/api/validate-score"))
										 .header("Content-Type", "application/json")
										 .header("X-CSRF-TOKEN", csrfToken)
										 .POST(HttpRequest.BodyPublishers.ofString(requestBody))
										 .build();

		final HttpResponse<String> httpResponse = HttpClient.newHttpClient()
															.send(request, HttpResponse.BodyHandlers.ofString());

		logger.info("Response ({}): {}", httpResponse.statusCode(), httpResponse.body());
	}

	private class Payload {

		private long score;
		private List<Long> words;

		public Payload(long score, List<Long> words) {
			this.score = score;
			this.words = words;
		}

		public long getScore() {
			return score;
		}

		public List<Long> getWords() {
			return words;
		}
	}

	private List<List<List<Integer>>> generateWordSelectionCombinations(int index,
																		List<String> words,
																		List<Short> wordIndexCombination,
																		List<List<Integer>> prefix,
																		Map<String, List<List<Integer>>> allPossibleWordsSelections) {
		if (index == wordIndexCombination.size()) {
			return List.of(prefix);
		}

		List<List<List<Integer>>> result = new ArrayList<>();

		final String word = words.get(wordIndexCombination.get(index));
		for (List<Integer> wordSelection : allPossibleWordsSelections.get(word)) {
			List<List<Integer>> newPrefix = new ArrayList<>(prefix);
			newPrefix.add(wordSelection);
			result.addAll(generateWordSelectionCombinations(index + 1, words, wordIndexCombination, newPrefix, allPossibleWordsSelections));
		}
		return result;
	}
}
