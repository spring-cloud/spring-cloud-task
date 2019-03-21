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

package org.springframework.cloud.task.batch.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.cloud.task.batch.listener.support.JobExecutionEvent;
import org.springframework.cloud.task.batch.listener.support.StepExecutionEvent;
import org.springframework.core.Ordered;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 * @author Ali Shahbour
 */
public class EventListenerTests {

	private QueueChannel queueChannel;

	private EventEmittingSkipListener eventEmittingSkipListener;

	private EventEmittingItemProcessListener eventEmittingItemProcessListener;

	private EventEmittingItemReadListener eventEmittingItemReadListener;

	private EventEmittingItemWriteListener eventEmittingItemWriteListener;

	private EventEmittingJobExecutionListener eventEmittingJobExecutionListener;

	private EventEmittingStepExecutionListener eventEmittingStepExecutionListener;

	private EventEmittingChunkListener eventEmittingChunkListener;

	@Before
	public void beforeTests() {
		this.queueChannel = new QueueChannel(1);
		this.eventEmittingSkipListener = new EventEmittingSkipListener(this.queueChannel);
		this.eventEmittingItemProcessListener = new EventEmittingItemProcessListener(
				this.queueChannel);
		this.eventEmittingItemReadListener = new EventEmittingItemReadListener(
				this.queueChannel);
		this.eventEmittingItemWriteListener = new EventEmittingItemWriteListener(
				this.queueChannel);
		this.eventEmittingJobExecutionListener = new EventEmittingJobExecutionListener(
				this.queueChannel);
		this.eventEmittingStepExecutionListener = new EventEmittingStepExecutionListener(
				this.queueChannel);
		this.eventEmittingChunkListener = new EventEmittingChunkListener(
				this.queueChannel, 0);
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
		RuntimeException exeption = new RuntimeException("Test Exception");
		this.eventEmittingItemProcessListener.onProcessError("HELLO", exeption);
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(1);

		Message msg = this.queueChannel.receive();
		assertThat(msg.getPayload())
				.isEqualTo("Exception while item was being processed");
	}

	@Test
	public void testItemProcessListenerAfterProcess() {
		this.eventEmittingItemProcessListener.afterProcess("HELLO_AFTER_PROCESS_EQUAL",
				"HELLO_AFTER_PROCESS_EQUAL");
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(1);
		Message msg = this.queueChannel.receive();
		assertThat(msg.getPayload()).isEqualTo("item equaled result after processing");

		this.eventEmittingItemProcessListener.afterProcess("HELLO_NOT_EQUAL", "WORLD");
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(1);
		msg = this.queueChannel.receive();
		assertThat(msg.getPayload())
				.isEqualTo("item did not equal result after processing");

		this.eventEmittingItemProcessListener.afterProcess("HELLO_AFTER_PROCESS", null);
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(1);
		msg = this.queueChannel.receive();
		assertThat(msg.getPayload()).isEqualTo("1 item was filtered");
	}

	@Test
	public void testItemProcessBeforeProcessor() {
		this.eventEmittingItemProcessListener.beforeProcess("HELLO_BEFORE_PROCESS");
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(0);
	}

	@Test
	public void EventEmittingSkipListenerSkipRead() {
		RuntimeException exeption = new RuntimeException("Text Exception");
		this.eventEmittingSkipListener.onSkipInRead(exeption);
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(1);
		Message msg = this.queueChannel.receive();
		assertThat(msg.getPayload()).isEqualTo("Skipped when reading.");
	}

	@Test
	public void EventEmittingSkipListenerSkipWrite() {
		final String MESSAGE = "HELLO_SKIP_WRITE";
		RuntimeException exeption = new RuntimeException("Text Exception");
		this.eventEmittingSkipListener.onSkipInWrite(MESSAGE, exeption);
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(1);
		Message msg = this.queueChannel.receive();
		assertThat(msg.getPayload()).isEqualTo(MESSAGE);
	}

	@Test
	public void EventEmittingSkipListenerSkipProcess() {
		final String MESSAGE = "HELLO_SKIP_PROCESS";
		RuntimeException exeption = new RuntimeException("Text Exception");
		this.eventEmittingSkipListener.onSkipInProcess(MESSAGE, exeption);
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(1);
		Message msg = this.queueChannel.receive();
		assertThat(msg.getPayload()).isEqualTo(MESSAGE);
	}

	@Test
	public void EventEmittingItemReadListener() {
		RuntimeException exeption = new RuntimeException("Text Exception");
		this.eventEmittingItemReadListener.onReadError(exeption);
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(1);
		Message msg = this.queueChannel.receive();
		assertThat(msg.getPayload()).isEqualTo("Exception while item was being read");
	}

