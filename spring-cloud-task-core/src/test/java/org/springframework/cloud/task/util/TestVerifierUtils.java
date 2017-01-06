/*
 * Copyright 2015-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.mockito.ArgumentMatcher;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.task.repository.TaskExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Offers utils to test the  results produced by the code being tested.
 *
 * @author Glenn Renfro
 */
public class TestVerifierUtils {

	public static final int ARG_SIZE = 5;

	/**
	 * Creates a mock {@link Appender} to be added to the root logger.
	 *
	 * @return reference to the mock appender.
	 */
	public static Appender getMockAppender() {
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		final Appender mockAppender = mock(Appender.class);
		when(mockAppender.getName()).thenReturn("MOCK");
		root.addAppender(mockAppender);
		return mockAppender;
	}

	/**
	 * Verifies that the log sample is contained within the content that was written
	 * to the mock appender.
	 *
	 * @param mockAppender The appender that is associated with the test.
	 * @param logSample    The string to search for in the log entry.
	 */
	public static void verifyLogEntryExists(Appender mockAppender, final String logSample) {
		verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
			@Override
			public boolean matches(final Object argument) {
				return ((LoggingEvent) argument).getFormattedMessage().contains(logSample);
			}
		}));
	}

	/**
	 * Creates a fully populated TaskExecution (except args) for testing.
	 *
	 * @return
	 */
	public static TaskExecution createSampleTaskExecutionNoArg() {
		Random randomGenerator = new Random();
		Date startTime = new Date();
		long executionId = randomGenerator.nextLong();
		String taskName = UUID.randomUUID().toString();

		return new TaskExecution(executionId, 0, taskName,
				startTime, null, null, new ArrayList<String>(), null, null);
	}

	/**
	 * Creates a fully populated TaskExecution (except args) for testing.
	 *
	 * @return
	 */
	public static TaskExecution endSampleTaskExecutionNoArg() {
		Random randomGenerator = new Random();
		int exitCode = randomGenerator.nextInt();
		Date startTime = new Date();
		Date endTime = new Date();
		long executionId = randomGenerator.nextLong();
		String taskName = UUID.randomUUID().toString();
		String exitMessage = UUID.randomUUID().toString();

		return new TaskExecution(executionId, exitCode, taskName,
				startTime, endTime, exitMessage, new ArrayList<String>(), null, null);
	}

	/**
	 * Creates a fully populated TaskExecution for testing.
	 *
	 * @return
	 */
	public static TaskExecution createSampleTaskExecution(long executionId) {
		Date startTime = new Date();
		String taskName = UUID.randomUUID().toString();
		String externalExecutionId = UUID.randomUUID().toString();
		List<String> args = new ArrayList<>(ARG_SIZE);
		for (int i = 0; i < ARG_SIZE; i++){
			args.add(UUID.randomUUID().toString());
		}
		return new TaskExecution(executionId, null, taskName,
				startTime, null, null, args, null, externalExecutionId);
	}

	/**
	 * Verifies that all the fields in between the expected and actual are the same;
	 *
	 * @param expectedTaskExecution The expected value for the task execution.
	 * @param actualTaskExecution   The actual value for the task execution.
	 */
	public static void verifyTaskExecution(TaskExecution expectedTaskExecution,
										   TaskExecution actualTaskExecution) {
		assertEquals("taskExecutionId must be equal", expectedTaskExecution.getExecutionId(),
				actualTaskExecution.getExecutionId());
		assertEquals("startTime must be equal",
				expectedTaskExecution.getStartTime(),
				actualTaskExecution.getStartTime());
		assertEquals("endTime must be equal",
				expectedTaskExecution.getEndTime(),
				actualTaskExecution.getEndTime());
		assertEquals("exitCode must be equal",
				expectedTaskExecution.getExitCode(),
				actualTaskExecution.getExitCode());
		assertEquals("taskName must be equal",
				expectedTaskExecution.getTaskName(),
				actualTaskExecution.getTaskName());
		assertEquals("exitMessage must be equal",
				expectedTaskExecution.getExitMessage(),
				actualTaskExecution.getExitMessage());
		assertEquals("errorMessage must be equal",
				expectedTaskExecution.getErrorMessage(),
				actualTaskExecution.getErrorMessage());
		assertEquals("externalExecutionId must be equal",
				expectedTaskExecution.getExternalExecutionId(),
				actualTaskExecution.getExternalExecutionId());
		assertEquals("parentExecutionId must be equal",
				expectedTaskExecution.getParentExecutionId(),
				actualTaskExecution.getParentExecutionId());

		if (expectedTaskExecution.getArguments() != null) {
			assertNotNull("arguments should not be null",
					actualTaskExecution.getArguments());
			assertEquals("arguments result set count should match expected count",
					expectedTaskExecution.getArguments().size(),
					actualTaskExecution.getArguments().size());
		}
		else {
			assertNull("arguments should be null", actualTaskExecution.getArguments());
		}
		Set<String> args = new HashSet<String>();
		for (String param : expectedTaskExecution.getArguments()) {
			args.add(param);
		}
		for (String arg : actualTaskExecution.getArguments()) {
			assertTrue("arg must exist in the repository", args.contains(arg));
		}
	}


}
