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

package org.springframework.cloud.task.repository.support;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.dao.MapTaskExecutionDao;
import org.springframework.cloud.task.util.TaskExecutionCreator;
import org.springframework.cloud.task.util.TestVerifierUtils;

import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Tests for the SimpleTaskRepository that uses Map as a datastore.
 *
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */
public class SimpleTaskRepositoryMapTests {

	private TaskRepository taskRepository;

	@Before
	public void setUp() {
		this.taskRepository = new SimpleTaskRepository(new TaskExecutionDaoFactoryBean());
	}

	@Test
	public void testCreateEmptyExecution() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
				.createAndStoreEmptyTaskExecution(this.taskRepository);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				getSingleTaskExecutionFromMapRepository(
						expectedTaskExecution.getExecutionId()));
	}

	@Test
	public void testCreateTaskExecutionNoParam() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
				.createAndStoreTaskExecutionNoParams(this.taskRepository);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				getSingleTaskExecutionFromMapRepository(
						expectedTaskExecution.getExecutionId()));
	}

	@Test
	public void testUpdateExternalExecutionId() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
				.createAndStoreTaskExecutionNoParams(this.taskRepository);
		expectedTaskExecution.setExternalExecutionId(UUID.randomUUID().toString());
		this.taskRepository.updateExternalExecutionId(
				expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getExternalExecutionId());
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				getSingleTaskExecutionFromMapRepository(
						expectedTaskExecution.getExecutionId()));
	}

	@Test
	public void testUpdateNullExternalExecutionId() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
				.createAndStoreTaskExecutionNoParams(this.taskRepository);
		expectedTaskExecution.setExternalExecutionId(null);
		this.taskRepository.updateExternalExecutionId(
				expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getExternalExecutionId());
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				getSingleTaskExecutionFromMapRepository(
						expectedTaskExecution.getExecutionId()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidExecutionIdForExternalExecutionIdUpdate() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
				.createAndStoreTaskExecutionNoParams(this.taskRepository);
		expectedTaskExecution.setExternalExecutionId(null);
		this.taskRepository.updateExternalExecutionId(-1,
				expectedTaskExecution.getExternalExecutionId());
	}

	@Test
	public void testCreateTaskExecutionWithParam() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
				.createAndStoreTaskExecutionWithParams(this.taskRepository);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				getSingleTaskExecutionFromMapRepository(
						expectedTaskExecution.getExecutionId()));
	}

	@Test
	public void startTaskExecutionWithParam() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
				.createAndStoreEmptyTaskExecution(this.taskRepository);

		expectedTaskExecution.setArguments(
				Collections.singletonList("foo=" + UUID.randomUUID().toString()));
		expectedTaskExecution.setStartTime(new Date());
		expectedTaskExecution.setTaskName(UUID.randomUUID().toString());

		TaskExecution actualTaskExecution = this.taskRepository.startTaskExecution(
				expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getTaskName(), expectedTaskExecution.getStartTime(),
				expectedTaskExecution.getArguments(),
				expectedTaskExecution.getExternalExecutionId(),
				expectedTaskExecution.getParentExecutionId());

		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	public void startTaskExecutionWithNoParam() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
				.createAndStoreEmptyTaskExecution(this.taskRepository);

		expectedTaskExecution.setStartTime(new Date());
		expectedTaskExecution.setTaskName(UUID.randomUUID().toString());

		TaskExecution actualTaskExecution = this.taskRepository.startTaskExecution(
				expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getTaskName(), expectedTaskExecution.getStartTime(),
				expectedTaskExecution.getArguments(),
				expectedTaskExecution.getExternalExecutionId());

		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	public void startTaskExecutionWithParent() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
				.createAndStoreEmptyTaskExecution(this.taskRepository);

		expectedTaskExecution.setStartTime(new Date());
		expectedTaskExecution.setTaskName(UUID.randomUUID().toString());
		expectedTaskExecution.setParentExecutionId(12345L);

		TaskExecution actualTaskExecution = this.taskRepository.startTaskExecution(
				expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getTaskName(), expectedTaskExecution.getStartTime(),
				expectedTaskExecution.getArguments(),
				expectedTaskExecution.getExternalExecutionId());

		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	public void testCompleteTaskExecution() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
				.createAndStoreTaskExecutionNoParams(this.taskRepository);
		expectedTaskExecution.setEndTime(new Date());
		expectedTaskExecution.setExitCode(0);
		TaskExecution actualTaskExecution = TaskExecutionCreator
				.completeExecution(this.taskRepository, expectedTaskExecution);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	private TaskExecution getSingleTaskExecutionFromMapRepository(long taskExecutionId) {
		Map<Long, TaskExecution> taskMap = ((MapTaskExecutionDao) ((SimpleTaskRepository) this.taskRepository)
				.getTaskExecutionDao()).getTaskExecutions();
		assertTrue("taskExecutionId must be in MapTaskExecutionRepository",
				taskMap.containsKey(taskExecutionId));
		return taskMap.get(taskExecutionId);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateTaskExecutionNullEndTime() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
				.createAndStoreTaskExecutionNoParams(this.taskRepository);
		expectedTaskExecution.setExitCode(-1);
		TaskExecutionCreator.completeExecution(this.taskRepository,
				expectedTaskExecution);
	}

}