	@Test
	public void EventEmittingItemReadListenerBeforeRead() {
		this.eventEmittingItemReadListener.beforeRead();
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(0);
	}

	@Test
	public void EventEmittingItemReadListenerAfterRead() {
		this.eventEmittingItemReadListener.afterRead("HELLO_AFTER_READ");
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(0);
	}

	@Test
	public void EventEmittingItemWriteListenerBeforeWrite() {
		this.eventEmittingItemWriteListener.beforeWrite(getSampleList());
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(1);
		Message msg = this.queueChannel.receive();
		assertThat(msg.getPayload()).isEqualTo("3 items to be written.");
	}

	@Test
	public void EventEmittingItemWriteListenerAfterWrite() {
		this.eventEmittingItemWriteListener.afterWrite(getSampleList());
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(1);
		Message msg = this.queueChannel.receive();
		assertThat(msg.getPayload()).isEqualTo("3 items have been written.");
	}

	@Test
	public void EventEmittingItemWriteListenerWriteError() {
		RuntimeException exeption = new RuntimeException("Text Exception");
		this.eventEmittingItemWriteListener.onWriteError(exeption, getSampleList());
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(1);
		Message msg = this.queueChannel.receive();
		assertThat(msg.getPayload())
				.isEqualTo("Exception while 3 items are attempted to be written.");
	}

	@Test
	public void EventEmittingJobExecutionListenerBeforeJob() {
		JobExecution jobExecution = getJobExecution();
		this.eventEmittingJobExecutionListener.beforeJob(jobExecution);
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(1);
		Message msg = this.queueChannel.receive();
		JobExecutionEvent jobEvent = (JobExecutionEvent) msg.getPayload();
		assertThat(jobEvent.getJobInstance().getJobName())
				.isEqualTo(jobExecution.getJobInstance().getJobName());
	}

	@Test
	public void EventEmittingJobExecutionListenerAfterJob() {
		JobExecution jobExecution = getJobExecution();
		this.eventEmittingJobExecutionListener.afterJob(jobExecution);
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(1);
		Message msg = this.queueChannel.receive();
		JobExecutionEvent jobEvent = (JobExecutionEvent) msg.getPayload();
		assertThat(jobEvent.getJobInstance().getJobName())
				.isEqualTo(jobExecution.getJobInstance().getJobName());
	}

	@Test
	public void EventEmittingStepExecutionListenerBeforeStep() {
		final String STEP_MESSAGE = "BEFORE_STEP_MESSAGE";
		JobExecution jobExecution = getJobExecution();
		StepExecution stepExecution = new StepExecution(STEP_MESSAGE, jobExecution);
		this.eventEmittingStepExecutionListener.beforeStep(stepExecution);
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(1);
		Message msg = this.queueChannel.receive();
		StepExecutionEvent stepExecutionEvent = (StepExecutionEvent) msg.getPayload();
		assertThat(stepExecutionEvent.getStepName()).isEqualTo(STEP_MESSAGE);
	}

	@Test
	public void EventEmittingStepExecutionListenerAfterStep() {
		final String STEP_MESSAGE = "AFTER_STEP_MESSAGE";
		JobExecution jobExecution = getJobExecution();
		StepExecution stepExecution = new StepExecution(STEP_MESSAGE, jobExecution);
		this.eventEmittingStepExecutionListener.afterStep(stepExecution);
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(1);
		Message msg = this.queueChannel.receive();
		StepExecutionEvent stepExecutionEvent = (StepExecutionEvent) msg.getPayload();
		assertThat(stepExecutionEvent.getStepName()).isEqualTo(STEP_MESSAGE);
	}

	@Test
	public void EventEmittingChunkExecutionListenerBeforeChunk() {
		final String CHUNK_MESSAGE = "Before Chunk Processing";
		ChunkContext chunkContext = getChunkContext();
		this.eventEmittingChunkListener.beforeChunk(chunkContext);
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(1);
		Message msg = this.queueChannel.receive();
		assertThat(msg.getPayload()).isEqualTo(CHUNK_MESSAGE);
	}

	@Test
	public void EventEmittingChunkExecutionListenerAfterChunk() {
		final String CHUNK_MESSAGE = "After Chunk Processing";
		ChunkContext chunkContext = getChunkContext();
		this.eventEmittingChunkListener.afterChunk(chunkContext);
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(1);
		Message msg = this.queueChannel.receive();
		assertThat(msg.getPayload()).isEqualTo(CHUNK_MESSAGE);
	}

	@Test
	public void EventEmittingChunkExecutionListenerAfterChunkError() {
		ChunkContext chunkContext = getChunkContext();
		this.eventEmittingChunkListener.afterChunkError(chunkContext);
		assertThat(this.queueChannel.getQueueSize()).isEqualTo(0);
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

}
