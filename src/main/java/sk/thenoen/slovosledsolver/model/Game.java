package sk.thenoen.slovosledsolver.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

public class Game {

	private static final Logger logger = LoggerFactory.getLogger(Game.class);

	private List<Tile> tiles;
	private List<String> words;
	private final Bonus bonus;
	private boolean bonusActive;
	private List<List<Integer>> wordSelections;
	private long score = 0;

	public Game(List<Tile> tiles,
				Bonus bonus,
				List<String> words,
				List<List<Integer>> wordSelections) {
		this.tiles = tiles.stream().map(Tile::copy).toList();
		this.words = words;
		this.bonus = bonus;
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

			if (!bonusActive) {
				bonusActive = isBonusActive();
			}

			for (int j = 0; j < wordSelection.size(); j++) {
				final int index = wordSelection.get(j);
				final Tile tile = tiles.get(index);
				wordSum += tile.getValue();
				tile.incrementUsageCount();
			}

			logger.debug("\ttiles after word {}: {}", word, tiles);

			long bonusScore = 0;
			if (bonusActive) {
				bonusScore = calculateBonusScore(word);
			}

			final long wordScore = wordSum * word.length() + bonusScore;
			logger.debug("{} - score: {} (selection: {})", word, wordScore, wordSelection);
			score += wordScore;

		}

		logger.debug("Game finished: {}", score);
		return score;
	}

	private boolean isBonusActive() {
		final int[] pattern = bonus.getPatternAsArray();

		int cols = 4;
		for (int i = 0; i < this.tiles.size() - cols; i++) {
			if (i % cols > cols - 2) {
				continue;
			}
			Tile[][] block = new Tile[2][];
			{
				block[0] = new Tile[]{this.tiles.get(i), this.tiles.get(i + 1)};
				block[1] = new Tile[]{this.tiles.get(i + cols), this.tiles.get(i + cols + 1)};
			}
			;
			boolean match = true;
			for (int r = 0; r < 2; r++) {
				for (int c = 0; c < 2; c++) {
					int expected = pattern[r * 2 + c];
					if (expected == 0) {
						continue;
					}
					if (block[r][c].getValue() != expected || block[r][c].isReachedMax()) {
						match = false;
						break;
					}
				}
				if (!match) {
					break;
				}
			}
			if (match) {
				return true;
			}
		}
		return false;
	}

	private long calculateBonusScore(String selectedWord) {

		switch (bonus.getIndex()) {
			case 0:
				return selectedWord.length() == 6 ? bonus.getValue() : 0;
			case 1:
				return selectedWord.length() == 7 ? bonus.getValue() : 0;
			case 2:
				final long count = selectedWord.chars()
											   .filter(c -> "AEIOUYÁÉÍÓÚÝÄ".contains(Character.toString((char) c)))
											   .count();
				return count * bonus.getValue();
			case 3:
				final long count1 = selectedWord.chars()
												.filter(c -> !"AEIOUYÁÉÍÓÚÝÄ".contains(Character.toString((char) c)))
												.count();
				return count1 * bonus.getValue();
			case 4:
				return !"AEIOUYÁÉÍÓÚÝÄ".contains(Character.toString(selectedWord.charAt(0))) ? bonus.getValue() : 0;
			case 5:
				return !"AEIOUYÁÉÍÓÚÝÄ".contains(Character.toString(selectedWord.charAt(selectedWord.length() - 1))) ? bonus.getValue() : 0;
			case 6:
				return "AEIOUYÁÉÍÓÚÝÄ".contains(Character.toString(selectedWord.charAt(0))) ? bonus.getValue() : 0;
			case 7:
				return "AEIOUYÁÉÍÓÚÝÄ".contains(Character.toString(selectedWord.charAt(selectedWord.length() - 1))) ? bonus.getValue() : 0;

			case 8:
				final String expectedLetter = tiles.get(LocalDate.now().getMonthValue() - 1).getLetter();
				return selectedWord.contains(expectedLetter) ? bonus.getValue() : 0;
			default:
				throw new IllegalStateException("Unexpected value: " + bonus.getIndex());
		}
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
