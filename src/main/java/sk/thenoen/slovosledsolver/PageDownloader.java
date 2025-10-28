package sk.thenoen.slovosledsolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PageDownloader {

	private static final Logger logger = LoggerFactory.getLogger(PageDownloader.class);

	private static final String PAGE_CACHE_LOCATION = "/tmp/slovosled-page-cache.txt";

	public String retrievePageContent() {
		var pageCacheFile = new File(PAGE_CACHE_LOCATION);

		String pageContent = null;

		if (pageCacheFile.exists()) {
			pageContent = readPageFromCache(pageCacheFile);
		} else {
			pageContent = downloadPage();
			cachePageContent(pageCacheFile, pageContent);
		}
		return pageContent;
	}

	private static String readPageFromCache(File fileCache) {
		logger.info("Page cache file exists - loading...");
		try (BufferedReader reader = new BufferedReader(new FileReader(fileCache))) {
			return reader.lines()
						 .collect(Collectors.joining());

		} catch (Exception ex) {
			throw new RuntimeException("Unable to read page from cache", ex);
		}
	}

	private static void cachePageContent(File pageCacheFile, String pageContent) {
		try (FileWriter fileWriter = new FileWriter(pageCacheFile, false)) {
			fileWriter.write(pageContent);
		} catch (Exception ex) {
			throw new RuntimeException("Unable to save page to cache", ex);
		}
	}

	private static String downloadPage() {
		logger.info("Downloading page slovosled.dennikn.sk ...");
		String content = null;
		URLConnection connection = null;
		try {
			connection = URL.of(URI.create("https://slovosled.dennikn.sk/"), null)
							.openConnection();

			Scanner scanner = new Scanner(connection.getInputStream());
			scanner.useDelimiter("\\Z");
			content = scanner.next();
			scanner.close();
		} catch (Exception ex) {
			throw new RuntimeException("Unable to download page", ex);
		}
		return content;
	}

}
