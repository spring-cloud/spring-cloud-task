package io.spring.cloud;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.batch.core.StepExecution;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.OutputCapture;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.stream.test.junit.redis.RedisTestSupport;
import org.springframework.context.annotation.PropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class BatchEventsApplicationTests {

	private static final String ITEM_INDICATOR = ">> -";

	@ClassRule
	public static RedisTestSupport redisTestSupport = new RedisTestSupport();

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	// Count for two job execution events per task
	static CountDownLatch latch = new CountDownLatch(4);


	@Test
	public void testExecution() throws Exception {
		SpringApplication.run(BatchEventsApplication.class);
		String output = this.outputCapture.toString();

		String taskIndicator = "Tasklet has run";
		Pattern pattern = Pattern.compile(taskIndicator);
		Matcher matcher = pattern.matcher(output);

		int count = 0;
		while (matcher.find()) {
			count++;
		}
		assertEquals("The number of task indicators did not match expected: ", 1, count);

		pattern = Pattern.compile(ITEM_INDICATOR);
		matcher = pattern.matcher(output);
		count = 0;
		while (matcher.find()) {
			count++;
			validateItemCount(count, output);
		}
		assertEquals("The number of item indicators did not match expected: ", 6, count);

		Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));

	}

	private void validateItemCount(int itemNumber, String output) {
		assertTrue("Test results do not show create task message: " + output,
				output.contains(ITEM_INDICATOR + itemNumber));
	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:io/spring/task/listener/sink-channel.properties")
	@EnableAutoConfiguration
	public static class ListenerBinding {

		@StreamListener(Sink.INPUT)
		public void receive(StepExecution execution) {
			Assert.assertEquals(String.format("Job name should be job"), "job", execution.getJobExecution().getJobInstance().getJobName());
			latch.countDown();
		}
	}
}
