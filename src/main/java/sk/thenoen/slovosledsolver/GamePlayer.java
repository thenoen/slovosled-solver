package sk.thenoen.slovosledsolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sk.thenoen.slovosledsolver.model.Bonus;
import sk.thenoen.slovosledsolver.model.Game;
import sk.thenoen.slovosledsolver.model.Tile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class GamePlayer {

	private static final Logger logger = LoggerFactory.getLogger(GamePlayer.class);

	@Value("${slovosled.submit-best-score:false}")
	private boolean submitBestScore;

	@Value("${slovosled.anonymize-words:true}")
	private boolean anonymizeWords;

	@Resource
	private PageParser pageParser;

	@Resource
	private DataStorage dataStorage;

	public void playGames(List<String> wordsSelectedForGames,
						  List<Tile> tiles,
						  Bonus bonus,
						  Map<String, List<List<Integer>>> allPossibleWordsSelections) {

		long wordIndexCombinationCount = 1;
		for (int i = 0; i < 5; i++) {
			wordIndexCombinationCount *= wordsSelectedForGames.size() - i;
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
																										  wordsSelectedForGames,
																										  indicesOfSelectedWords,
																										  new ArrayList<>(new ArrayList<>()),
																										  allPossibleWordsSelections);

			currentIndex++;
			for (List<List<Integer>> wordSelectionCombination : wordSelectionCombinations) {
				final List<String> selectedWords = indicesOfSelectedWords.stream()
																		 .map(wordsSelectedForGames::get)
																		 .toList();
				final Game game = new Game(tiles, bonus, selectedWords, wordSelectionCombination);
				final long score = game.play();

				if (score > bestScore) {
					bestScore = score;
					bestGame = game;
					logger.info("Found best score: {} (bonus: {}) for word combination:", bestScore, game.getBonusActive());
					for (int i = 0; i < selectedWords.size(); i++) {
						final String word;
						if (anonymizeWords) {
							word = selectedWords.get(i).replaceAll("[A-Ž]", "*");
						} else {
							word = selectedWords.get(i);
						}
						logger.info("\t{}: {}",
									word,
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

	private static class Payload {

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
			return List.of(new ArrayList<>(prefix));
		}

		List<List<List<Integer>>> result = new ArrayList<>();
		String word = words.get(wordIndexCombination.get(index));
		List<List<Integer>> possibleSelections = allPossibleWordsSelections.get(word);

		for (List<Integer> selection : possibleSelections) {
			prefix.add(selection);
			result.addAll(generateWordSelectionCombinations(index + 1, words, wordIndexCombination, prefix, allPossibleWordsSelections));
			prefix.remove(prefix.size() - 1);
		}

		return result;
	}

}
