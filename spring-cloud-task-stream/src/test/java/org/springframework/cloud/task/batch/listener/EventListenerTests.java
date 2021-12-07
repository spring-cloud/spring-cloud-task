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

package org.springframework.cloud.task.batch.listener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.task.batch.listener.support.JobExecutionEvent;
import org.springframework.cloud.task.batch.listener.support.MessagePublisher;
import org.springframework.cloud.task.batch.listener.support.StepExecutionEvent;
import org.springframework.cloud.task.batch.listener.support.TaskEventProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 * @author Ali Shahbour
 */
public class EventListenerTests {

	private EventEmittingSkipListener eventEmittingSkipListener;

	private EventEmittingItemProcessListener eventEmittingItemProcessListener;

	private EventEmittingItemReadListener eventEmittingItemReadListener;

	private EventEmittingItemWriteListener eventEmittingItemWriteListener;

	private EventEmittingJobExecutionListener eventEmittingJobExecutionListener;

	private EventEmittingStepExecutionListener eventEmittingStepExecutionListener;

	private EventEmittingChunkListener eventEmittingChunkListener;

	private ConfigurableApplicationContext applicationContext;

	private final TaskEventProperties taskEventProperties = new TaskEventProperties();

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	public void beforeTests() {
		this.applicationContext = new SpringApplicationBuilder()
			.sources(TestChannelBinderConfiguration
				.getCompleteConfiguration(BatchEventsApplication.class)).web(WebApplicationType.NONE).build()
			.run();
		StreamBridge streamBridge = this.applicationContext.getBean(StreamBridge.class);
		MessagePublisher messagePublisher = new MessagePublisher(streamBridge);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		this.eventEmittingSkipListener = new EventEmittingSkipListener(
			messagePublisher, this.taskEventProperties);
		this.eventEmittingItemProcessListener = new EventEmittingItemProcessListener(
			messagePublisher, this.taskEventProperties);
		this.eventEmittingItemReadListener = new EventEmittingItemReadListener(
			messagePublisher, this.taskEventProperties);
		this.eventEmittingItemWriteListener = new EventEmittingItemWriteListener(
			messagePublisher, this.taskEventProperties);
		this.eventEmittingJobExecutionListener = new EventEmittingJobExecutionListener(
			messagePublisher, this.taskEventProperties);
		this.eventEmittingStepExecutionListener = new EventEmittingStepExecutionListener(
			messagePublisher, this.taskEventProperties);
		this.eventEmittingChunkListener = new EventEmittingChunkListener(
			messagePublisher, 0, this.taskEventProperties);
	}

	@AfterEach
	public void tearDown() {
		if (this.applicationContext != null && this.applicationContext.isActive()) {
			this.applicationContext.close();
		}
	}

	@Test
	public void testEventListenerOrderProperty() {
		assertThat(Ordered.LOWEST_PRECEDENCE)
				.isEqualTo(this.eventEmittingSkipListener.getOrder());
		assertThat(Ordered.LOWEST_PRECEDENCE)
				.isEqualTo(this.eventEmittingItemProcessListener.getOrder());
		assertThat(Ordered.LOWEST_PRECEDENCE)
				.isEqualTo(this.eventEmittingItemReadListener.getOrder());
		assertThat(Ordered.LOWEST_PRECEDENCE)
				.isEqualTo(this.eventEmittingItemWriteListener.getOrder());
		assertThat(Ordered.LOWEST_PRECEDENCE)
				.isEqualTo(this.eventEmittingJobExecutionListener.getOrder());
		assertThat(Ordered.LOWEST_PRECEDENCE)
				.isEqualTo(this.eventEmittingStepExecutionListener.getOrder());
		assertThat(0).isEqualTo(this.eventEmittingChunkListener.getOrder());
	}

	@Test
	public void testItemProcessListenerOnProcessorError() {
		this.eventEmittingItemProcessListener.onProcessError("HELLO",
			new RuntimeException("Test Exception"));

		assertThat(getStringFromDestination(this.taskEventProperties.getItemProcessEventBindingName()))
				.isEqualTo("Exception while item was being processed");
	}

	@Test
	public void testItemProcessListenerAfterProcess() {
		this.eventEmittingItemProcessListener.afterProcess("HELLO_AFTER_PROCESS_EQUAL",
				"HELLO_AFTER_PROCESS_EQUAL");
		assertThat(getStringFromDestination(this.taskEventProperties.getItemProcessEventBindingName()))
			.isEqualTo("item equaled result after processing");

		this.eventEmittingItemProcessListener.afterProcess("HELLO_NOT_EQUAL", "WORLD");
		assertThat(getStringFromDestination(this.taskEventProperties.getItemProcessEventBindingName()))
				.isEqualTo("item did not equal result after processing");

		this.eventEmittingItemProcessListener.afterProcess("HELLO_AFTER_PROCESS", null);
		assertThat(getStringFromDestination(this.taskEventProperties.
			getItemProcessEventBindingName())).isEqualTo("1 item was filtered");
	}

