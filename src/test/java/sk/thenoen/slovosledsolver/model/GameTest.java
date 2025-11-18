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

	private static PageDownloader pageDownloader = Mockito.mock(PageDownloader.class);
	private static PageParser pageParser;

	private DataStorage dataStorageMock;

	@BeforeAll
	static void setUp() {
		pageParser = new PageParser(pageDownloader, new ObjectMapper());
		String pageContent = TestUtils.loadPageContent();
		Mockito.when(pageDownloader.retrievePageContent()).thenReturn(pageContent);
	}

	private void initDataStorage() {
		dataStorageMock = Mockito.mock(DataStorage.class);
	}

	@Test
	void playGameAndVerifyScore() {
		final List<String> words = List.of("ŠUPA", "LUPA", "LUPY", "LALU", "PELU", "PAŠU");

		final List<Tile> tiles = pageParser.retrieveLetters();
		tiles.get(0).setLetter("L");

		GameGenerator gameGenerator = new GameGenerator(dataStorageMock);
		final Map<String, List<List<Integer>>> wordSelectionCombinations = gameGenerator.generateAllPossibleWordSelectionCombinations(tiles, words);

		final String wordsCombination = wordSelectionCombinations.keySet().stream().findFirst().get();
		final List<List<Integer>> wordSelectionCombination = wordSelectionCombinations.get(wordsCombination);

		Bonus bonus = new Bonus();
		bonus.setPattern("0111");
		bonus.setValue(5);
		bonus.setIndex(2);
		bonus.setText("some text");

		final Game game = new Game(tiles, bonus, words, wordSelectionCombination);
		long score = game.play();
		Assertions.assertEquals(128, score);

		score = game.play();
		Assertions.assertEquals(16, score);

	}
}