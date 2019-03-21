/*
 * Copyright 2016-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.listener;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import configuration.JobConfiguration;
import configuration.JobSkipConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.test.junit.rabbit.RabbitTestSupport;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.task.batch.listener.BatchEventAutoConfiguration;
import org.springframework.cloud.task.batch.listener.support.JobExecutionEvent;
import org.springframework.cloud.task.batch.listener.support.StepExecutionEvent;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
public class BatchExecutionEventTests {

	private static final String TASK_NAME = "jobEventTest";

	@ClassRule
	public static RabbitTestSupport rabbitTestSupport = new RabbitTestSupport();

	// Count for two job execution events per job
	static CountDownLatch jobExecutionLatch = new CountDownLatch(2);

	// Count for four step execution events per job
	static CountDownLatch stepExecutionLatch = new CountDownLatch(4);
	static int stepOneCount = 0;
	static int stepTwoCount = 0;

	// Count for twelve item process events per job
	static CountDownLatch itemProcessLatch = new CountDownLatch(6);

	// Count for eight chunk events per job
	static CountDownLatch chunkEventsLatch = new CountDownLatch(8);

	// Count for zero read events per job
	static CountDownLatch itemReadEventsLatch = new CountDownLatch(0);

	// Count for six write events per job
	static CountDownLatch itemWriteEventsLatch = new CountDownLatch(2);

	// Count for 3 skip events per job
	static CountDownLatch skipEventsLatch = new CountDownLatch(3);
	static int readSkipCount = 0;
	static int writeSkipCount = 0;

	private ConfigurableApplicationContext applicationContext;

	@After
	public void tearDown() {
		if (this.applicationContext != null && this.applicationContext.isActive()) {
			this.applicationContext.close();
		}
	}

	@Test
	public void testContext() {
		this.applicationContext = new SpringApplicationBuilder()
				.sources(this.getConfigurations(
						BatchExecutionEventTests.ListenerBinding.class,
						JobConfiguration.class))
				.build().run(getCommandLineParams(
						"--spring.cloud.stream.bindings.job-execution-events.destination=bazbar"));

		assertThat(this.applicationContext.getBean("jobExecutionEventsListener"))
				.isNotNull();
		assertThat(this.applicationContext.getBean("stepExecutionEventsListener"))
				.isNotNull();
		assertThat(this.applicationContext.getBean("chunkEventsListener")).isNotNull();
		assertThat(this.applicationContext.getBean("itemReadEventsListener")).isNotNull();
		assertThat(this.applicationContext.getBean("itemWriteEventsListener"))
				.isNotNull();
		assertThat(this.applicationContext.getBean("itemProcessEventsListener"))
				.isNotNull();
		assertThat(this.applicationContext.getBean("skipEventsListener")).isNotNull();
		assertThat(this.applicationContext
				.getBean(BatchEventAutoConfiguration.BatchEventsChannels.class))
						.isNotNull();

	}

	@Test
	public void testJobEventListener() throws Exception {
		testListener(
				"--spring.cloud.stream.bindings.job-execution-events.destination=foobar",
				jobExecutionLatch, BatchExecutionEventTests.ListenerBinding.class);

	}

	@Test
	public void testStepEventListener() throws Exception {
		testListener(
				"--spring.cloud.stream.bindings.step-execution-events.destination=step-execution-foobar",
				stepExecutionLatch, BatchExecutionEventTests.StepListenerBinding.class);
		assertThat(stepOneCount).as("the number of step1 events did not match")
				.isEqualTo(2);
		assertThat(stepTwoCount).as("the number of step2 events did not match")
				.isEqualTo(2);

	}

	@Test
	public void testItemProcessEventListener() throws Exception {
		testListener(
				"--spring.cloud.stream.bindings.item-process-events.destination=item-process-foobar",
				itemProcessLatch,
				BatchExecutionEventTests.ItemProcessListenerBinding.class);
	}

	@Test
	public void testChunkListener() throws Exception {
		testListener(
				"--spring.cloud.stream.bindings.chunk-events.destination=chunk-events-foobar",
				chunkEventsLatch,
				BatchExecutionEventTests.ChunkEventsListenerBinding.class);
	}

	@Test
	public void testItemReadListener() throws Exception {
		testListener(
				"--spring.cloud.stream.bindings.item-read-events.destination=item-read-events-foobar",
				itemReadEventsLatch,
				BatchExecutionEventTests.ItemReadEventsListenerBinding.class);
	}

	@Test
	public void testWriteListener() throws Exception {
		testListener(
				"--spring.cloud.stream.bindings.item-write-events.destination=item-write-events-foobar",
				itemWriteEventsLatch,
				BatchExecutionEventTests.ItemWriteEventsListenerBinding.class);
	}

	@Test
	public void testSkipEventListener() throws Exception {
		testListenerSkip(
				"--spring.cloud.stream.bindings.skip-events.destination=skip-event-foobar",
				skipEventsLatch,
				BatchExecutionEventTests.SkipEventsListenerBinding.class);
		assertThat(readSkipCount).as("read skip count did not match expected result")
				.isEqualTo(2);
		assertThat(writeSkipCount).as("write skip count did not match expected result")
				.isEqualTo(1);
	}

	private Class[] getConfigurations(Class sinkClazz, Class jobConfigurationClazz) {
		return new Class[] { PropertyPlaceholderAutoConfiguration.class,
				jobConfigurationClazz, sinkClazz };
	}

	private String[] getCommandLineParams(String sinkChannelParam) {
		return new String[] { "--spring.cloud.task.closecontext_enable=false",
				"--spring.cloud.task.name=" + TASK_NAME,
				"--spring.main.web-environment=false",
				"--spring.cloud.stream.defaultBinder=rabbit",
				"--spring.cloud.stream.bindings.task-events.destination=test",
				"foo=" + UUID.randomUUID().toString(), sinkChannelParam };
	}

	private void testListener(String channelBinding, CountDownLatch latch, Class<?> clazz)
			throws Exception {
		this.applicationContext = new SpringApplicationBuilder()
				.sources(this.getConfigurations(clazz, JobConfiguration.class)).build()
				.run(getCommandLineParams(channelBinding));

		assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
	}

	private void testListenerSkip(String channelBinding, CountDownLatch latch,
			Class<?> clazz) throws Exception {
		this.applicationContext = new SpringApplicationBuilder()
				.sources(this.getConfigurations(clazz, JobSkipConfiguration.class))
				.build().run(getCommandLineParams(channelBinding));

		assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:/org/springframework/cloud/task/listener/job-execution-sink-channel.properties")
	@EnableAutoConfiguration
	@EnableTask
	public static class ListenerBinding {

		@StreamListener(Sink.INPUT)
		public void receive(JobExecutionEvent execution) {
			Assertions.assertThat(execution.getJobInstance().getJobName())
					.isEqualTo("job").as("Job name should be job");
			jobExecutionLatch.countDown();
		}

	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:/org/springframework/cloud/task/listener/step-execution-sink-channel.properties")
	@EnableAutoConfiguration
	@EnableTask
	public static class StepListenerBinding {

		@StreamListener(Sink.INPUT)
		public void receive(StepExecutionEvent execution) {
			if (execution.getStepName().equals("step1")) {
				stepOneCount++;
			}
			if (execution.getStepName().equals("step2")) {
				stepTwoCount++;
			}

			stepExecutionLatch.countDown();
		}

	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:/org/springframework/cloud/task/listener/item-process-sink-channel.properties")
	@EnableAutoConfiguration
	@EnableTask
	public static class ItemProcessListenerBinding {

		@StreamListener(Sink.INPUT)
		public void receive(String object) {
			itemProcessLatch.countDown();
		}

	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:/org/springframework/cloud/task/listener/chunk-events-sink-channel.properties")
	@EnableAutoConfiguration
	@EnableTask
	public static class ChunkEventsListenerBinding {

		@StreamListener(Sink.INPUT)
		public void receive(String message) {
			chunkEventsLatch.countDown();
		}

	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:/org/springframework/cloud/task/listener/item-read-events-sink-channel.properties")
	@EnableAutoConfiguration
	@EnableTask
	public static class ItemReadEventsListenerBinding {

		@StreamListener(Sink.INPUT)
		public void receive(Object itemRead) {
			itemReadEventsLatch.countDown();
		}

	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:/org/springframework/cloud/task/listener/skip-events-sink-channel.properties")
	@EnableAutoConfiguration
	@EnableTask
	public static class SkipEventsListenerBinding {

		private static final String SKIPPING_READ_MESSAGE = "Skipped when reading.";

		private static final String SKIPPING_WRITE_CONTENT = "-1";

		@StreamListener(Sink.INPUT)
		public void receive(String exceptionMessage) {
			if (exceptionMessage.toString().equals(SKIPPING_READ_MESSAGE)) {
				readSkipCount++;
			}
			if (exceptionMessage.toString().equals(SKIPPING_WRITE_CONTENT)) {
				writeSkipCount++;
			}
			skipEventsLatch.countDown();
		}

	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:/org/springframework/cloud/task/listener/item-write-events-sink-channel.properties")
	@EnableAutoConfiguration
	@EnableTask
	public static class ItemWriteEventsListenerBinding {

		@StreamListener(Sink.INPUT)
		public void receive(String itemWrite) {
			Assertions.assertThat(itemWrite).startsWith("3 items ")
					.as("Message should start with '3 items'");
			Assertions.assertThat(itemWrite).endsWith("written.")
					.as("Message should end with ' written.'");
			itemWriteEventsLatch.countDown();
		}

	}

}
