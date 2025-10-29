package sk.thenoen.slovosledsolver;

import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.stream.Collectors;

public class TestUtils {

	public static String loadPageContent() {
		final ClassPathResource pageResource = new ClassPathResource("/slovosled-page-cache.html");
		try (BufferedReader reader = new BufferedReader(new FileReader(pageResource.getFile()))) {
			return reader.lines()
						 .collect(Collectors.joining());

		} catch (Exception ex) {
			throw new RuntimeException("Unable to read page from cache", ex);
		}
	}

}
