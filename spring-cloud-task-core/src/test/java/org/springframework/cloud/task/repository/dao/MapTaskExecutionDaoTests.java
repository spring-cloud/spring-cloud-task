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

package org.springframework.cloud.task.repository.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.util.TestVerifierUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
		TaskExecution expectedTaskExecution = this.dao.createTaskExecution(null, null, new ArrayList<String>(0), null);

		expectedTaskExecution.setArguments(Collections.singletonList("foo=" + UUID.randomUUID().toString()));
		expectedTaskExecution.setStartTime(new Date());
		expectedTaskExecution.setTaskName(UUID.randomUUID().toString());

		this.dao.startTaskExecution(expectedTaskExecution.getExecutionId(), expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
				expectedTaskExecution.getExternalExecutionId());
		Map<Long, TaskExecution> taskExecutionMap = this.dao.getTaskExecutions();
		assertNotNull("taskExecutionMap must not be null", taskExecutionMap);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				taskExecutionMap.get(expectedTaskExecution.getExecutionId()));
	}

	@Test
	public void createEmptyTaskExecution() {
		TaskExecution expectedTaskExecution = dao.createTaskExecution(null, null,
				new ArrayList<String>(0), null);

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
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
				expectedTaskExecution.getExternalExecutionId());
		Map<Long, TaskExecution> taskExecutionMap = this.dao.getTaskExecutions();
		assertNotNull("taskExecutionMap must not be null", taskExecutionMap);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				taskExecutionMap.get(expectedTaskExecution.getExecutionId()));
	}

	@Test
	public void completeTaskExecution(){
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoArg();
		expectedTaskExecution = this.dao.createTaskExecution(expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
				expectedTaskExecution.getExternalExecutionId());
		this.dao.completeTaskExecution(expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getExitCode(), expectedTaskExecution.getEndTime(),
				expectedTaskExecution.getExitMessage());
		Map<Long, TaskExecution> taskExecutionMap = this.dao.getTaskExecutions();
		assertNotNull("taskExecutionMap must not be null", taskExecutionMap);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				taskExecutionMap.get(expectedTaskExecution.getExecutionId()));
	}

	@Test
	public void testJobQueries(){
		List<TaskExecution> expectedTaskExecutionList = new ArrayList<>(2);
		expectedTaskExecutionList.add(TestVerifierUtils.createSampleTaskExecutionNoArg());
		expectedTaskExecutionList.add(TestVerifierUtils.createSampleTaskExecutionNoArg());

		for (TaskExecution expectedTaskExecution : expectedTaskExecutionList) {
			expectedTaskExecution = this.dao.createTaskExecution(expectedTaskExecution.getTaskName(),
					expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
					expectedTaskExecution.getExternalExecutionId());
			this.dao.completeTaskExecution(expectedTaskExecution.getExecutionId(),
					expectedTaskExecution.getExitCode(), expectedTaskExecution.getEndTime(),
					expectedTaskExecution.getExitMessage());
		}
		Set jobIds = new HashSet<Long>(2);
		jobIds.add(123L);
		jobIds.add(456L);
		this.dao.getBatchJobAssociations().put(
				expectedTaskExecutionList.get(0).getExecutionId(), jobIds);

		assertEquals(Long.valueOf(expectedTaskExecutionList.get(0).getExecutionId()),
				this.dao.getTaskExecutionIdByJobExecutionId(123L));
		assertEquals(Long.valueOf(expectedTaskExecutionList.get(0).getExecutionId()),
				this.dao.getTaskExecutionIdByJobExecutionId(456L));
		assertNull(this.dao.getTaskExecutionIdByJobExecutionId(789L));
	}

	@Test
	public void testStartExecutionWithNullExternalExecutionIdExisting(){
		TaskExecution expectedTaskExecution =
				initializeTaskExecutionWithExternalExecutionId();
		Map<Long, TaskExecution> taskExecutionMap = this.dao.getTaskExecutions();
		this.dao.startTaskExecution(expectedTaskExecution.getExecutionId(), expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
				null);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				taskExecutionMap.get(expectedTaskExecution.getExecutionId()));
	}

	@Test
	public void testStartExecutionWithNullExternalExecutionIdNonExisting(){
		TaskExecution expectedTaskExecution =
				initializeTaskExecutionWithExternalExecutionId();
		Map<Long, TaskExecution> taskExecutionMap = this.dao.getTaskExecutions();
		this.dao.startTaskExecution(expectedTaskExecution.getExecutionId(), expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
				"BAR");
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				taskExecutionMap.get(expectedTaskExecution.getExecutionId()));
	}

	private TaskExecution initializeTaskExecutionWithExternalExecutionId() {
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoArg();
		return this.dao.createTaskExecution(expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
				"FOO1");
	}
}
