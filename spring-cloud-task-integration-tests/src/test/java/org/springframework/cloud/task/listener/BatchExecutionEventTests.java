
/*
 *  Copyright 2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.listener;

import java.util.concurrent.*;

import configuration.JobConfiguration;
import configuration.JobSkipConfiguration;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.redis.config.RedisServiceAutoConfiguration;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.stream.test.junit.redis.RedisTestSupport;
import org.springframework.cloud.task.batch.configuration.TaskBatchAutoConfiguration;
import org.springframework.cloud.task.batch.listener.BatchEventAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Glenn Renfro
 */
public class BatchExecutionEventTests {
	@ClassRule
	public static RedisTestSupport redisTestSupport = new RedisTestSupport();

	// Count for two job execution events per job
	static CountDownLatch jobExecutionLatch = new CountDownLatch(2);

	// Count for four step execution events per job
	static CountDownLatch stepExecutionLatch = new CountDownLatch(4);

	// Count for twelve item process events per job
	static CountDownLatch itemProcessLatch = new CountDownLatch(6);

	// Count for eight chunk events per job
	static CountDownLatch chunkEventsLatch = new CountDownLatch(8);

	// Count for zero read events per job
	static CountDownLatch itemReadEventsLatch = new CountDownLatch(0);

	// Count for six write events per job
	static CountDownLatch itemWriteEventsLatch = new CountDownLatch(2);

	// Count for 2 skip events per job
	static CountDownLatch skipEventsLatch = new CountDownLatch(2);


	private static final String TASK_NAME = "jobEventTest";

	private ConfigurableApplicationContext applicationContext;

	@After
	public void tearDown() {
		if (applicationContext != null && applicationContext.isActive() ) {
			applicationContext.close();
		}
	}

	@Test
	public void testContext() {
		applicationContext = new SpringApplicationBuilder()
				.sources(this.getConfigurations(BatchExecutionEventTests.ListenerBinding.class, JobConfiguration.class))
				.build().run(new String[]{ "--spring.cloud.task.closecontext.enable=false" });

		assertNotNull(applicationContext.getBean("jobExecutionEventsListener"));
		assertNotNull(applicationContext.getBean("stepExecutionEventsListener"));
		assertNotNull(applicationContext.getBean("chunkEventsListener"));
		assertNotNull(applicationContext.getBean("itemReadEventsListener"));
		assertNotNull(applicationContext.getBean("itemWriteEventsListener"));
		assertNotNull(applicationContext.getBean("itemProcessEventsListener"));
		assertNotNull(applicationContext.getBean("skipEventsListener"));
		assertNotNull(applicationContext.getBean(BatchEventAutoConfiguration.BatchEventsChannels.class));

	}

	@Test
	public void testEventListener() throws Exception {
		testListener("--spring.cloud.stream.bindings.job-execution-events.destination=foobar",
				jobExecutionLatch, BatchExecutionEventTests.ListenerBinding.class);

	}

	@Test
	public void testStepEventListener() throws Exception {
		testListener("--spring.cloud.stream.bindings.step-execution-events.destination=step-execution-foobar",
				stepExecutionLatch, BatchExecutionEventTests.StepListenerBinding.class);
	}

	@Test
	public void testItemProcessEventListener() throws Exception {
		testListener("--spring.cloud.stream.bindings.item-process-events.destination=item-process-foobar",
				itemProcessLatch, BatchExecutionEventTests.ItemProcessListenerBinding.class);
	}


	@Test
	public void testChunkListener() throws Exception {
		testListener("--spring.cloud.stream.bindings.chunk-events.destination=chunk-events-foobar",
				chunkEventsLatch, BatchExecutionEventTests.ChunkEventsListenerBinding.class);
	}

	@Test
	public void testItemReadListener() throws Exception {
		testListener("--spring.cloud.stream.bindings.item-read-events.destination=item-read-events-foobar",
				itemReadEventsLatch, BatchExecutionEventTests.ItemReadEventsListenerBinding.class);
	}

	@Test
	public void testWriteListener() throws Exception {
		testListener("--spring.cloud.stream.bindings.item-write-events.destination=item-write-events-foobar",
				itemWriteEventsLatch, BatchExecutionEventTests.ItemWriteEventsListenerBinding.class);
	}

