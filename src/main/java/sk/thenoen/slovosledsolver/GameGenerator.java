package sk.thenoen.slovosledsolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import sk.thenoen.slovosledsolver.model.Game;
import sk.thenoen.slovosledsolver.model.Tile;

@Component
public class GameGenerator {

	private static final Logger logger = LoggerFactory.getLogger(GameGenerator.class);

	private final DataStorage dataStorage;

	public GameGenerator(DataStorage dataStorage) {
		this.dataStorage = dataStorage;
	}

	public List<List<Short>> generateAllPossibleWordCombinations(List<String> words) {

		List<Short> wordIndices = new ArrayList<>();
		for (int i = 0; i < words.size(); i++) {
			wordIndices.add((short) i);
		}
		List<List<Short>> result = findAllPossibleWordCombinationsUsingIndices(new ArrayList<>(), 0, wordIndices);

		logger.info("Found {} possible word combinations", result.size());
		return result;
	}

	public Map<List<String>, List<List<List<Integer>>>> generateAllPossibleWordSelectionCombinations(List<Tile> tiles, List<String> words) {

		logger.info("Generating all possible word combinations ...");
		List<Short> wordIndices = new ArrayList<>();
		for (int i = 0; i < words.size(); i++) {
			wordIndices.add((short) i);
		}
		List<List<Short>> wordIndexCombinations = findAllPossibleWordCombinationsUsingIndices(new ArrayList<>(), 0, wordIndices);
		List<List<String>> wordCombinations = findAllPossibleWordCombinations(new ArrayList<>(), 0, words);

		logger.info("Found {} possible word combinations", wordCombinations.size());
		logger.info("Found {} possible word combinations using indices", wordIndexCombinations.size());

		logger.info("Generating all possible word selections ...");
		final Map<String, List<List<Integer>>> allPossibleWordsSelections = findAllPossibleWordsSelections(tiles, words);
		logger.info("Generated all possible word selections finished", allPossibleWordsSelections.size());

		logger.info("Generating all possible word selection combinations ...");
		final var wordSelectionCombinations = generateWordSelectionCombinations(wordCombinations, allPossibleWordsSelections); // todo: optimize memory
		logger.info("Found {} possible word selection combinations", wordSelectionCombinations.size());

		return wordSelectionCombinations;
	}

	public List<Game> generateAllPossibleGames(List<Tile> tiles, List<String> words) {

		logger.info("Generating all possible games ...");

		List<Game> games = new ArrayList<>();

		final Map<List<String>, List<List<List<Integer>>>> wordSelectionCombinations = generateAllPossibleWordSelectionCombinations(tiles, words);

		logger.info("Found {} possible word combinations", wordSelectionCombinations.size());

		wordSelectionCombinations.forEach((wordCombination, wordSelectionCombinationsForWordCombination) -> {
			wordSelectionCombinationsForWordCombination.forEach(wordSelectionCombination -> {
					games.add(new Game(tiles, wordCombination, wordSelectionCombination));
			});
		});

		logger.info("Generated {} possible games", games.size());
		return games;
	}

	public Map<List<String>, List<List<List<Integer>>>> generateWordSelectionCombinations(List<List<String>> wordCombinations,
																						  Map<String, List<List<Integer>>> allPossibleWordsSelections) {

		Map<List<String>, List<List<List<Integer>>>> wordSelectionCombinations = new HashMap<>();

		for (List<String> wordCombination : wordCombinations) {
			wordSelectionCombinations.put(wordCombination,
										  generateWordSelectionCombinations(new ArrayList<>(new ArrayList<>()), 0, wordCombination,
																			allPossibleWordsSelections));
		}

		return wordSelectionCombinations;
	}

	private List<List<List<Integer>>> generateWordSelectionCombinations(List<List<Integer>> prefix,
																		int index,
																		List<String> wordCombination,
																		Map<String, List<List<Integer>>> allPossibleWordsSelections) {
		if (index == wordCombination.size()) {
			return List.of(prefix);
		}

		List<List<List<Integer>>> result = new ArrayList<>();

		final String word = wordCombination.get(index);
		for (List<Integer> wordSelection : allPossibleWordsSelections.get(word)) {
			List<List<Integer>> newPrefix = new ArrayList<>(prefix);
			newPrefix.add(wordSelection);
			result.addAll(generateWordSelectionCombinations(newPrefix, index + 1, wordCombination, allPossibleWordsSelections));
		}
		return result;
	}