	@Test
	public void testItemProcessBeforeProcessor() {
		this.eventEmittingItemProcessListener.beforeProcess("HELLO_BEFORE_PROCESS");
		assertNoMessageFromDestination(this.taskEventProperties.getItemProcessEventBindingName());
	}

	@Test
	public void EventEmittingSkipListenerSkipRead() {
		this.eventEmittingSkipListener.onSkipInRead(new RuntimeException("Text Exception"));
		assertThat(getStringFromDestination(this.taskEventProperties.
			getSkipEventBindingName())).isEqualTo("Skipped when reading.");
	}

	@Test
	public void EventEmittingSkipListenerSkipWrite() {
		final String MESSAGE = "\"HELLO_SKIP_WRITE\"";
		this.eventEmittingSkipListener.onSkipInWrite(MESSAGE,
			new RuntimeException("Text Exception"));
		assertThat(getStringFromDestination(this.taskEventProperties.
			getSkipEventBindingName())).isEqualTo(MESSAGE);
	}

	@Test
	public void EventEmittingSkipListenerSkipProcess() {
		final String MESSAGE = "\"HELLO_SKIP_PROCESS\"";
		this.eventEmittingSkipListener.onSkipInProcess(MESSAGE,
			new RuntimeException("Text Exception"));
		assertThat(getStringFromDestination(this.taskEventProperties.
			getSkipEventBindingName())).isEqualTo(MESSAGE);
	}

	@Test
	public void EventEmittingItemReadListener() {
		this.eventEmittingItemReadListener.onReadError(new RuntimeException("Text Exception"));
		assertThat(getStringFromDestination(this.taskEventProperties.
			getItemReadEventBindingName())).isEqualTo("Exception while item was being read");
	}

	@Test
	public void EventEmittingItemReadListenerBeforeRead() {
		this.eventEmittingItemReadListener.beforeRead();
		assertNoMessageFromDestination(this.taskEventProperties.getItemReadEventBindingName());
	}

	@Test
	public void EventEmittingItemReadListenerAfterRead() {
		this.eventEmittingItemReadListener.afterRead("HELLO_AFTER_READ");
		assertNoMessageFromDestination(this.taskEventProperties.getItemReadEventBindingName());
	}

	@Test
	public void EventEmittingItemWriteListenerBeforeWrite() {
		this.eventEmittingItemWriteListener.beforeWrite(getSampleList());
		assertThat(getStringFromDestination(this.taskEventProperties.getItemWriteEventBindingName()))
			.isEqualTo("3 items to be written.");
	}

	@Test
	public void EventEmittingItemWriteListenerAfterWrite() {
		this.eventEmittingItemWriteListener.afterWrite(getSampleList());
		assertThat(getStringFromDestination(this.taskEventProperties.getItemWriteEventBindingName()))
			.isEqualTo("3 items have been written.");
	}

	@Test
	public void EventEmittingItemWriteListenerWriteError() {
		RuntimeException exception = new RuntimeException("Text Exception");
		this.eventEmittingItemWriteListener.onWriteError(exception, getSampleList());

		assertThat(getStringFromDestination(this.taskEventProperties.getItemWriteEventBindingName()))
				.isEqualTo("Exception while 3 items are attempted to be written.");
	}

	@Test
	public void EventEmittingJobExecutionListenerBeforeJob() throws IOException {
		JobExecution jobExecution = getJobExecution();
		this.eventEmittingJobExecutionListener.beforeJob(jobExecution);
		List<Message<byte[]>> result = testListener(this.taskEventProperties.getJobExecutionEventBindingName(), 1);
		assertThat(result.get(0)).isNotNull();

		JobExecutionEvent jobEvent =  this.objectMapper.readValue(result.get(0).getPayload(), JobExecutionEvent.class);
		assertThat(jobEvent.getJobInstance().getJobName())
				.isEqualTo(jobExecution.getJobInstance().getJobName());
	}

