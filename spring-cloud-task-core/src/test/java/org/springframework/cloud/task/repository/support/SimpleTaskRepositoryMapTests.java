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

import static org.springframework.test.util.AssertionErrors.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.dao.MapTaskExecutionDao;
import org.springframework.cloud.task.util.TaskExecutionCreator;
import org.springframework.cloud.task.util.TestVerifierUtils;

/**
 * Tests for the SimpleTaskRepository that uses Map as a datastore.
 * @author Glenn Renfro.
 */
public class SimpleTaskRepositoryMapTests {

	private TaskRepository taskRepository;

	@Before
	public void setUp() {
		MapTaskRepositoryFactoryBean factoryBean =
				new MapTaskRepositoryFactoryBean();
		taskRepository = factoryBean.getObject();
	}

	@Test
	public void testCreateTaskExecutionNoParam() {
		TaskExecution expectedTaskExecution =
				TaskExecutionCreator.createAndStoreTaskExecutionNoParams(taskRepository);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				getSingleTaskExecutionFromMapRepository(taskRepository,
						expectedTaskExecution.getExecutionId()));
	}

	@Test
	public void testCreateTaskExecutionWithParam() {
		TaskExecution expectedTaskExecution =
				TaskExecutionCreator.createAndStoreTaskExecutionWithParams(taskRepository);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				getSingleTaskExecutionFromMapRepository(taskRepository,
						expectedTaskExecution.getExecutionId()));
	}

	@Test
	public void testUpdateTaskExecution() {
		TaskExecution expectedTaskExecution =
				TaskExecutionCreator.createAndStoreTaskExecutionNoParams(taskRepository);
		expectedTaskExecution = TaskExecutionCreator.updateTaskExecution(taskRepository,
				expectedTaskExecution.getExecutionId());
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				getSingleTaskExecutionFromMapRepository(taskRepository,
						expectedTaskExecution.getExecutionId()));
	}

	private TaskExecution getSingleTaskExecutionFromMapRepository(
			TaskRepository repository, long taskExecutionId){
		Map<Long, TaskExecution> taskMap = ((MapTaskExecutionDao)
				((SimpleTaskRepository)taskRepository).getTaskExecutionDao()).getTaskExecutions();
		assertTrue("taskExecutionId must be in MapTaskExecutionRepository",
				taskMap.containsKey(taskExecutionId));
		return taskMap.get(taskExecutionId);
	}
}
