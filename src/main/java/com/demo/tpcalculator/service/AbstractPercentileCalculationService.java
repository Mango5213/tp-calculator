package com.demo.tpcalculator.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yue.su
 * @date 2019年2月24日
 */
@Slf4j
public abstract class AbstractPercentileCalculationService<T> implements TopPercentileService {

	protected static final String RESPONSE_TEMPLATE =
			"%s%% of requests return a response within %d ms\r\n";

	protected static final BigDecimal hundred = new BigDecimal("100");

	@Override
	public String getTopPercentile(String directory, List<String> percentiles) {
		validateParameters(percentiles);
		List<BigDecimal> formatPercentiles = percentiles.stream()
				.map(per -> new BigDecimal(per).divide(hundred)).collect(Collectors.toList());
		StringBuilder builder = new StringBuilder();
		try {
			// do a estimation before actually reading data will increase the
			// accuracy,
			// e.g. median, max/min rsp time
			// however this could be very time-consuming so I didn't use it.
			final T result = getResult(directory);
			for (BigDecimal percentile : formatPercentiles) {
				int rspTime = calculatePercentile(percentile, result);
				builder.append(String.format(RESPONSE_TEMPLATE,
						percentile.multiply(hundred).stripTrailingZeros().toPlainString(),
						rspTime));
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return builder.toString();
	}

	/**
	 * Get the result and stored in a specified data structure
	 * 
	 * @param directory
	 * @return result set in generic type
	 */
	protected abstract T getResult(String directory);

	/**
	 * Calculate the top percentile based on the data set.
	 * 
	 * @param bucketSize
	 * @param formatPercentiles
	 * @param result
	 * @return
	 */
	protected abstract int calculatePercentile(BigDecimal formatPercentiles, final T result);


	/**
	 * Validate the input parameters, the rules should be common.
	 * 
	 * @param percentiles
	 */
	protected void validateParameters(List<String> percentiles) {
		if (percentiles == null || percentiles.isEmpty()) {
			log.error("percentiles is null or empty");
			throw new RuntimeException("percentiles is null or empty");
		} else {
			percentiles.stream().forEach(per -> {
				if (new BigDecimal(per).compareTo(BigDecimal.ZERO) <= 0
						|| new BigDecimal(per).compareTo(hundred) > 0) {
					log.error("percentile [{}] is not valid", per);
					throw new RuntimeException("percentile is not valid");
				}
			});
		}
	}

}
