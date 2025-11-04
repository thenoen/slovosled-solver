package sk.thenoen.slovosledsolver.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import sk.thenoen.slovosledsolver.DataStorage;
import sk.thenoen.slovosledsolver.GameGenerator;
import sk.thenoen.slovosledsolver.PageDownloader;
import sk.thenoen.slovosledsolver.PageParser;
import sk.thenoen.slovosledsolver.TestUtils;

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
	void playGameAndVerifyScore() {
		final List<String> words = List.of("ŠUPA", "LUPA", "LUPY", "LALU", "PELU", "PAŠU");

		final List<Tile> tiles = pageParser.retrieveLetters();
		tiles.get(0).setLetter("L");

		GameGenerator gameGenerator = new GameGenerator(new DataStorage("/tmp"));
		final Map<List<String>, List<List<List<Integer>>>> wordSelectionCombinations = gameGenerator.generateAllPossibleWordSelectionCombinations(tiles, words);

		final List<String> wordsCombination = wordSelectionCombinations.keySet().stream().findFirst().get();
		final List<List<Integer>> wordSelectionCombination = wordSelectionCombinations.get(wordsCombination).get(0);

		final Game game = new Game(tiles, wordsCombination, wordSelectionCombination);
		long score = game.play();
		Assertions.assertEquals(128, score);

//		score = game.play(List.of("ŠUPA"));
//		Assertions.assertEquals(16, score);

	}
}