	@Test
	public void EventEmittingJobExecutionListenerAfterJob() throws IOException {
		JobExecution jobExecution = getJobExecution();
		this.eventEmittingJobExecutionListener.afterJob(jobExecution);
		List<Message<byte[]>> result = testListener(this.taskEventProperties.getJobExecutionEventBindingName(), 1);
		assertThat(result.get(0)).isNotNull();

		JobExecutionEvent jobEvent =  this.objectMapper.readValue(result.get(0).getPayload(), JobExecutionEvent.class);
		assertThat(jobEvent.getJobInstance().getJobName())
				.isEqualTo(jobExecution.getJobInstance().getJobName());
	}

	@Test
	public void EventEmittingStepExecutionListenerBeforeStep() throws IOException {
		final String STEP_MESSAGE = "BEFORE_STEP_MESSAGE";
		StepExecution stepExecution = new StepExecution(STEP_MESSAGE, getJobExecution());
		this.eventEmittingStepExecutionListener.beforeStep(stepExecution);

		List<Message<byte[]>> result = testListener(this.taskEventProperties.getStepExecutionEventBindingName(), 1);
		assertThat(result.get(0)).isNotNull();

		StepExecutionEvent stepExecutionEvent =  this.objectMapper.readValue(result.get(0).getPayload(), StepExecutionEvent.class);
		assertThat(stepExecutionEvent.getStepName()).isEqualTo(STEP_MESSAGE);
	}

	@Test
	public void EventEmittingStepExecutionListenerAfterStep() throws IOException {
		final String STEP_MESSAGE = "AFTER_STEP_MESSAGE";
		StepExecution stepExecution = new StepExecution(STEP_MESSAGE, getJobExecution());
		this.eventEmittingStepExecutionListener.afterStep(stepExecution);
		List<Message<byte[]>> result = testListener(this.taskEventProperties.getStepExecutionEventBindingName(), 1);

		assertThat(result.get(0)).isNotNull();
		StepExecutionEvent stepExecutionEvent = this.objectMapper.readValue(result.get(0).getPayload(), StepExecutionEvent.class);
		assertThat(stepExecutionEvent.getStepName()).isEqualTo(STEP_MESSAGE);
	}

	@Test
	public void EventEmittingChunkExecutionListenerBeforeChunk() {
		final String CHUNK_MESSAGE = "Before Chunk Processing";
		this.eventEmittingChunkListener.beforeChunk(getChunkContext());
		assertThat(getStringFromDestination(this.taskEventProperties.getChunkEventBindingName()))
			.isEqualTo(CHUNK_MESSAGE);
	}

	@Test
	public void EventEmittingChunkExecutionListenerAfterChunk() {
		final String CHUNK_MESSAGE = "After Chunk Processing";
		this.eventEmittingChunkListener.afterChunk(getChunkContext());
		assertThat(getStringFromDestination(this.taskEventProperties.getChunkEventBindingName()))
			.isEqualTo(CHUNK_MESSAGE);
	}

	@Test
	public void EventEmittingChunkExecutionListenerAfterChunkError() {
		this.eventEmittingChunkListener.afterChunkError(getChunkContext());
		assertNoMessageFromDestination(this.taskEventProperties.getChunkEventBindingName());
	}

	private JobExecution getJobExecution() {
		final String JOB_NAME = UUID.randomUUID().toString();
		JobInstance jobInstance = new JobInstance(1L, JOB_NAME);
		return new JobExecution(jobInstance, 1L, new JobParameters(),
				UUID.randomUUID().toString());
	}

	private List<String> getSampleList() {
		List<String> testList = new ArrayList<>(3);
		testList.add("Hello");
		testList.add("World");
		testList.add("foo");
		return testList;
	}

	private ChunkContext getChunkContext() {
		JobExecution jobExecution = getJobExecution();
		StepExecution stepExecution = new StepExecution("STEP1", jobExecution);
		StepContext stepContext = new StepContext(stepExecution);
		ChunkContext chunkContext = new ChunkContext(stepContext);
		return chunkContext;
	}

	private List<Message<byte[]>> testListener(String bindingName, int numberToRead) {
		List<Message<byte[]>> results = new ArrayList<>();
		OutputDestination target = this.applicationContext.getBean(OutputDestination.class);
		for (int i = 0; i < numberToRead; i++) {
			results.add(target.receive(10000, bindingName));
		}
		return results;
	}

	private String getStringFromDestination(String bindingName) {
		List<Message<byte[]>> result = testListener(bindingName, 1);
		assertThat(result.get(0)).isNotNull();

		assertThat(new String(result.get(0).getPayload()));
		return new String(result.get(0).getPayload());
	}

	private void assertNoMessageFromDestination(String bindingName) {
		List<Message<byte[]>> result = testListener(bindingName, 1);
		assertThat(result.get(0)).isNull();
	}

	@SpringBootApplication
	public static class BatchEventsApplication {
	}
}
