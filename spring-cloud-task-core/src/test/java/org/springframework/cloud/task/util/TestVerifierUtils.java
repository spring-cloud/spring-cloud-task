/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.cloud.task.util;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Offers utils to test the results produced by the code being tested.
 *
 * @author Glenn Renfro
 */
public final class TestVerifierUtils {

	/**
	 * The default number of arguments to use for the test TaskExecutions.
	 */
	public static final int ARG_SIZE = 5;

	private TestVerifierUtils() {
	}

	/**
	 * Creates a mock {@link Appender} to be added to the root logger.
	 * @return reference to the mock appender.
	 */
	public static Appender getMockAppender() {
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		final Appender mockAppender = mock(Appender.class);
		when(mockAppender.getName()).thenReturn("MOCK");
		root.addAppender(mockAppender);
		return mockAppender;
	}

	/**
	 * Verifies that the log sample is contained within the content that was written to
	 * the mock appender.
	 * @param mockAppender The appender that is associated with the test.
	 * @param logSample The string to search for in the log entry.
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
	 * @return instance of a TaskExecution
	 */
	public static TaskExecution createSampleTaskExecutionNoArg() {
		Random randomGenerator = new Random();
		long executionId = randomGenerator.nextLong();
		String taskName = UUID.randomUUID().toString();

		return new TaskExecution(executionId, null, taskName, LocalDateTime.now(), null, null, new ArrayList<>(), null,
				null);
	}

	/**
	 * Creates a fully populated TaskExecution (except args) for testing.
	 * @return instance of a TaskExecution.
	 */
	public static TaskExecution endSampleTaskExecutionNoArg() {
		Random randomGenerator = new Random();
		int exitCode = randomGenerator.nextInt();
		long executionId = randomGenerator.nextLong();
		String taskName = UUID.randomUUID().toString();
		String exitMessage = UUID.randomUUID().toString();

		return new TaskExecution(executionId, exitCode, taskName, LocalDateTime.now(), LocalDateTime.now(), exitMessage,
				new ArrayList<>(), null, null);
	}

	/**
	 * Creates a fully populated TaskExecution for testing.
	 * @param executionId the taskExecutionId to be created.
	 * @return instance of the TaskExecution.
	 */
	public static TaskExecution createSampleTaskExecution(long executionId) {
		String taskName = UUID.randomUUID().toString();
		String externalExecutionId = UUID.randomUUID().toString();
		List<String> args = new ArrayList<>(ARG_SIZE);
		for (int i = 0; i < ARG_SIZE; i++) {
			args.add(UUID.randomUUID().toString());
		}
		return new TaskExecution(executionId, null, taskName, LocalDateTime.now(), null, null, args, null,
				externalExecutionId);
	}

	/**
	 * Verifies that all the fields in between the expected and actual are the same.
	 * @param expectedTaskExecution The expected value for the task execution.
	 * @param actualTaskExecution The actual value for the task execution.
	 */
	public static void verifyTaskExecution(TaskExecution expectedTaskExecution, TaskExecution actualTaskExecution) {
		assertThat(actualTaskExecution.getExecutionId()).as("taskExecutionId must be equal")
				.isEqualTo(expectedTaskExecution.getExecutionId());
		if (actualTaskExecution.getStartTime() != null) {
			assertThat(actualTaskExecution.getStartTime().getHour() == expectedTaskExecution.getStartTime().getHour())
					.as("startTime hour must be equal").isTrue();
			assertThat(
					actualTaskExecution.getStartTime().getMinute() == expectedTaskExecution.getStartTime().getMinute())
							.as("startTime minute must be equal").isTrue();
			assertThat(
					actualTaskExecution.getStartTime().getSecond() == expectedTaskExecution.getStartTime().getSecond())
							.as("startTime second must be equal").isTrue();
			assertThat(actualTaskExecution.getStartTime().getDayOfYear() == expectedTaskExecution.getStartTime()
					.getDayOfYear()).as("startTime day must be equal").isTrue();
			assertThat(actualTaskExecution.getStartTime().getYear() == expectedTaskExecution.getStartTime().getYear())
					.as("startTime year must be equal").isTrue();
			assertThat(actualTaskExecution.getStartTime().getMonthValue() == expectedTaskExecution.getStartTime()
					.getMonthValue()).as("startTime month must be equal").isTrue();
		}
		if (actualTaskExecution.getEndTime() != null) {
			assertThat(actualTaskExecution.getEndTime().getHour() == expectedTaskExecution.getEndTime().getHour())
					.as("endTime hour must be equal").isTrue();
			assertThat(actualTaskExecution.getEndTime().getMinute() == expectedTaskExecution.getEndTime().getMinute())
					.as("endTime minute must be equal").isTrue();
			assertThat(actualTaskExecution.getEndTime().getSecond() == expectedTaskExecution.getEndTime().getSecond())
					.as("endTime second must be equal").isTrue();
			assertThat(actualTaskExecution.getEndTime().getDayOfYear() == expectedTaskExecution.getEndTime()
					.getDayOfYear()).as("endTime day must be equal").isTrue();
			assertThat(actualTaskExecution.getEndTime().getYear() == expectedTaskExecution.getEndTime().getYear())
					.as("endTime year must be equal").isTrue();
			assertThat(actualTaskExecution.getEndTime().getMonthValue() == expectedTaskExecution.getEndTime()
					.getMonthValue()).as("endTime month must be equal").isTrue();
		}
		assertThat(actualTaskExecution.getExitCode()).as("exitCode must be equal")
				.isEqualTo(expectedTaskExecution.getExitCode());
		assertThat(actualTaskExecution.getTaskName()).as("taskName must be equal")
				.isEqualTo(expectedTaskExecution.getTaskName());
		assertThat(actualTaskExecution.getExitMessage()).as("exitMessage must be equal")
				.isEqualTo(expectedTaskExecution.getExitMessage());
		assertThat(actualTaskExecution.getErrorMessage()).as("errorMessage must be equal")
				.isEqualTo(expectedTaskExecution.getErrorMessage());
		assertThat(actualTaskExecution.getExternalExecutionId()).as("externalExecutionId must be equal")
				.isEqualTo(expectedTaskExecution.getExternalExecutionId());
		assertThat(actualTaskExecution.getParentExecutionId()).as("parentExecutionId must be equal")
				.isEqualTo(expectedTaskExecution.getParentExecutionId());

		if (expectedTaskExecution.getArguments() != null) {
			assertThat(actualTaskExecution.getArguments()).as("arguments should not be null").isNotNull();
			assertThat(actualTaskExecution.getArguments().size())
					.as("arguments result set count should match expected count")
					.isEqualTo(expectedTaskExecution.getArguments().size());
		}
		else {
			assertThat(actualTaskExecution.getArguments()).as("arguments should be null").isNull();
		}
		Set<String> args = new HashSet<>();
		for (String param : expectedTaskExecution.getArguments()) {
			args.add(param);
		}
		for (String arg : actualTaskExecution.getArguments()) {
			assertThat(args.contains(arg)).as("arg must exist in the repository").isTrue();
		}
	}

}
