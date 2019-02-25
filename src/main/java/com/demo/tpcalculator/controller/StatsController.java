package com.demo.tpcalculator.controller;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.demo.tpcalculator.config.ConfigProperties;
import com.demo.tpcalculator.service.TopPercentileService;

/**
 * @author yue.su
 * @date 2019年2月23日
 */
@RestController
@RequestMapping("/stats")
public class StatsController {

	@Autowired
	@Qualifier("defaultPercentileCalculationService")
	private TopPercentileService percentileService;
	@Autowired
	private ConfigProperties properties;

	@GetMapping("/tp")
	public String testTp(String[] percentiles) {
		List<String> pList = properties.getDefaultPercentiles();
		if (percentiles != null && percentiles.length > 0) {
			pList = Arrays.asList(percentiles);
		}
		return percentileService.getTopPercentile(properties.getDirectory(), pList);
	}

}
