package sk.thenoen.slovosledsolver.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Game {

	private static final Logger logger = LoggerFactory.getLogger(Game.class);

	private List<Tile> tiles;
	private List<String> words;
	private List<List<Integer>> wordSelections;
	private long score = 0;

	public Game(List<Tile> tiles, List<String> words, List<List<Integer>> wordSelections) {
		this.tiles = tiles.stream().map(Tile::copy).toList();
		this.words = words;
		this.wordSelections = wordSelections;

		logger.debug("Game: {}", words);
		for (int i = 0; i < words.size(); i++) {
			if (logger.isDebugEnabled()) {
				logger.debug("\t{}: {}", words.get(i), wordSelections.get(i));
			}
		}
		logger.debug("\ttiles: {}", tiles);
	}

	public long play() {
		logger.debug("-------------------------------");
		logger.debug("Playing game: {}", words);

		for (int i = 0; i < words.size(); i++) {
			final String word = words.get(i);
			final List<Integer> wordSelection = wordSelections.get(i);

			logger.debug("\t{}: {}", word, wordSelection);
			long wordSum = 0;

			for (int j = 0; j < wordSelection.size(); j++) {
				final int index = wordSelection.get(j);
				final Tile tile = tiles.get(index);
				wordSum += tile.getValue();
				tile.incrementUsageCount();
			}

			logger.debug("\ttiles after word {}: {}", word, tiles);

			final long wordScore = wordSum * word.length();
			logger.debug("{} - score: {} (selection: {})", word, wordScore, wordSelection);
			score += wordScore;

		}

		logger.debug("Game finished: {}", score);
		return score;
	}

	public void printScore() {
		logger.info("Game finished: {}", score);
		logger.info("\t words: {}", words);
		for (int i = 0; i < words.size(); i++) {
			logger.info("\t{}: {}", words.get(i), wordSelections.get(i));
		}
	}

	public long getScore() {
		return score;
	}
}
