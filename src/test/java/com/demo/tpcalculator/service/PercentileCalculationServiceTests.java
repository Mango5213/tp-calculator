package com.demo.tpcalculator.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import com.demo.tpcalculator.config.ConfigProperties;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PercentileCalculationServiceTests {

	@Autowired
	private ConfigProperties properties;
	@Autowired
	DefaultPercentileCalculationService service;

	@Test
	public void testUniformDistributedWithAccuracy() throws IOException {
		List<Long> list = new ArrayList<>(11);
		properties.setBucketSize(1);
		for (int i = 0; i < 11; i++) {
			list.add(0L);
		}
		for (long i = 1L; i <= 10; i++) {
			list.set((int) i, 1L);
		}

		int rspTime = service.calculatePercentile(new BigDecimal("0.90"), list);
		Assert.assertEquals(9, rspTime);
		rspTime = service.calculatePercentile(new BigDecimal("0.95"), list);
		Assert.assertEquals(9, rspTime);
		rspTime = service.calculatePercentile(new BigDecimal("0.99"), list);
		Assert.assertEquals(9, rspTime);
		rspTime = service.calculatePercentile(new BigDecimal("1.00"), list);
		Assert.assertEquals(10, rspTime);
		rspTime = service.calculatePercentile(new BigDecimal("0.5"), list);
		Assert.assertEquals(5, rspTime);
	}

	@Test
	public void testUniformDistributedWithBucket() throws IOException {
		List<Long> list = new ArrayList<>(11);
		properties.setBucketSize(10);
		for (int i = 0; i < 11; i++) {
			list.add(0L);
		}
		for (long i = 1L; i <= 100; i++) {
			long value = list.get((int) i / 10);
			list.set((int) i / 10, ++value);
		}
		int rspTime = service.calculatePercentile(new BigDecimal("0.90"), list);
		Assert.assertEquals(90, rspTime);
		rspTime = service.calculatePercentile(new BigDecimal("0.95"), list);
		Assert.assertEquals(95, rspTime);
		rspTime = service.calculatePercentile(new BigDecimal("0.99"), list);
		Assert.assertEquals(99, rspTime);
		rspTime = service.calculatePercentile(new BigDecimal("0.5"), list);
		Assert.assertEquals(50, rspTime);
	}

}
