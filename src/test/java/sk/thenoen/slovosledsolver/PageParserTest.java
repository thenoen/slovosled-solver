package sk.thenoen.slovosledsolver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import sk.thenoen.slovosledsolver.model.Tile;

class PageParserTest {

	static PageDownloader pageDownloader = Mockito.mock(PageDownloader.class);
	static PageParser pageParser;

	@BeforeAll
	static void setUp() {
		pageParser = new PageParser(pageDownloader, new ObjectMapper());
		String pageContent = TestUtils.loadPageContent();
		Mockito.when(pageDownloader.retrievePageContent()).thenReturn(pageContent);
	}

	@Test
	void retrieveHashes() {
		final String[] hashes = pageParser.retrieveHashes();

		Assertions.assertEquals(456, hashes.length);
		Assertions.assertEquals("df0ccd8a95222a27ee44783ceea02ddb1d2a660b1983e57bf487fff179a5542e", hashes[0]);
		Assertions.assertEquals("61ca11a849ea9992a5273da64fe955fd4bcd42b59c0ce5cabea5d88189ab6cb5", hashes[hashes.length - 1]);
	}

	@Test
	void retrieveGrid() {
		final List<String> grid = pageParser.retrieveGrid();

		Assertions.assertEquals(12, grid.size());

		Assertions.assertEquals("V", grid.get(0));
		Assertions.assertEquals("F", grid.get(1));
		Assertions.assertEquals("E", grid.get(2));
		Assertions.assertEquals("L", grid.get(3));
		Assertions.assertEquals("U", grid.get(4));
		Assertions.assertEquals("Y", grid.get(5));
		Assertions.assertEquals("P", grid.get(6));
		Assertions.assertEquals("Š", grid.get(7));
		Assertions.assertEquals("U", grid.get(8));
		Assertions.assertEquals("A", grid.get(9));
		Assertions.assertEquals("H", grid.get(10));
		Assertions.assertEquals("I", grid.get(11));
	}

	@Test
	void retrieveLetters() {
		final List<Tile> tiles = pageParser.retrieveLetters();

		Assertions.assertEquals(12, tiles.size());

		assertLetter("V", tiles.get(0));
		assertLetter("F", tiles.get(1));
		assertLetter("I", tiles.get(11));
	}

	private static void assertLetter(String V, Tile tile) {
		Assertions.assertEquals(V, tile.getLetter());
		Assertions.assertEquals(1, tile.getValue());
		Assertions.assertEquals(0, tile.getUsageCount());
		Assertions.assertFalse(tile.isSelected());
	}
}