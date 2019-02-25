package com.demo.tpcalculator.service;

import java.util.List;

public interface TopPercentileService {

	String getTopPercentile(String directory, List<String> percentiles);
}
