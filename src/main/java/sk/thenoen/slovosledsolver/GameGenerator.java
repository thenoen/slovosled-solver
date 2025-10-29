package sk.thenoen.slovosledsolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import sk.thenoen.slovosledsolver.model.Tile;

public class GameGenerator {

	private static final Logger logger = LoggerFactory.getLogger(GameGenerator.class);

	private List<Tile> originalTiles;

	public GameGenerator(List<Tile> originalTiles) {
		this.originalTiles = originalTiles;
	}

	public long play(List<String> words) {
		long score = 0;
		final List<Tile> tiles = originalTiles.stream()
											  .map(Tile::copy)
											  .toList();

		for (String word : words) {

			long wordSum = 0;

			for (int i = 0; i < word.length(); i++) {

				final String character = word.charAt(i) + "";
				final List<Tile> tilesWithChar = tiles.stream()
													  .filter(t -> t.getLetter().equals(character))
													  .toList();
				logger.info("Found {} tiles with letter {}", tilesWithChar.size(), character);

				if (tilesWithChar.size() == 0) {
					logger.error("No tiles with letter {} found! This is not expected", character);
					throw new IllegalStateException("No tiles with letter " + character + " found!");
				}

				if (tilesWithChar.size() > 1) {
					logger.warn("Found {} tiles with letter {}. Multiple combinations should be tested", tilesWithChar.size(), character);
				}

				final Tile tile = tilesWithChar.get(0);
				wordSum += tile.getValue();
				tile.incrementUsageCount();

			}

			final long wordScore = wordSum * word.length();
			logger.info("{} - word score: {}", word, wordScore);
			score += wordScore;

			tiles.forEach(t -> t.setSelected(false));

		}

		return score;
	}

	public List<List<String>> generateAllPossibleWordCombinations(List<String> words) {
		final Map<String, List<List<Integer>>> allPossibleWordsSelections = findAllPossibleWordsSelections(words);

		List<List<String>> result = new ArrayList<>();
		for (String word : words) {
			final List<List<String>> allPossibleWordCombinations = findAllPossibleWordCombinations(new ArrayList<>(List.of(word)), 1, words);
			result.addAll(allPossibleWordCombinations);
		}

		logger.info("Found {} possible word combinations", result.size());
		return result;
	}

	public Map<List<String>, List<List<List<Integer>>>> generateAllPossibleWordSelectionCombinations(List<String> words) {
		List<List<String>> wordCombinations = new ArrayList<>();
		for (String word : words) {
			final List<List<String>> allPossibleWordCombinations = findAllPossibleWordCombinations(new ArrayList<>(List.of(word)), 1, words);
			wordCombinations.addAll(allPossibleWordCombinations);
		}

		final Map<String, List<List<Integer>>> allPossibleWordsSelections = findAllPossibleWordsSelections(words);

		logger.info("Found {} possible word combinations", wordCombinations.size());

		final Map<List<String>, List<List<List<Integer>>>> wordSelectionCombinations = generateWordSelectionCombinations(wordCombinations, allPossibleWordsSelections);
		return wordSelectionCombinations;
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

	private static List<List<String>> findAllPossibleWordCombinations(List<String> prefix, int index, List<String> words) {
		if (index == 5) {
			logger.debug("Found possible word combination: {}", prefix);
			return List.of(prefix);
		}

		List<List<String>> result = new ArrayList<>();
		for (String word : words) {
			if (!prefix.contains(word)) {
				List<String> newPrefix = new ArrayList<>(prefix);
				newPrefix.add(word);
				result.addAll(findAllPossibleWordCombinations(newPrefix, index + 1, words));
			}
		}
		return result;
	}

	public Map<String, List<List<Integer>>> findAllPossibleWordsSelections(List<String> words) {
		Map<String, List<List<Integer>>> wordSelections = new HashMap<>();
		for (String word : words) {
			wordSelections.put(word, findAllPossibleWordSelections(word));
		}

		return wordSelections;
	}

	public List<List<Integer>> findAllPossibleWordSelections(String word) {

		final List<Tile> tiles = originalTiles.stream()
											  .map(Tile::copy)
											  .toList();

		logger.info("Letters: {}", tiles.stream()
										.map(Tile::getLetter)
										.toList());

		List<List<Integer>> possibleCharacterSelections = new ArrayList<>();

		for (int i = 0; i < word.length(); i++) {

			final String character = word.charAt(i) + "";
			final List<Tile> tilesWithChar = tiles.stream()
												  .filter(t -> t.getLetter().equals(character))
												  .toList();
			logger.info("Found {} tiles with letter {}", tilesWithChar.size(), character);

			possibleCharacterSelections.add(tilesWithChar.stream()
														 .map(tiles::indexOf)
														 .toList());
		}

		final List<List<Integer>> characterSelections = possibleCharacterSelections;
		final List<List<Integer>> wordSelections = findAllPossibleWordSelections(Collections.emptyList(), 0, characterSelections);

		for (List<Integer> integers : wordSelections) {
			logger.info("Found possible word selection: {}", integers);
		}

		List<List<Integer>> uniqueWordSelections = new ArrayList<>();
		for (List<Integer> wordSelection : wordSelections) {
			final HashSet<Integer> uniqueCharacterIndices = new HashSet<>(wordSelection);
			if (uniqueCharacterIndices.size() == wordSelection.size()) {
				logger.info("Found unique word selection: {}", wordSelection);
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
