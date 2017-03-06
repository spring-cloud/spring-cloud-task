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

import static org.junit.Assert.assertEquals;

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
		queueChannel = new QueueChannel(1);
		eventEmittingSkipListener = new EventEmittingSkipListener(queueChannel);
		eventEmittingItemProcessListener = new EventEmittingItemProcessListener(queueChannel);
		eventEmittingItemReadListener = new EventEmittingItemReadListener(queueChannel);
		eventEmittingItemWriteListener = new EventEmittingItemWriteListener(queueChannel);
		eventEmittingJobExecutionListener = new EventEmittingJobExecutionListener(queueChannel);
		eventEmittingStepExecutionListener = new EventEmittingStepExecutionListener(queueChannel);
		eventEmittingChunkListener = new EventEmittingChunkListener(queueChannel,0);
	}

	@Test
	public void testEventListenerOrderProperty() {
		assertEquals(eventEmittingSkipListener.getOrder(),Ordered.LOWEST_PRECEDENCE);
		assertEquals(eventEmittingItemProcessListener.getOrder(), Ordered.LOWEST_PRECEDENCE);
		assertEquals(eventEmittingItemReadListener.getOrder(), Ordered.LOWEST_PRECEDENCE);
		assertEquals(eventEmittingItemWriteListener.getOrder(), Ordered.LOWEST_PRECEDENCE);
		assertEquals(eventEmittingJobExecutionListener.getOrder(), Ordered.LOWEST_PRECEDENCE);
		assertEquals(eventEmittingStepExecutionListener.getOrder(), Ordered.LOWEST_PRECEDENCE);
		assertEquals(eventEmittingChunkListener.getOrder(),0);
	}


	@Test
	public void testItemProcessListenerOnProcessorError() {
		RuntimeException exeption = new RuntimeException("Test Exception");
		eventEmittingItemProcessListener.onProcessError("HELLO", exeption);
		assertEquals(1, queueChannel.getQueueSize());

		Message msg = queueChannel.receive();
		assertEquals("Exception while item was being processed", msg.getPayload());
	}

	@Test
	public void testItemProcessListenerAfterProcess() {
		eventEmittingItemProcessListener.afterProcess("HELLO_AFTER_PROCESS_EQUAL",
				"HELLO_AFTER_PROCESS_EQUAL");
		assertEquals(1, queueChannel.getQueueSize());
		Message msg = queueChannel.receive();
		assertEquals("item equaled result after processing", msg.getPayload());

		eventEmittingItemProcessListener.afterProcess("HELLO_NOT_EQUAL", "WORLD");
		assertEquals(1, queueChannel.getQueueSize());
		msg = queueChannel.receive();
		assertEquals("item did not equal result after processing", msg.getPayload());

		eventEmittingItemProcessListener.afterProcess("HELLO_AFTER_PROCESS", null);
		assertEquals(1, queueChannel.getQueueSize());
		msg = queueChannel.receive();
		assertEquals("1 item was filtered", msg.getPayload());
	}

	@Test
	public void testItemProcessBeforeProcessor() {
		eventEmittingItemProcessListener.beforeProcess("HELLO_BEFORE_PROCESS");
		assertEquals(0, queueChannel.getQueueSize());
	}

	@Test
	public void EventEmittingSkipListenerSkipRead() {
		RuntimeException exeption = new RuntimeException("Text Exception");
		eventEmittingSkipListener.onSkipInRead(exeption);
		assertEquals(1, queueChannel.getQueueSize());
		Message msg = queueChannel.receive();
		assertEquals("Skipped when reading.", msg.getPayload());
	}

	@Test
	public void EventEmittingSkipListenerSkipWrite() {
		final String MESSAGE = "HELLO_SKIP_WRITE";
		RuntimeException exeption = new RuntimeException("Text Exception");
		eventEmittingSkipListener.onSkipInWrite(MESSAGE, exeption);
		assertEquals(1, queueChannel.getQueueSize());
		Message msg = queueChannel.receive();
		assertEquals(MESSAGE, msg.getPayload());
	}

	@Test
	public void EventEmittingSkipListenerSkipProcess() {
		final String MESSAGE = "HELLO_SKIP_PROCESS";
		RuntimeException exeption = new RuntimeException("Text Exception");
		eventEmittingSkipListener.onSkipInProcess(MESSAGE, exeption);
		assertEquals(1, queueChannel.getQueueSize());
		Message msg = queueChannel.receive();
		assertEquals(MESSAGE, msg.getPayload());
	}

	@Test
	public void EventEmittingItemReadListener() {
		RuntimeException exeption = new RuntimeException("Text Exception");
		eventEmittingItemReadListener.onReadError(exeption);
		assertEquals(1, queueChannel.getQueueSize());
		Message msg = queueChannel.receive();
		assertEquals("Exception while item was being read", msg.getPayload());
	}

	@Test
	public void EventEmittingItemReadListenerBeforeRead() {
		eventEmittingItemReadListener.beforeRead();
		assertEquals(0, queueChannel.getQueueSize());
	}

	@Test
	public void EventEmittingItemReadListenerAfterRead() {
		eventEmittingItemReadListener.afterRead("HELLO_AFTER_READ");
		assertEquals(0, queueChannel.getQueueSize());
	}

	@Test
	public void EventEmittingItemWriteListenerBeforeWrite() {
		eventEmittingItemWriteListener.beforeWrite(getSampleList());
		assertEquals(1, queueChannel.getQueueSize());
		Message msg = queueChannel.receive();
		assertEquals("3 items to be written.", msg.getPayload());
	}

	@Test
	public void EventEmittingItemWriteListenerAfterWrite() {
		eventEmittingItemWriteListener.afterWrite(getSampleList());
		assertEquals(1, queueChannel.getQueueSize());
		Message msg = queueChannel.receive();
		assertEquals("3 items have been written.", msg.getPayload());
	}

	@Test
	public void EventEmittingItemWriteListenerWriteError() {
		RuntimeException exeption = new RuntimeException("Text Exception");
		eventEmittingItemWriteListener.onWriteError(exeption, getSampleList());
		assertEquals(1, queueChannel.getQueueSize());
		Message msg = queueChannel.receive();
		assertEquals("Exception while 3 items are attempted to be written.", msg.getPayload());
	}


	@Test
	public void EventEmittingJobExecutionListenerBeforeJob() {
		JobExecution jobExecution = getJobExecution();
		eventEmittingJobExecutionListener.beforeJob(jobExecution);
		assertEquals(1, queueChannel.getQueueSize());
		Message msg = queueChannel.receive();
		JobExecutionEvent jobEvent = (JobExecutionEvent) msg.getPayload();
		assertEquals(jobExecution.getJobInstance().getJobName(),
				jobEvent.getJobInstance().getJobName());
	}

	@Test
	public void EventEmittingJobExecutionListenerAfterJob() {
		JobExecution jobExecution = getJobExecution();
		eventEmittingJobExecutionListener.afterJob(jobExecution);
		assertEquals(1, queueChannel.getQueueSize());
		Message msg = queueChannel.receive();
		JobExecutionEvent jobEvent = (JobExecutionEvent) msg.getPayload();
		assertEquals(jobExecution.getJobInstance().getJobName(),
				jobEvent.getJobInstance().getJobName());
	}

	@Test
	public void EventEmittingStepExecutionListenerBeforeStep() {
		final String STEP_MESSAGE = "BEFORE_STEP_MESSAGE";
		JobExecution jobExecution = getJobExecution();
		StepExecution stepExecution = new StepExecution(STEP_MESSAGE,jobExecution);
		eventEmittingStepExecutionListener.beforeStep(stepExecution);
		assertEquals(1, queueChannel.getQueueSize());
		Message msg = queueChannel.receive();
		StepExecutionEvent stepExecutionEvent = (StepExecutionEvent) msg.getPayload();
		assertEquals(STEP_MESSAGE,
				stepExecutionEvent.getStepName());
	}

	@Test
	public void EventEmittingStepExecutionListenerAfterStep() {
		final String STEP_MESSAGE = "AFTER_STEP_MESSAGE";
		JobExecution jobExecution = getJobExecution();
		StepExecution stepExecution = new StepExecution(STEP_MESSAGE,jobExecution);
		eventEmittingStepExecutionListener.afterStep(stepExecution);
		assertEquals(1, queueChannel.getQueueSize());
		Message msg = queueChannel.receive();
		StepExecutionEvent stepExecutionEvent = (StepExecutionEvent) msg.getPayload();
		assertEquals(STEP_MESSAGE,
				stepExecutionEvent.getStepName());
	}

	@Test
	public void EventEmittingChunkExecutionListenerBeforeChunk() {
		final String CHUNK_MESSAGE = "Before Chunk Processing";
		ChunkContext chunkContext = getChunkContext();
		eventEmittingChunkListener.beforeChunk(chunkContext);
		assertEquals(1,queueChannel.getQueueSize());
		Message msg = queueChannel.receive();
		assertEquals(CHUNK_MESSAGE,msg.getPayload());
	}

	@Test
	public void EventEmittingChunkExecutionListenerAfterChunk() {
		final String CHUNK_MESSAGE = "After Chunk Processing";
		ChunkContext chunkContext = getChunkContext();
		eventEmittingChunkListener.afterChunk(chunkContext);
		assertEquals(1,queueChannel.getQueueSize());
		Message msg = queueChannel.receive();
		assertEquals(CHUNK_MESSAGE,msg.getPayload());
	}

	@Test
	public void EventEmittingChunkExecutionListenerAfterChunkError() {
		ChunkContext chunkContext = getChunkContext();
		eventEmittingChunkListener.afterChunkError(chunkContext);
		assertEquals(0,queueChannel.getQueueSize());
	}

	private JobExecution getJobExecution() {
		final String JOB_NAME = UUID.randomUUID().toString();
		JobInstance jobInstance = new JobInstance(1L, JOB_NAME);
		return new JobExecution(jobInstance, 1L,
				new JobParameters(), UUID.randomUUID().toString());
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
		StepExecution stepExecution = new StepExecution("STEP1",jobExecution);
		StepContext stepContext = new StepContext(stepExecution);
		ChunkContext chunkContext = new ChunkContext(stepContext);
		return chunkContext;
	}

}
