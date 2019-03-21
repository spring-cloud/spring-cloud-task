/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.task.repository.dao;

import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.Test;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.util.TestVerifierUtils;

/**
 * Executes unit tests on MapTaskExecutionDaoTests.
 * @author Glenn Renfro
 */
public class MapTaskExecutionDaoTests {


	@Test
	public void saveTaskExecution(){
		MapTaskExecutionDao dao = new MapTaskExecutionDao();
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoArg();
		expectedTaskExecution = dao.createTaskExecution(expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments());
		Map<Long, TaskExecution> taskExecutionMap = dao.getTaskExecutions();
		assertNotNull("taskExecutionMap must not be null", taskExecutionMap);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				taskExecutionMap.get(expectedTaskExecution.getExecutionId()));
	}

	@Test
	public void completeTaskExecution(){
		MapTaskExecutionDao dao = new MapTaskExecutionDao();
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoArg();
		expectedTaskExecution = dao.createTaskExecution(expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments());
		dao.completeTaskExecution(expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getExitCode(), expectedTaskExecution.getEndTime(),
				expectedTaskExecution.getExitMessage());
		Map<Long, TaskExecution> taskExecutionMap = dao.getTaskExecutions();
		assertNotNull("taskExecutionMap must not be null", taskExecutionMap);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				taskExecutionMap.get(expectedTaskExecution.getExecutionId()));
	}

}
