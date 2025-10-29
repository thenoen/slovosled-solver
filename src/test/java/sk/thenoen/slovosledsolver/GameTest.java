package sk.thenoen.slovosledsolver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import sk.thenoen.slovosledsolver.model.Tile;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

	static PageDownloader pageDownloader = Mockito.mock(PageDownloader.class);
	static PageParser pageParser;

	@BeforeAll
	static void setUp() {
		pageParser = new PageParser(pageDownloader, new ObjectMapper());
		String pageContent = TestUtils.loadPageContent();
		Mockito.when(pageDownloader.retrievePageContent()).thenReturn(pageContent);
	}

	@Test
	void simpleGame() {

		final List<Tile> tiles = pageParser.retrieveLetters();

		final Game game = new Game(tiles);

		long score = game.play(List.of("ŠUPA"));
		Assertions.assertEquals(16, score);

		score = game.play(List.of("ŠUPA"));
		Assertions.assertEquals(16, score);

	}

	@Test
	void towConsecutiveGames() {

		final List<Tile> tiles = pageParser.retrieveLetters();

		final Game game = new Game(tiles);

		long score = game.play(List.of("ŠUPA"));
		Assertions.assertEquals(16, score);

		score = game.play(List.of("ŠUPA"));
		Assertions.assertEquals(16, score);

	}

	@Test
	void multipleWords() {

		final List<Tile> tiles = pageParser.retrieveLetters();

		final Game game = new Game(tiles);

		long score = game.play(List.of("ŠUPA", "LUPA", "LUPY"));
		Assertions.assertEquals(80, score);

	}
}