/*
 * Copyright 2016-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import configuration.JobConfiguration;
import configuration.JobSkipConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.task.batch.listener.support.JobExecutionEvent;
import org.springframework.cloud.task.batch.listener.support.StepExecutionEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
public class BatchExecutionEventTests {

	private static final String TASK_NAME = "taskEventTest";

	private final ObjectMapper objectMapper = new ObjectMapper();
	private ConfigurableApplicationContext applicationContext;

	@BeforeEach
	public void setup() {
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@AfterEach
	public void tearDown() {
		if (this.applicationContext != null && this.applicationContext.isActive()) {
			this.applicationContext.close();
		}
	}

	@Test
	public void testContext() {
		this.applicationContext = new SpringApplicationBuilder()
			.sources(TestChannelBinderConfiguration
				.getCompleteConfiguration(BatchEventsApplication.class)).web(WebApplicationType.NONE)
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
	}

	@Test
	public void testJobEventListener() throws Exception {
		List<Message<byte[]>> result = testListener(
			"--spring.cloud.task.batch.events.jobExecutionEventBindingName=foobar", "foobar", 1);
		JobExecutionEvent jobExecutionEvent = this.objectMapper.readValue(result.get(0).getPayload(),
			JobExecutionEvent.class);
		Assertions.assertThat(jobExecutionEvent.getJobInstance().getJobName())
			.isEqualTo("job").as("Job name should be job");
	}

	@Test
	public void testStepEventListener() throws Exception {
		final String bindingName = "step-execution-foobar";
		List<Message<byte[]>> result = testListener(
			"--spring.cloud.task.batch.events.stepExecutionEventBindingName=" + bindingName,
			bindingName, 4);
		int stepOneCount = 0;
		int stepTwoCount = 0;
		for (int i = 0; i < 4; i++) {
			StepExecutionEvent stepExecutionEvent = this.objectMapper.readValue(result.get(i).getPayload(),
				StepExecutionEvent.class);
			if (stepExecutionEvent.getStepName().equals("step1")) {
				stepOneCount++;
			}
			if (stepExecutionEvent.getStepName().equals("step2")) {
				stepTwoCount++;
			}
		}

		assertThat(stepOneCount).as("the number of step1 events did not match")
			.isEqualTo(2);
		assertThat(stepTwoCount).as("the number of step2 events did not match")
			.isEqualTo(2);

	}

	@Test
	public void testItemProcessEventListener() {
		final String bindingName = "item-execution-foobar";

		List<Message<byte[]>> result = testListener(
			"--spring.cloud.task.batch.events.itemProcessEventBindingName=" + bindingName,
			bindingName, 1);
		String value = new String(result.get(0).getPayload());
		assertThat(value).isEqualTo("\"item did not equal result after processing\"");

	}

	@Test
	public void testChunkListener() {
		final String bindingName = "chunk-events-foobar";

		List<Message<byte[]>> result = testListener(
			"--spring.cloud.task.batch.events.chunkEventBindingName=" + bindingName,
			bindingName, 2);
		String value = new String(result.get(0).getPayload());
		assertThat(value).isEqualTo("\"Before Chunk Processing\"");
		value = new String(result.get(1).getPayload());
		assertThat(value).isEqualTo("\"After Chunk Processing\"");
	}

	@Test
	public void testWriteListener() {
		final String bindingName = "item-write-events-foobar";

		List<Message<byte[]>> result = testListener(
			"--spring.cloud.task.batch.events.itemWriteEventBindingName=" + bindingName,
			bindingName, 2);
		String value = new String(result.get(0).getPayload());
		assertThat(value).isEqualTo("\"3 items to be written.\"");
		value = new String(result.get(1).getPayload());
		assertThat(value).isEqualTo("\"3 items have been written.\"");
	}

	private String[] getCommandLineParams(String sinkChannelParam) {
		return getCommandLineParams(sinkChannelParam, true);
	}

	private String[] getCommandLineParams(String sinkChannelParam, boolean enableFailJobConfig) {
		String jobConfig = enableFailJobConfig ?
			"--spring.cloud.task.test.enable-job-configuration=true" :
			"--spring.cloud.task.test.enable-fail-job-configuration=true";
		return new String[]{"--spring.cloud.task.closecontext_enable=false",
			"--spring.cloud.task.name=" + TASK_NAME,
			"--spring.main.web-environment=false",
			"--spring.cloud.stream.defaultBinder=rabbit",
			"--spring.cloud.stream.bindings.task-events.destination=test",
			jobConfig,
			"foo=" + UUID.randomUUID(), sinkChannelParam};
	}

	private List<Message<byte[]>> testListener(String channelBinding, String bindingName, int numberToRead) {
		return testListenerForApp(channelBinding, bindingName, numberToRead, BatchEventsApplication.class, true);
	}

	private List<Message<byte[]>> testListenerSkip(String channelBinding, String bindingName, int numberToRead) {
		return testListenerForApp(channelBinding, bindingName, numberToRead, BatchSkipEventsApplication.class, false);
	}

	private List<Message<byte[]>> testListenerForApp(String channelBinding,
		String bindingName, int numberToRead, Class clazz, boolean enableFailJobConfig) {
		List<Message<byte[]>> results = new ArrayList<>();

		this.applicationContext = new SpringApplicationBuilder()
			.sources(TestChannelBinderConfiguration
				.getCompleteConfiguration(clazz)).web(WebApplicationType.NONE)
			.build().run(getCommandLineParams(channelBinding, enableFailJobConfig));

		OutputDestination target = this.applicationContext.getBean(OutputDestination.class);

		for (int i = 0; i < numberToRead; i++) {
			results.add(target.receive(10000, bindingName));
		}
		return results;
	}

	@Test
	public void testItemReadListener() {
		final String bindingName = "item-read-events-foobar";

		List<Message<byte[]>> result = testListenerSkip(
			"--spring.cloud.task.batch.events.itemReadEventBindingName=" + bindingName,
			bindingName, 1);
		String exceptionMessage = new String(result.get(0).getPayload());
		assertThat(exceptionMessage).isEqualTo("\"Exception while item was being read\"");
	}

	@Test
	public void testSkipEventListener() {
		final String SKIPPING_READ_MESSAGE = "\"Skipped when reading.\"";

		final String SKIPPING_WRITE_CONTENT = "\"-1\"";
		final String bindingName = "skip-event-foobar";
		List<Message<byte[]>> result = testListenerSkip(
			"--spring.cloud.task.batch.events.skipEventBindingName=" + bindingName,
			bindingName, 3);
		int readSkipCount = 0;
		int writeSkipCount = 0;
		for (int i = 0; i < 3; i++) {
			String exceptionMessage = new String(result.get(i).getPayload());
			if (exceptionMessage.equals(SKIPPING_READ_MESSAGE)) {
				readSkipCount++;
			}
			if (exceptionMessage.equals(SKIPPING_WRITE_CONTENT)) {
				writeSkipCount++;
			}
		}

		assertThat(readSkipCount).as("the number of read skip events did not match")
			.isEqualTo(2);
		assertThat(writeSkipCount).as("the number of write skip events did not match")
			.isEqualTo(1);
	}

	@SpringBootApplication
	@Import(JobConfiguration.class)
	public static class BatchEventsApplication {
	}

	@SpringBootApplication
	@Import(JobSkipConfiguration.class)
	public static class BatchSkipEventsApplication {
	}
}
