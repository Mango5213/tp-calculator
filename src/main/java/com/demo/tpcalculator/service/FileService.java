package com.demo.tpcalculator.service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.demo.tpcalculator.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yue.su
 * @date 2019年2月23日
 */
@Service
@Slf4j
public class FileService {

	@Autowired
	private ConfigProperties configProperties;

	/**
	 * Get all files from a directory, only regular file will be returned.
	 * 
	 * @param directory
	 * @return
	 */
	public List<Path> getAllFiles(String directory) {
		if (directory == null || directory.isEmpty()) {
			log.error("invalid directory [{}], please check", directory);
			throw new RuntimeException();
		}
		List<Path> ret = new ArrayList<>();
		try (Stream<Path> paths = Files.walk(Paths.get(directory)).filter(Files::isRegularFile)) {
			ret = paths.collect(Collectors.toList());
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return ret;
	}

	public long countLogs(String directory) {
		if (directory == null || directory.isEmpty()) {
			log.error("invalid directory [{}], please check", directory);
			throw new RuntimeException();
		}
		long count = 0L;
		try (Stream<Path> paths = Files.walk(Paths.get(directory)).filter(Files::isRegularFile)) {
			count = paths.mapToLong(fileName -> {
				try (Stream<String> stream = Files.lines(fileName,
						Charset.forName(configProperties.getDefaultCharset()))) {
					return stream.filter(
							data -> data != null && !data.isEmpty() && !data.trim().isEmpty())
							.count();
				} catch (AccessDeniedException e1) {
					log.warn("access denied exception");
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
				return 0L;
			}).sum();
		} catch (Exception e2) {
			log.error(e2.getMessage(), e2);
		}
		return count;

	}

	/**
	 * Receive file path as parameter, resolve log and return response time as a stream. DO CLOSE
	 * the stream yourself when finished.
	 * 
	 * @param path
	 * @param withSampling enable sampling or not
	 * @param sampleFrequency sampling every N lines
	 * @return a stream of response time in ms
	 * @throws IOException
	 */
	public Stream<Long> resolveResponseTime(Path path, boolean withSampling, int sampleFrequency)
			throws IOException {
		try {
			Stream<String> stream = readLines(path, withSampling, sampleFrequency);
			// blank line & invalid data will be discarded
			return stream.filter(data -> data != null && !data.isEmpty() && !data.trim().isEmpty())
					.map(data -> data.trim().replaceAll("^.*?(\\w+)\\W*$", "$1")).peek(data -> {
						if (!data.matches("[0-9]+")) {
							log.warn("invalid response time [{}] in [{}]", data, path);
						}
					}).filter(data -> data.matches("[0-9]+")).map(Long::valueOf);
		} catch (IOException e) {
			log.error(e.getMessage());
			return Stream.empty();
		}
	}

	private Stream<String> readLines(Path path, boolean withSampling, int sampleFrequency)
			throws IOException {
		@SuppressWarnings("resource")
		Stream<String> stream =
				Files.lines(path, Charset.forName(configProperties.getDefaultCharset()));
		if (withSampling) {
			if (sampleFrequency <= 1) {
				log.warn("invalid sampleFrequency [{}], ignore sampling", sampleFrequency);
			} else {
				return stream.filter(
						data -> ThreadLocalRandom.current().nextInt(0, sampleFrequency) < 1);
			}
		}
		return stream;
	}

}
