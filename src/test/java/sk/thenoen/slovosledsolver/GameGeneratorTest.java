package sk.thenoen.slovosledsolver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import sk.thenoen.slovosledsolver.model.Tile;

class GameGeneratorTest {

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

		final GameGenerator gameGenerator = new GameGenerator(tiles);

		long score = gameGenerator.play(List.of("ŠUPA"));
		Assertions.assertEquals(16, score);

		score = gameGenerator.play(List.of("ŠUPA"));
		Assertions.assertEquals(16, score);

	}

	@Test
	void twoConsecutiveGames() {

		final List<Tile> tiles = pageParser.retrieveLetters();

		final GameGenerator gameGenerator = new GameGenerator(tiles);

		long score = gameGenerator.play(List.of("ŠUPA"));
		Assertions.assertEquals(16, score);

		score = gameGenerator.play(List.of("ŠUPA"));
		Assertions.assertEquals(16, score);

	}

	@Test
	void multipleWords() {

		final List<Tile> tiles = pageParser.retrieveLetters();

		final GameGenerator gameGenerator = new GameGenerator(tiles);

		long score = gameGenerator.play(List.of("ŠUPA", "LUPA", "LUPY"));
		Assertions.assertEquals(80, score);

	}

	@Test
	void wordSelections() {
		final List<Tile> tiles = pageParser.retrieveLetters();

		final GameGenerator gameGenerator = new GameGenerator(tiles);

		final List<List<Integer>> wordSelections = gameGenerator.findAllPossibleWordSelections("ŠUPA");
		Assertions.assertEquals(2, wordSelections.size());
		Assertions.assertEquals(List.of(7, 4, 6, 9), wordSelections.get(0));
		Assertions.assertEquals(List.of(7, 8, 6, 9), wordSelections.get(1));
	}

	@Test
	void play2() {
		final List<Tile> tiles = pageParser.retrieveLetters();
		tiles.get(0).setLetter("L");

		final GameGenerator gameGenerator = new GameGenerator(tiles);

		Map<String, List<List<Integer>>> result = gameGenerator.findAllPossibleWordsSelections(List.of("ŠUPA", "LUPA", "LUPY", "LALA"));
		Assertions.assertNotNull(result);

		Assertions.assertEquals(2, result.get("ŠUPA").size());
		Assertions.assertEquals(List.of(7, 4, 6, 9), result.get("ŠUPA").get(0));
		Assertions.assertEquals(List.of(7, 8, 6, 9), result.get("ŠUPA").get(1));

		Assertions.assertEquals(4, result.get("LUPA").size());
		Assertions.assertEquals(List.of(0, 4, 6, 9), result.get("LUPA").get(0));
		Assertions.assertEquals(List.of(0, 8, 6, 9), result.get("LUPA").get(1));
		Assertions.assertEquals(List.of(3, 4, 6, 9), result.get("LUPA").get(2));
		Assertions.assertEquals(List.of(3, 8, 6, 9), result.get("LUPA").get(3));

		Assertions.assertEquals(4, result.get("LUPY").size());
		Assertions.assertEquals(List.of(0, 4, 6, 5), result.get("LUPY").get(0));
		Assertions.assertEquals(List.of(0, 8, 6, 5), result.get("LUPY").get(1));
		Assertions.assertEquals(List.of(3, 4, 6, 5), result.get("LUPY").get(2));
		Assertions.assertEquals(List.of(3, 8, 6, 5), result.get("LUPY").get(3));

		final List<List<Integer>> lalaSelections = result.get("LALA");
		Assertions.assertEquals(0, lalaSelections.size());

	}

	@Test
	void play2RepeatedLettersInWord() {
		final String testedWord = "LALU";
		final List<Tile> tiles = pageParser.retrieveLetters();
		tiles.get(0).setLetter("L");

		final GameGenerator gameGenerator = new GameGenerator(tiles);

		Map<String, List<List<Integer>>> result = gameGenerator.findAllPossibleWordsSelections(List.of(testedWord));

		final List<List<Integer>> lalaSelections = result.get(testedWord);
		Assertions.assertEquals(4, lalaSelections.size());
		lalaSelections.forEach(lalaSelection -> {
			Assertions.assertEquals(4, lalaSelection.size());
		});

	}

	@Test
	void generatingWordCombinations() {
		final List<Tile> tiles = pageParser.retrieveLetters();
		tiles.get(0).setLetter("L");

		final GameGenerator gameGenerator = new GameGenerator(tiles);

		final List<List<String>> result = gameGenerator.generateAllPossibleWordCombinations(List.of("ŠUPA", "LUPA", "LUPY", "LALU", "PELU", "PAŠU"));
		Assertions.assertEquals(720, result.size());
	}

	@Test
	void generateAllPossibleWordSelectionCombinations() {
		final List<Tile> tiles = pageParser.retrieveLetters();
		tiles.get(0).setLetter("L");

		final GameGenerator gameGenerator = new GameGenerator(tiles);

		final Map<List<String>, List<List<List<Integer>>>> result = gameGenerator.generateAllPossibleWordSelectionCombinations(
				List.of("ŠUPA", "LUPA", "LUPY", "LALU", "PELU", "PAŠU"));
		Assertions.assertEquals(720, result.size());
	}
}