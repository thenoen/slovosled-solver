package sk.thenoen.slovosledsolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import sk.thenoen.slovosledsolver.model.Tile;

public class Game {

	private static final Logger logger = LoggerFactory.getLogger(Game.class);

	private List<Tile> originalTiles;

	public Game(List<Tile> originalTiles) {
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

	public Map<String, List<List<Integer>>> findAllPossibleWordsSelections(Collection<String> words) {
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
			if(uniqueCharacterIndices.size() == wordSelection.size()) {
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
