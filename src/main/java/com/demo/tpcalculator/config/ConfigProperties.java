package com.demo.tpcalculator.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * @author yue.su
 * @date 2019年2月23日
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "config")
public class ConfigProperties {

	private String directory = "/var/log/httpd/";

	private long samplingThreshold = 1000000L;

	private int bucketSize = 10;

	private int defaultConcurrency = 4;

	private String defaultCharset = "utf8";

	private List<String> defaultPercentiles = Arrays.asList("99", "95", "90");

}
