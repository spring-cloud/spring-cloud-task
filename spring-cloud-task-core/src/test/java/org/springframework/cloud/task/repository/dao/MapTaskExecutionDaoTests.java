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

package org.springframework.cloud.task.repository.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.util.TestVerifierUtils;

import static org.junit.Assert.assertNotNull;

/**
 * Executes unit tests on MapTaskExecutionDaoTests.
 * @author Glenn Renfro
 */
public class MapTaskExecutionDaoTests {

	private MapTaskExecutionDao dao;

	@Before
	public void setUp() {
		this.dao = new MapTaskExecutionDao();
	}

	@Test
	public void testStartTaskExecution() {
		TaskExecution expectedTaskExecution = this.dao.createTaskExecution(null, null, new ArrayList<String>(0));

		expectedTaskExecution.setArguments(Collections.singletonList("foo=" + UUID.randomUUID().toString()));
		expectedTaskExecution.setStartTime(new Date());
		expectedTaskExecution.setTaskName(UUID.randomUUID().toString());

		this.dao.startTaskExecution(expectedTaskExecution.getExecutionId(), expectedTaskExecution.getTaskName(), expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments());
		Map<Long, TaskExecution> taskExecutionMap = this.dao.getTaskExecutions();
		assertNotNull("taskExecutionMap must not be null", taskExecutionMap);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				taskExecutionMap.get(expectedTaskExecution.getExecutionId()));
	}

	@Test
	public void createEmptyTaskExecution() {
		TaskExecution expectedTaskExecution = dao.createTaskExecution(null, null,
				new ArrayList<String>(0));

		Map<Long, TaskExecution> taskExecutionMap = this.dao.getTaskExecutions();
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				taskExecutionMap.get(expectedTaskExecution.getExecutionId()));
	}

	@Test(expected = IllegalStateException.class)
	public void completeTaskExecutionWithNoCreate() {
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoArg();
		this.dao.completeTaskExecution(expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getExitCode(), expectedTaskExecution.getEndTime(),
				expectedTaskExecution.getExitMessage());
	}

	@Test
	public void saveTaskExecution(){
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoArg();
		expectedTaskExecution = this.dao.createTaskExecution(expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments());
		Map<Long, TaskExecution> taskExecutionMap = this.dao.getTaskExecutions();
		assertNotNull("taskExecutionMap must not be null", taskExecutionMap);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				taskExecutionMap.get(expectedTaskExecution.getExecutionId()));
	}

	@Test
	public void completeTaskExecution(){
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoArg();
		expectedTaskExecution = this.dao.createTaskExecution(expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments());
		this.dao.completeTaskExecution(expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getExitCode(), expectedTaskExecution.getEndTime(),
				expectedTaskExecution.getExitMessage());
		Map<Long, TaskExecution> taskExecutionMap = this.dao.getTaskExecutions();
		assertNotNull("taskExecutionMap must not be null", taskExecutionMap);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				taskExecutionMap.get(expectedTaskExecution.getExecutionId()));
	}

}
