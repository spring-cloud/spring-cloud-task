/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.task.repository.support;

import java.util.Date;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.configuration.SimpleTaskConfiguration;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.util.TaskExecutionCreator;
import org.springframework.cloud.task.util.TestDBUtils;
import org.springframework.cloud.task.util.TestVerifierUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests for the SimpleTaskRepository that uses JDBC as a datastore.
 *
 * @author Glenn Renfro.
 * @author Michael Minella
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {EmbeddedDataSourceConfiguration.class,
		SimpleTaskConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class})
public class SimpleTaskRepositoryJdbcTests {

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private DataSource dataSource;

	@Test
	@DirtiesContext
	public void testCreateTaskExecutionNoParam() {
		TaskExecution expectedTaskExecution =
				TaskExecutionCreator.createAndStoreTaskExecutionNoParams(taskRepository);
		TaskExecution actualTaskExecution = TestDBUtils.getTaskExecutionFromDB(dataSource,
				expectedTaskExecution.getExecutionId());
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	@DirtiesContext
	public void testCreateTaskExecutionWithParam() {
		TaskExecution expectedTaskExecution =
				TaskExecutionCreator.createAndStoreTaskExecutionWithParams(taskRepository);
		TaskExecution actualTaskExecution = TestDBUtils.getTaskExecutionFromDB(
				dataSource, expectedTaskExecution.getExecutionId());
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	@DirtiesContext
	public void testCompleteTaskExecution() {
		TaskExecution expectedTaskExecution =
				TaskExecutionCreator.createAndStoreTaskExecutionNoParams(taskRepository);
		expectedTaskExecution.setEndTime(new Date());
		expectedTaskExecution.setExitCode(77);
		expectedTaskExecution.setExitMessage(UUID.randomUUID().toString());

		TaskExecution actualTaskExecution = TaskExecutionCreator.completeExecution(taskRepository, expectedTaskExecution);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	@DirtiesContext
	public void testCreateTaskExecutionNoParamMaxExitMessageSize(){
		TaskExecution expectedTaskExecution = TaskExecutionCreator.createAndStoreTaskExecutionNoParams(taskRepository);
		expectedTaskExecution.setExitMessage(new String(new char[SimpleTaskRepository.MAX_EXIT_MESSAGE_SIZE+1]));
		expectedTaskExecution.setEndTime(new Date());
		taskRepository.completeTaskExecution(expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getExitCode(), new Date(),
				expectedTaskExecution.getExitMessage());
	}

	@Test(expected=IllegalArgumentException.class)
	@DirtiesContext
	public void testCreateTaskExecutionNoParamMaxTaskName(){
		taskRepository.createTaskExecution(
				new String(new char[SimpleTaskRepository.MAX_TASK_NAME_SIZE+1]),
				new Date(), null);
	}

	@Test(expected=IllegalArgumentException.class)
	@DirtiesContext
	public void testCreateTaskExecutionNegativeException(){
		TaskExecution expectedTaskExecution =
				TaskExecutionCreator.createAndStoreTaskExecutionNoParams(taskRepository);
		expectedTaskExecution.setEndTime(new Date());
		expectedTaskExecution.setExitCode(-1);

		TaskExecution actualTaskExecution = TaskExecutionCreator.completeExecution(taskRepository, expectedTaskExecution);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}


	@Test(expected=IllegalArgumentException.class)
	@DirtiesContext
	public void testCreateTaskExecutionNullEndTime(){
		TaskExecution expectedTaskExecution =
				TaskExecutionCreator.createAndStoreTaskExecutionNoParams(taskRepository);
		expectedTaskExecution.setExitCode(-1);
		TaskExecutionCreator.completeExecution(taskRepository, expectedTaskExecution);
	}
}