	@Test
	public void testSkipEventListener() throws Exception {
		testListenerSkip("--spring.cloud.stream.bindings.skip-events.destination=skip-event-foobar",
				skipEventsLatch, BatchExecutionEventTests.SkipEventsListenerBinding.class);
	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:/org/springframework/cloud/task/listener/job-execution-sink-channel.properties")
	@EnableAutoConfiguration
	public static class ListenerBinding {

		@StreamListener(Sink.INPUT)
		public void receive(JobExecution execution) {
			assertEquals(String.format("Job name should be job"), "job", execution.getJobInstance().getJobName());
			jobExecutionLatch.countDown();
		}
	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:/org/springframework/cloud/task/listener/step-execution-sink-channel.properties")
	@EnableAutoConfiguration
	public static class StepListenerBinding {

		@StreamListener(Sink.INPUT)
		public void receive(StepExecution execution) {
			assertEquals(String.format("Job name should be job"), "job", execution.getJobExecution().getJobInstance().getJobName());
			stepExecutionLatch.countDown();
		}
	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:/org/springframework/cloud/task/listener/item-process-sink-channel.properties")
	@EnableAutoConfiguration
	public static class ItemProcessListenerBinding {

		@StreamListener(Sink.INPUT)
		public void receive(Object object) {
			itemProcessLatch.countDown();
		}
	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:/org/springframework/cloud/task/listener/chunk-events-sink-channel.properties")
	@EnableAutoConfiguration
	public static class ChunkEventsListenerBinding {

		@StreamListener(Sink.INPUT)
		public void receive(Object chunkContext) {
			chunkEventsLatch.countDown();
		}
	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:/org/springframework/cloud/task/listener/item-read-events-sink-channel.properties")
	@EnableAutoConfiguration
	public static class ItemReadEventsListenerBinding {

		@StreamListener(Sink.INPUT)
		public void receive(Object itemRead) {
			itemReadEventsLatch.countDown();
		}
	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:/org/springframework/cloud/task/listener/skip-events-sink-channel.properties")
	@EnableAutoConfiguration
	public static class SkipEventsListenerBinding {

		@StreamListener(Sink.INPUT)
		public void receive(Object exceptionMessage) {
			assertEquals("Exception message is not correct","Skipped when reading.",exceptionMessage);
			skipEventsLatch.countDown();
		}
	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:/org/springframework/cloud/task/listener/item-write-events-sink-channel.properties")
	@EnableAutoConfiguration
	public static class ItemWriteEventsListenerBinding {

		@StreamListener(Sink.INPUT)
		public void receive(Object itemWrite) {
			assertTrue("Message should start with '3 items'", itemWrite.toString().startsWith("3 items "));
			assertTrue("Message should end with ' written.'", itemWrite.toString().endsWith(" written."));
			itemWriteEventsLatch.countDown();
		}
	}

	private Object[] getConfigurations(Class sinkClazz, Class jobConfigurationClazz) {
		return new Object[]{
				jobConfigurationClazz,
				PropertyPlaceholderAutoConfiguration.class,
				BatchAutoConfiguration.class,
				TaskBatchAutoConfiguration.class,
				TaskEventAutoConfiguration.class,
				BatchEventAutoConfiguration.class,
				RedisServiceAutoConfiguration.class,
				sinkClazz };
	}

	private String[] getCommandLineParams(String sinkChannelParam) {
		return new String[]{ "--spring.cloud.task.closecontext.enable=false",
				"--spring.cloud.task.name=" + TASK_NAME,
				"--spring.main.web-environment=false",
				"--spring.cloud.stream.defaultBinder=redis",
				"--spring.cloud.stream.bindings.task-events.destination=test",
				sinkChannelParam };
	}

	private  void testListener(String channelBinding, CountDownLatch latch, Class<?> clazz) throws Exception{
		applicationContext = new SpringApplicationBuilder()
				.sources(this.getConfigurations(clazz, JobConfiguration.class))
				.build().run(getCommandLineParams(channelBinding));

		assertTrue(latch.await(1, TimeUnit.SECONDS));
	}

	private  void testListenerSkip(String channelBinding, CountDownLatch latch, Class<?> clazz) throws Exception{
		applicationContext = new SpringApplicationBuilder()
				.sources(this.getConfigurations(clazz, JobSkipConfiguration.class))
				.build().run(getCommandLineParams(channelBinding));

		assertTrue(latch.await(1, TimeUnit.SECONDS));
	}
}
