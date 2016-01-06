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

package org.springframework.cloud.task.repository.support;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.configuration.SimpleTaskConfiguration;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.util.TaskExecutionCreator;
import org.springframework.cloud.task.util.TestDBUtils;
import org.springframework.cloud.task.util.TestVerifierUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Tests for the SimpleTaskRepository that uses JDBC as a datastore.
 *
 * @author Glenn Renfro.
 */
public class SimpleTaskRepositoryJdbcTests {

	private TaskRepository taskRepository;

	private DataSource dataSource;

	private AnnotationConfigApplicationContext context;

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(SimpleTaskConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		dataSource = this.context.getBean(DataSource.class);
		JdbcTaskRepositoryFactoryBean factoryBean =
				new JdbcTaskRepositoryFactoryBean(dataSource);
		taskRepository = factoryBean.getObject();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testCreateTaskExecutionNoParam() {
		TaskExecution expectedTaskExecution =
				TaskExecutionCreator.createAndStoreTaskExecutionNoParams(taskRepository);
		TaskExecution actualTaskExecution = TestDBUtils.getTaskExecutionFromDB(dataSource,
				expectedTaskExecution.getExecutionId());
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	public void testCreateTaskExecutionWithParam() {
		TaskExecution expectedTaskExecution =
				TaskExecutionCreator.createAndStoreTaskExecutionWithParams(taskRepository);
		TaskExecution actualTaskExecution = TestDBUtils.getTaskExecutionFromDB(
				dataSource, expectedTaskExecution.getExecutionId());
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	public void testUpdateTaskExecution() {
		TaskExecution expectedTaskExecution =
				TaskExecutionCreator.createAndStoreTaskExecutionNoParams(taskRepository);
		expectedTaskExecution = TaskExecutionCreator.updateTaskExecution(taskRepository,
				expectedTaskExecution.getExecutionId());
		TaskExecution actualTaskExecution = TestDBUtils.getTaskExecutionFromDB(
				dataSource, expectedTaskExecution.getExecutionId());
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	public void testCreateTaskExecutionNoParamMaxExitMessageSize(){
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoParam();
		expectedTaskExecution.setExitMessage(new String(new char[SimpleTaskRepository.MAX_EXIT_MESSAGE_SIZE+1]));
		taskRepository.createTaskExecution(expectedTaskExecution);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateTaskExecutionNoParamMaxTaskName(){
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoParam();
		expectedTaskExecution.setTaskName(new String(new char[SimpleTaskRepository.MAX_TASK_NAME_SIZE+1]));
		taskRepository.createTaskExecution(expectedTaskExecution);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateTaskExecutionNoParamMaxStatus(){
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoParam();
		expectedTaskExecution.setStatusCode(new String(new char[SimpleTaskRepository.MAX_STATUS_CODE_SIZE+1]));
		taskRepository.createTaskExecution(expectedTaskExecution);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateTaskExecutionNoParamMaxExternalExecutionId(){
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoParam();
		expectedTaskExecution.setExternalExecutionID(
				new String(new char[SimpleTaskRepository.MAX_EXTERNAL_EXECUTION_ID_SIZE+1]));
		taskRepository.createTaskExecution(expectedTaskExecution);
	}
}

