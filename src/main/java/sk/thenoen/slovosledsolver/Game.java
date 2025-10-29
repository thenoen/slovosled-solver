package sk.thenoen.slovosledsolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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

}
