package sk.thenoen.slovosledsolver;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
public class SlovosledSolverApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(SlovosledSolverApplication.class);

	@Resource
	private Downloader downloader;

	@Resource
	private WordsFinder wordsFinder;

	public static void main(String[] args) {
		SpringApplication.run(SlovosledSolverApplication.class, args);
	}

	@Override
	public void run(String... args) {
		logger.info("SlovosledSolverApplication started");

		final String[] hashes = downloader.retrieveHashes();

		final HexFormat hexFormat = HexFormat.of();
//		final byte[] bytes = hexFormat.parseHex(hashes[0]);
//		final String hex = hexFormat.formatHex(bytes);
//		Arrays.stream(hashes)
//				.map(hexFormat::parseHex)
//				.collect(Collectors.toSet());

		final List<String> grid = downloader.retrieveGrid();

		wordsFinder.findWords(grid, List.of(hashes));

		logger.info("SlovosledSolverApplication finished");
	}
}