	private List<List<String>> findAllPossibleWordCombinations(List<String> prefix, int index, List<String> words) {
		if (index == 5) {
			logger.debug("Found possible word combination: {}", prefix);
			dataStorage.storeWordCombination(prefix);
//			return List.of(prefix);
			return Collections.emptyList();
		}

		List<List<String>> result = new ArrayList<>();
		for (String word : words) {
			if (!prefix.contains(word)) {
				List<String> newPrefix = new ArrayList<>(prefix);
				newPrefix.add(word);
				List<String> newWords = new ArrayList<>(words);
				newWords.remove(word);
				result.addAll(findAllPossibleWordCombinations(newPrefix, index + 1, newWords));
			}
		}
		return result;
	}

	private List<List<Short>> findAllPossibleWordCombinationsUsingIndices(List<Short> prefix, int index, List<Short> wordIndices) {
		if (index == 5) {
			logger.debug("Found possible word combination: {}", prefix);
			dataStorage.storeWordIndexCombination(prefix);
			//			return List.of(prefix);
			return Collections.emptyList();
		}

		List<List<Short>> result = new ArrayList<>();
		for (Short wordIndex : wordIndices) {
			if (!prefix.contains(wordIndex)) {
				List<Short> newPrefix = new ArrayList<>(prefix);
				newPrefix.add(wordIndex);
				List<Short> newWords = new ArrayList<>(wordIndices);
				newWords.remove(wordIndex);
				result.addAll(findAllPossibleWordCombinationsUsingIndices(newPrefix, index + 1, newWords));
			}
		}
		return result;
	}

	public Map<String, List<List<Integer>>> findAllPossibleWordsSelections(List<Tile> tiles, List<String> words) {
		Map<String, List<List<Integer>>> wordSelections = new HashMap<>();
		for (String word : words) {
			wordSelections.put(word, findAllPossibleWordSelections(tiles, word));
		}

		return wordSelections;
	}

	public List<List<Integer>> findAllPossibleWordSelections(List<Tile> tiles, String word) {

		logger.debug("Letters: {}", tiles.stream()
										 .map(Tile::getLetter)
										 .toList());

		List<List<Integer>> possibleCharacterSelections = new ArrayList<>();

		for (int i = 0; i < word.length(); i++) {

			final String character = word.charAt(i) + "";
			final List<Tile> tilesWithChar = tiles.stream()
												  .filter(t -> t.getLetter().equals(character))
												  .toList();
			logger.debug("Found {} tiles with letter {}", tilesWithChar.size(), character);

			possibleCharacterSelections.add(tilesWithChar.stream()
														 .map(tiles::indexOf)
														 .toList());
		}

		final List<List<Integer>> characterSelections = possibleCharacterSelections;
		final List<List<Integer>> wordSelections = findAllPossibleWordSelections(Collections.emptyList(), 0, characterSelections);

		for (List<Integer> integers : wordSelections) {
			logger.debug("Found possible word selection: {}", integers);
		}

		List<List<Integer>> uniqueWordSelections = new ArrayList<>();
		for (List<Integer> wordSelection : wordSelections) {
			final HashSet<Integer> uniqueCharacterIndices = new HashSet<>(wordSelection);
			if (uniqueCharacterIndices.size() == wordSelection.size()) {
				logger.debug("Found unique word selection: {}", wordSelection);
				uniqueWordSelections.add(wordSelection);
			}
		}

		return uniqueWordSelections;

	}

	private static List<List<Integer>> findAllPossibleWordSelections(List<Integer> prefix, int index, List<List<Integer>> characterSelections) {
		if (index == characterSelections.size()) {
			return List.of(prefix);
		}

		List<List<Integer>> result = new ArrayList<>();

		final List<Integer> characterIndexes = characterSelections.get(index);
		for (int i = 0; i < characterIndexes.size(); i++) {
			final ArrayList<Integer> newPrefix = new ArrayList<>(prefix);
			newPrefix.add(characterIndexes.get(i));
			final List<List<Integer>> wordSelections = findAllPossibleWordSelections(newPrefix, index + 1, characterSelections);
			result.addAll(wordSelections);
		}

		return result;
	}

}
