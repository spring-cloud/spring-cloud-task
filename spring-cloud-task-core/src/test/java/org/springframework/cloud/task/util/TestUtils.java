/*
 * Copyright 2015 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.mockito.ArgumentMatcher;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Offers utils to test the log results produced by the code being tested.
 *
 * @author Glenn Renfro
 */
public class TestUtils {

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
	 * Creates a fully populated TaskExecution (except params) for testing.
	 *
	 * @return
	 */
	public static TaskExecution createSampleTaskExecutionNoParam() {
		Random randomGenerator = new Random();
		int exitCode = randomGenerator.nextInt();
		Date startTime = new Date();
		Date endTime = new Date();
		String executionId = UUID.randomUUID().toString();
		String taskName = UUID.randomUUID().toString();
		String exitMessage = UUID.randomUUID().toString();
		String statusCode = UUID.randomUUID().toString().substring(0, 9);

		return new TaskExecution(executionId, exitCode, taskName,
				startTime, endTime, statusCode,
				exitMessage, new ArrayList<String>());
	}

	/**
	 * Verifies that all the fields in between the expected and actual are the same;
	 * @param expectedTaskExecution The expected value for the task execution.
	 * @param actualTaskExecution The actual value for the task execution.
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
		assertEquals("statusCode must be equal",
				expectedTaskExecution.getStatusCode(),
				actualTaskExecution.getStatusCode());
		if (expectedTaskExecution.getParameters() != null) {
			assertNotNull("parameters should not be null",
					actualTaskExecution.getParameters());
			assertEquals("parameters result set count should match expected count",
					expectedTaskExecution.getParameters().size(),
					actualTaskExecution.getParameters().size());
		}
		else {
			assertNull("parameters should be null", actualTaskExecution.getParameters());
		}
		Set<String> params = new HashSet<String>();
		for (String param : expectedTaskExecution.getParameters()) {
			params.add(param);
		}
		for (String param : actualTaskExecution.getParameters()) {
			assertTrue("param must exist in the repository", params.contains(param));
		}
	}

	/**
	 * Creates a sample TaskExecution and stores it in the taskRepository.
	 * @param taskRepository the taskRepository where the taskExecution should be stored.
	 * @return the taskExecution created.
	 */
	public static TaskExecution createAndStoreTaskExecutionNoParams(TaskRepository taskRepository){
		TaskExecution expectedTaskExecution = TestUtils.createSampleTaskExecutionNoParam();
		taskRepository.createTaskExecution(expectedTaskExecution);
		return expectedTaskExecution;
	}

	/**
	 * Creates a sample TaskExecution and stores it in the taskRepository with params.
	 * @param taskRepository the taskRepository where the taskExecution should be stored.
	 * @return the taskExecution created.
	 */
	public static TaskExecution createAndStoreTaskExecutionWithParams(TaskRepository taskRepository){
		TaskExecution expectedTaskExecution = TestUtils.createSampleTaskExecutionNoParam();
		List<String> params = new ArrayList<String>();
		params.add(UUID.randomUUID().toString());
		params.add(UUID.randomUUID().toString());
		expectedTaskExecution.setParameters(params);
		taskRepository.createTaskExecution(expectedTaskExecution);
		return expectedTaskExecution;
	}

	/**
	 * Updates a sample TaskExecution in the taskRepository.
	 * @param taskRepository the taskRepository where the taskExecution should be updated.
	 * @return the taskExecution created.
	 */
	public static TaskExecution updateTaskExecution(TaskRepository taskRepository,
																	String taskExecutionId){
		TaskExecution expectedTaskExecution = TestUtils.createSampleTaskExecutionNoParam();
		expectedTaskExecution.setExecutionId(taskExecutionId);
		taskRepository.update(expectedTaskExecution);
		return expectedTaskExecution;
	}

	/**
	 * Retrieves the TaskExecution from the datasource
	 * @param dataSource The datasource from which to retrieve the taskExecution
	 * @param taskExecutionId The id of the task to search .
	 * @return taskExecution
	 */
	public static TaskExecution getTaskExecutionFromDB(DataSource dataSource,
													   String taskExecutionId){
		String sql = "SELECT * FROM TASK_EXECUTION WHERE TASK_EXECUTION_ID = '"
				+ taskExecutionId + "'";

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
		assertEquals("only one row should be returned", 1, rows.size());
		TaskExecution taskExecution = new TaskExecution();
		for (Map row : rows) {
			taskExecution.setExecutionId((String)row.get("TASK_EXECUTION_ID"));
			taskExecution.setStartTime((Date) row.get("START_TIME"));
			taskExecution.setEndTime((Date) row.get("END_TIME"));
			taskExecution.setExitCode((Integer)row.get("EXIT_CODE"));
			taskExecution.setExitMessage((String)row.get("EXIT_MESSAGE"));
			taskExecution.setStatusCode((String)row.get("STATUS_CODE"));
			taskExecution.setTaskName((String)row.get("TASK_NAME"));
		}
		populateParams(dataSource, taskExecution);
		return taskExecution;
	}
	private static void populateParams(DataSource dataSource, TaskExecution taskExecution){
		String sql = "SELECT * FROM TASK_EXECUTION_PARAMS WHERE TASK_EXECUTION_ID = '"
				+ taskExecution.getExecutionId() + "'";

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
		List<String> params = new ArrayList<>();
		for (Map row : rows) {
			params.add((String) row.get("TASK_PARAM"));
		}
		taskExecution.setParameters(params);
	}

}
