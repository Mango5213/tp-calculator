package com.demo.tpcalculator.service;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.demo.tpcalculator.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Default percentile calculation. Use an array to store the response time, the index of array is
 * exactly the response time or the response time within the bucket. Use multi-thread to concurrent
 * read files. Use sampling to reduce calculation time.
 * 
 * @author yue.su
 * @date 2019年2月24日
 */
@Service
@Slf4j
public class DefaultPercentileCalculationService
		extends AbstractPercentileCalculationService<List<Long>> {

	@Autowired
	private ConfigProperties configProperties;
	@Autowired
	private FileService fileService;

	@Override
	protected void validateParameters(List<String> percentiles) {
		int bucketSize = configProperties.getBucketSize();
		if (bucketSize <= 0) {
			log.error("bucket size [{}] is not valid", bucketSize);
			throw new RuntimeException("bucket size is not valid");
		}
		super.validateParameters(percentiles);
	}

	@Override
	protected List<Long> getResult(String directory) {
		log.info("start getting result, directory: [{}]", directory);
		int bucketSize = configProperties.getBucketSize();
		long count = fileService.countLogs(configProperties.getDirectory());
		if (count == 0L) {
			log.error("no valid logs, please check");
			throw new RuntimeException("no valid logs, please check");
		}
		boolean enableSampling = false;
		int sampleFrequency = 1;
		// if log count is greater than 2 times of sampling threshold
		if (count / configProperties.getSamplingThreshold() > 1L) {
			enableSampling = true;
			sampleFrequency = (int) (count / configProperties.getSamplingThreshold());
			log.info("sampling enabled, frequency is : [{}]", sampleFrequency);
		}
		List<Long> result = new ArrayList<>();
		// concurrent read files
		ExecutorService pool =
				Executors.newFixedThreadPool(configProperties.getDefaultConcurrency());
		try {
			CompletionService<List<Long>> completionService = new ExecutorCompletionService<>(pool);
			List<Path> paths = fileService.getAllFiles(directory);
			if (paths == null || paths.isEmpty()) {
				log.error("no files in directory: [{}], please check");
				throw new RuntimeException("no files in directory");
			}
			int taskNum = 0;
			for (Path path : paths) {
				completionService.submit(new SingleFileCalculator(path, bucketSize, enableSampling,
						sampleFrequency));
				taskNum++;
			}
			for (int i = 0; i < taskNum; i++) {
				List<Long> ret = completionService.take().get();
				if (i == 0) {
					result = ret;
				} else {
					result = mergeList(result, ret);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage());
		} finally {
			pool.shutdown();
		}
		log.info("end getting result, directory: [{}]", directory);
		return result;
	}

	private List<Long> mergeList(List<Long> list1, List<Long> list2) {
		List<Long> small;
		List<Long> big;
		// do the protection copy
		if (list1.size() >= list2.size()) {
			small = new ArrayList<>(list2);
			big = new ArrayList<>(list1);
		} else {
			small = new ArrayList<>(list1);
			big = new ArrayList<>(list2);
		}
		Long bigValue;
		for (int i = 0; i < small.size(); i++) {
			bigValue = big.get(i) + small.get(i);
			big.set(i, bigValue);
		}
		return big;
	}

	@Override
	protected int calculatePercentile(BigDecimal percentile, List<Long> result) {
		log.info("start calculating percentile [{}]", percentile);
		long total = result.stream().mapToLong(a -> a).sum();
		log.info("total records: [{}]", total);
		int bucketSize = configProperties.getBucketSize();
		int rspTime = 0;
		if (percentile.compareTo(BigDecimal.ONE) == 0) {
			// special case: top percentile 100%
			rspTime = result.size() * bucketSize - 1;
		} else {

			if (bucketSize == 1) {
				log.info("buckestSize = 1, use accuracy mode");
				rspTime = accurateCalculatePercentile(result, total, percentile);
			} else {
				log.info("bucketSize > 1, use bucket for approximation");
				rspTime = approximteCalculatePercentile(bucketSize, result, total, percentile);
			}
		}
		log.info("percentile: [{}], response time calculated: [{}]", percentile, rspTime);
		log.info("end calculating percentile [{}]", percentile);
		return rspTime;
	}

	private int approximteCalculatePercentile(int bucketSize, List<Long> result, long total,
			BigDecimal percentile) {
		long percentileCount = new BigDecimal(total).multiply(percentile).longValue();
		long lower = total;
		int rspTime = 0;
		for (int i = result.size() - 1; i >= 0; i--) {
			lower -= result.get(i);
			if (lower == percentileCount) {
				for (int j = i - 1; j >= 0; j--) {
					if (result.get(j) != 0L) {
						rspTime = (j + 1) * bucketSize - 1;
						break;
					}
				}
				break;
			} else if (lower < percentileCount) {
				// upper = lower+result.get(i)
				// lower < percentileCount < upper
				// assume response time is uniformly distributed within bucket
				rspTime = (int) (i * bucketSize
						+ (percentileCount - lower) * bucketSize / result.get(i)) - 1;
				break;
			}
		}
		return rspTime;
	}

	private int accurateCalculatePercentile(List<Long> result, long total, BigDecimal percentile) {
		long percentileCount = new BigDecimal(total).multiply(percentile).longValue();
		long lower = total;
		int rspTime = 0;
		for (int i = result.size() - 1; i >= 0; i--) {
			lower -= result.get(i);
			if (lower <= percentileCount) {
				// find the largest non-zero count
				for (int j = i - 1; j >= 0; j--) {
					if (result.get(j) != 0L) {
						rspTime = j;
						break;
					}
				}
				break;
			}
		}
		return rspTime;
	}

	private class SingleFileCalculator implements Callable<List<Long>> {

		private Path path;
		private int bucketSize;
		private boolean enableSampling;
		private int sampleFrequency;

		public SingleFileCalculator(Path path, int bucketSize, boolean enableSampling,
				int sampleFrequency) {
			this.path = path;
			this.bucketSize = bucketSize;
			this.enableSampling = enableSampling;
			this.sampleFrequency = sampleFrequency;
		};

		@Override
		public List<Long> call() throws Exception {
			final List<Long> result = new ArrayList<>();
			Stream<Long> stream = Stream.empty();
			try {
				log.info("start processing [{}]", path);
				stream = fileService.resolveResponseTime(path, enableSampling, sampleFrequency);
				stream.forEach(latency -> {
					int bucketPosition = (int) (latency / bucketSize);
					if (result.size() < bucketPosition + 1) {
						// enlarge list
						((ArrayList<Long>) result).ensureCapacity(bucketPosition + 1);
						for (int i = result.size() - 1; i < bucketPosition; i++) {
							// init value is 0
							result.add(0L);
						}
					}

					Long count = result.get(bucketPosition);
					result.set(bucketPosition, ++count);
				});
			} finally {
				stream.close();
			}
			log.info("end processing [{}]", path);
			return result;
		}

	}

}
