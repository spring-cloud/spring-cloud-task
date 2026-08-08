/*
 * Copyright 2015-present the original author or authors.
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

/**
 * Tests for {@link MongoTaskExecutionDao}.
 *
 * @author JongJun Kim
 */
@Testcontainers
public class MongoTaskExecutionDaoTests extends BaseTaskExecutionDaoTestCases {

	private static final String DATABASE_NAME = "test-task-db";

	@Container
	static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0")
		.withExposedPorts(27017);

	private MongoClient mongoClient;
	private MongoOperations mongoOperations;
	private MongoTaskExecutionDao mongoDao;
	private TaskProperties taskProperties;

	@BeforeEach
	public void setup() {
		mongoClient = MongoClients.create(mongoDBContainer.getConnectionString());
		mongoOperations = new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, DATABASE_NAME));
		taskProperties = new TaskProperties();
		taskProperties.setTablePrefix("TASK_");
		mongoDao = new MongoTaskExecutionDao(mongoOperations, taskProperties);
		super.dao = mongoDao;

		// Clean up collections before each test
		cleanupCollections();
	}

	@AfterEach
	public void tearDown() {
		if (mongoClient != null) {
			cleanupCollections();
			mongoClient.close();
		}
	}

	private void cleanupCollections() {
		mongoOperations.getCollection("TASK_task_executions").drop();
		mongoOperations.getCollection("TASK_task_execution_parameters").drop();
		mongoOperations.getCollection("TASK_task_batch_associations").drop();
		mongoOperations.getCollection("TASK_task_sequence").drop();
	}

	@Test
	public void testCreateTaskExecution() {
		String taskName = "testTask";
		LocalDateTime startTime = LocalDateTime.now();
		List<String> arguments = Arrays.asList("arg1", "arg2", "arg3");
		String externalExecutionId = "ext123";

		TaskExecution taskExecution = mongoDao.createTaskExecution(taskName, startTime, arguments, externalExecutionId);

		assertThat(taskExecution).isNotNull();
		assertThat(taskExecution.getExecutionId()).isGreaterThan(0);
		assertThat(taskExecution.getTaskName()).isEqualTo(taskName);
		assertThat(taskExecution.getStartTime()).isEqualTo(startTime);
		assertThat(taskExecution.getArguments()).hasSize(3);
		assertThat(taskExecution.getArguments()).containsExactlyInAnyOrder("arg1", "arg2", "arg3");
		assertThat(taskExecution.getExternalExecutionId()).isEqualTo(externalExecutionId);
	}

	@Test
	public void testCreateTaskExecutionWithParentId() {
		String taskName = "parentTask";
		LocalDateTime startTime = LocalDateTime.now();
		List<String> arguments = Arrays.asList("arg1");
		String externalExecutionId = "ext123";
		Long parentExecutionId = 100L;

		TaskExecution taskExecution = mongoDao.createTaskExecution(taskName, startTime, arguments, externalExecutionId, parentExecutionId);

		assertThat(taskExecution).isNotNull();
		assertThat(taskExecution.getParentExecutionId()).isEqualTo(parentExecutionId);
		assertThat(taskExecution.getTaskName()).isEqualTo(taskName);
		assertThat(taskExecution.getExternalExecutionId()).isEqualTo(externalExecutionId);
	}

	@Test
	public void testCreateTaskExecutionWithNullArguments() {
		String taskName = "testTask";
		LocalDateTime startTime = LocalDateTime.now();
		String externalExecutionId = "ext123";

		TaskExecution taskExecution = mongoDao.createTaskExecution(taskName, startTime, null, externalExecutionId);

		assertThat(taskExecution).isNotNull();
		assertThat(taskExecution.getArguments()).isEmpty();
	}

	@Test
	public void testStartTaskExecution() {
		long executionId = mongoDao.getNextExecutionId();
		String taskName = "startTask";
		LocalDateTime startTime = LocalDateTime.now();
		List<String> arguments = Arrays.asList("startArg1", "startArg2");
		String externalExecutionId = "startExt123";

		TaskExecution taskExecution = mongoDao.startTaskExecution(executionId, taskName, startTime, arguments, externalExecutionId);

		assertThat(taskExecution).isNotNull();
		assertThat(taskExecution.getExecutionId()).isEqualTo(executionId);
		assertThat(taskExecution.getTaskName()).isEqualTo(taskName);
		assertThat(taskExecution.getArguments()).hasSize(2);
		assertThat(taskExecution.getExternalExecutionId()).isEqualTo(externalExecutionId);
	}

	@Test
	public void testCompleteTaskExecution() {
		TaskExecution taskExecution = mongoDao.createTaskExecution("completeTask", LocalDateTime.now(),
				Arrays.asList("arg1"), "ext123");

		Integer exitCode = 0;
		LocalDateTime endTime = LocalDateTime.now();
		String exitMessage = "Task completed successfully";
		String errorMessage = null;

		mongoDao.completeTaskExecution(taskExecution.getExecutionId(), exitCode, endTime, exitMessage, errorMessage);

		TaskExecution completedTask = mongoDao.getTaskExecution(taskExecution.getExecutionId());
		assertThat(completedTask.getExitCode()).isEqualTo(exitCode);
		assertThat(completedTask.getEndTime()).isCloseTo(endTime, within(1, ChronoUnit.SECONDS));
		assertThat(completedTask.getExitMessage()).isEqualTo(exitMessage);
		assertThat(completedTask.getErrorMessage()).isNull();
	}

	@Test
	public void testCompleteTaskExecutionWithError() {
		TaskExecution taskExecution = mongoDao.createTaskExecution("errorTask", LocalDateTime.now(),
				Arrays.asList("arg1"), "ext123");

		Integer exitCode = 1;
		LocalDateTime endTime = LocalDateTime.now();
		String exitMessage = "Task failed";
		String errorMessage = "Error occurred during execution";

		mongoDao.completeTaskExecution(taskExecution.getExecutionId(), exitCode, endTime, exitMessage, errorMessage);

		TaskExecution completedTask = mongoDao.getTaskExecution(taskExecution.getExecutionId());
		assertThat(completedTask.getExitCode()).isEqualTo(exitCode);
		assertThat(completedTask.getEndTime()).isCloseTo(endTime, within(1, ChronoUnit.SECONDS));
		assertThat(completedTask.getExitMessage()).isEqualTo(exitMessage);
		assertThat(completedTask.getErrorMessage()).isEqualTo(errorMessage);
	}

	@Test
	public void testGetTaskExecution() {
		TaskExecution original = mongoDao.createTaskExecution("getTask", LocalDateTime.now(),
				Arrays.asList("arg1", "arg2"), "ext123");

		TaskExecution retrieved = mongoDao.getTaskExecution(original.getExecutionId());

		assertThat(retrieved).isNotNull();
		assertThat(retrieved.getExecutionId()).isEqualTo(original.getExecutionId());
		assertThat(retrieved.getTaskName()).isEqualTo(original.getTaskName());
		assertThat(retrieved.getArguments()).hasSize(2);
		assertThat(retrieved.getExternalExecutionId()).isEqualTo(original.getExternalExecutionId());
	}

	@Test
	public void testGetTaskExecutionNonExistent() {
		TaskExecution retrieved = mongoDao.getTaskExecution(999L);
		assertThat(retrieved).isNull();
	}

	@Test
	public void testGetTaskExecutionCountByTaskName() {
		String taskName = "countTask";
		mongoDao.createTaskExecution(taskName, LocalDateTime.now(), Collections.emptyList(), "ext1");
		mongoDao.createTaskExecution(taskName, LocalDateTime.now(), Collections.emptyList(), "ext2");
		mongoDao.createTaskExecution("otherTask", LocalDateTime.now(), Collections.emptyList(), "ext3");

		long count = mongoDao.getTaskExecutionCountByTaskName(taskName);
		assertThat(count).isEqualTo(2);
	}

	@Test
	public void testGetRunningTaskExecutionCountByTaskName() {
		String taskName = "runningTask";
		TaskExecution task1 = mongoDao.createTaskExecution(taskName, LocalDateTime.now(), Collections.emptyList(), "ext1");
		TaskExecution task2 = mongoDao.createTaskExecution(taskName, LocalDateTime.now(), Collections.emptyList(), "ext2");
		mongoDao.createTaskExecution("otherTask", LocalDateTime.now(), Collections.emptyList(), "ext3");

		// Complete one task
		mongoDao.completeTaskExecution(task2.getExecutionId(), 0, LocalDateTime.now(), "completed");

		long runningCount = mongoDao.getRunningTaskExecutionCountByTaskName(taskName);
		assertThat(runningCount).isEqualTo(1);
	}

	@Test
	public void testGetRunningTaskExecutionCount() {
		TaskExecution task1 = mongoDao.createTaskExecution("task1", LocalDateTime.now(), Collections.emptyList(), "ext1");
		TaskExecution task2 = mongoDao.createTaskExecution("task2", LocalDateTime.now(), Collections.emptyList(), "ext2");
		TaskExecution task3 = mongoDao.createTaskExecution("task3", LocalDateTime.now(), Collections.emptyList(), "ext3");

		// Complete one task
		mongoDao.completeTaskExecution(task2.getExecutionId(), 0, LocalDateTime.now(), "completed");

		long runningCount = mongoDao.getRunningTaskExecutionCount();
		assertThat(runningCount).isEqualTo(2);
	}

	@Test
	public void testGetTaskExecutionCount() {
		mongoDao.createTaskExecution("task1", LocalDateTime.now(), Collections.emptyList(), "ext1");
		mongoDao.createTaskExecution("task2", LocalDateTime.now(), Collections.emptyList(), "ext2");
		mongoDao.createTaskExecution("task3", LocalDateTime.now(), Collections.emptyList(), "ext3");

		long totalCount = mongoDao.getTaskExecutionCount();
		assertThat(totalCount).isEqualTo(3);
	}

	@Test
	public void testFindRunningTaskExecutions() {
		String taskName = "runningFindTask";
		mongoDao.createTaskExecution(taskName, LocalDateTime.now().minusHours(2), Collections.emptyList(), "ext1");
		TaskExecution task2 = mongoDao.createTaskExecution(taskName, LocalDateTime.now().minusHours(1), Collections.emptyList(), "ext2");
		mongoDao.createTaskExecution(taskName, LocalDateTime.now(), Collections.emptyList(), "ext3");

		// Complete one task
		mongoDao.completeTaskExecution(task2.getExecutionId(), 0, LocalDateTime.now(), "completed");

		Pageable pageable = PageRequest.of(0, 10);
		Page<TaskExecution> runningTasks = mongoDao.findRunningTaskExecutions(taskName, pageable);

		assertThat(runningTasks.getTotalElements()).isEqualTo(2);
		assertThat(runningTasks.getContent()).hasSize(2);
	}

	@Test
	public void testFindTaskExecutionsByExternalExecutionId() {
		String externalId = "external123";
		mongoDao.createTaskExecution("task1", LocalDateTime.now(), Collections.emptyList(), externalId);
		mongoDao.createTaskExecution("task2", LocalDateTime.now(), Collections.emptyList(), externalId);
		mongoDao.createTaskExecution("task3", LocalDateTime.now(), Collections.emptyList(), "other");

		Pageable pageable = PageRequest.of(0, 10);
		Page<TaskExecution> tasks = mongoDao.findTaskExecutionsByExternalExecutionId(externalId, pageable);

		assertThat(tasks.getTotalElements()).isEqualTo(2);
		assertThat(tasks.getContent()).hasSize(2);
		tasks.getContent().forEach(task ->
			assertThat(task.getExternalExecutionId()).isEqualTo(externalId));
	}

	@Test
	public void testGetTaskExecutionCountByExternalExecutionId() {
		String externalId = "external123";
		mongoDao.createTaskExecution("task1", LocalDateTime.now(), Collections.emptyList(), externalId);
		mongoDao.createTaskExecution("task2", LocalDateTime.now(), Collections.emptyList(), externalId);
		mongoDao.createTaskExecution("task3", LocalDateTime.now(), Collections.emptyList(), "other");

		long count = mongoDao.getTaskExecutionCountByExternalExecutionId(externalId);
		assertThat(count).isEqualTo(2);
	}

	@Test
	public void testFindTaskExecutionsByName() {
		String taskName = "findByNameTask";
		mongoDao.createTaskExecution(taskName, LocalDateTime.now(), Collections.emptyList(), "ext1");
		mongoDao.createTaskExecution(taskName, LocalDateTime.now(), Collections.emptyList(), "ext2");
		mongoDao.createTaskExecution("otherTask", LocalDateTime.now(), Collections.emptyList(), "ext3");

		Pageable pageable = PageRequest.of(0, 10);
		Page<TaskExecution> tasks = mongoDao.findTaskExecutionsByName(taskName, pageable);

		assertThat(tasks.getTotalElements()).isEqualTo(2);
		assertThat(tasks.getContent()).hasSize(2);
		tasks.getContent().forEach(task ->
			assertThat(task.getTaskName()).isEqualTo(taskName));
	}

	@Test
	public void testGetTaskNames() {
		mongoDao.createTaskExecution("taskA", LocalDateTime.now(), Collections.emptyList(), "ext1");
		mongoDao.createTaskExecution("taskB", LocalDateTime.now(), Collections.emptyList(), "ext2");
		mongoDao.createTaskExecution("taskA", LocalDateTime.now(), Collections.emptyList(), "ext3");

		List<String> taskNames = mongoDao.getTaskNames();
		assertThat(taskNames).hasSize(2);
		assertThat(taskNames).containsExactlyInAnyOrder("taskA", "taskB");
	}

	@Test
	public void testFindAllPaginated() {
		mongoDao.createTaskExecution("task1", LocalDateTime.now(), Collections.emptyList(), "ext1");
		mongoDao.createTaskExecution("task2", LocalDateTime.now(), Collections.emptyList(), "ext2");
		mongoDao.createTaskExecution("task3", LocalDateTime.now(), Collections.emptyList(), "ext3");

		Pageable pageable = PageRequest.of(0, 2);
		Page<TaskExecution> tasks = mongoDao.findAll(pageable);

		assertThat(tasks.getTotalElements()).isEqualTo(3);
		assertThat(tasks.getContent()).hasSize(2);
		assertThat(tasks.getTotalPages()).isEqualTo(2);
	}

	@Test
	public void testGetNextExecutionId() {
		long id1 = mongoDao.getNextExecutionId();
		long id2 = mongoDao.getNextExecutionId();
		long id3 = mongoDao.getNextExecutionId();

		assertThat(id1).isGreaterThan(0);
		assertThat(id2).isGreaterThan(id1);
		assertThat(id3).isGreaterThan(id2);
	}

	@Test
	public void testTaskBatchAssociations() {
		long taskExecutionId = mongoDao.getNextExecutionId();
		long jobExecutionId1 = 100L;
		long jobExecutionId2 = 200L;

		// Create associations
		mongoDao.createTaskBatchAssociation(taskExecutionId, jobExecutionId1);
		mongoDao.createTaskBatchAssociation(taskExecutionId, jobExecutionId2);

		// Test getJobExecutionIdsByTaskExecutionId
		Set<Long> jobExecutionIds = mongoDao.getJobExecutionIdsByTaskExecutionId(taskExecutionId);
		assertThat(jobExecutionIds).hasSize(2);
		assertThat(jobExecutionIds).contains(jobExecutionId1, jobExecutionId2);

		// Test getTaskExecutionIdByJobExecutionId
		Long retrievedTaskExecutionId1 = mongoDao.getTaskExecutionIdByJobExecutionId(jobExecutionId1);
		assertThat(retrievedTaskExecutionId1).isEqualTo(taskExecutionId);

		Long retrievedTaskExecutionId2 = mongoDao.getTaskExecutionIdByJobExecutionId(jobExecutionId2);
		assertThat(retrievedTaskExecutionId2).isEqualTo(taskExecutionId);
	}

	@Test
	public void testDeleteTaskBatchAssociationsByTaskExecutionId() {
		long taskExecutionId = mongoDao.getNextExecutionId();
		long jobExecutionId1 = 100L;
		long jobExecutionId2 = 200L;

		mongoDao.createTaskBatchAssociation(taskExecutionId, jobExecutionId1);
		mongoDao.createTaskBatchAssociation(taskExecutionId, jobExecutionId2);

		mongoDao.deleteTaskBatchAssociationsByTaskExecutionId(taskExecutionId);

		Set<Long> jobExecutionIds = mongoDao.getJobExecutionIdsByTaskExecutionId(taskExecutionId);
		assertThat(jobExecutionIds).isEmpty();
	}

	@Test
	public void testDeleteTaskBatchAssociationsByJobExecutionId() {
		long taskExecutionId = mongoDao.getNextExecutionId();
		long jobExecutionId = 100L;

		mongoDao.createTaskBatchAssociation(taskExecutionId, jobExecutionId);

		mongoDao.deleteTaskBatchAssociationsByJobExecutionId(jobExecutionId);

		Long retrievedTaskExecutionId = mongoDao.getTaskExecutionIdByJobExecutionId(jobExecutionId);
		assertThat(retrievedTaskExecutionId).isNull();
	}

	@Test
	public void testUpdateExternalExecutionId() {
		TaskExecution taskExecution = mongoDao.createTaskExecution("updateTask", LocalDateTime.now(),
				Collections.emptyList(), "original");

		String newExternalId = "updated";
		mongoDao.updateExternalExecutionId(taskExecution.getExecutionId(), newExternalId);

		TaskExecution updatedTask = mongoDao.getTaskExecution(taskExecution.getExecutionId());
		assertThat(updatedTask.getExternalExecutionId()).isEqualTo(newExternalId);
	}

	@Test
	public void testGetLatestTaskExecutionForTaskName() {
		String taskName = "latestTask";
		mongoDao.createTaskExecution(taskName, LocalDateTime.now().minusHours(3), Collections.emptyList(), "ext1");
		TaskExecution latest = mongoDao.createTaskExecution(taskName, LocalDateTime.now().minusHours(1), Collections.emptyList(), "ext2");
		mongoDao.createTaskExecution(taskName, LocalDateTime.now().minusHours(2), Collections.emptyList(), "ext3");

		TaskExecution retrievedLatest = mongoDao.getLatestTaskExecutionForTaskName(taskName);
		assertThat(retrievedLatest).isNotNull();
		assertThat(retrievedLatest.getExecutionId()).isEqualTo(latest.getExecutionId());
		assertThat(retrievedLatest.getExternalExecutionId()).isEqualTo("ext2");
	}

	@Test
	public void testGetLatestTaskExecutionsByTaskNames() {
		String taskName1 = "latestTask1";
		String taskName2 = "latestTask2";

		mongoDao.createTaskExecution(taskName1, LocalDateTime.now().minusHours(2), Collections.emptyList(), "ext1");
		TaskExecution latest1 = mongoDao.createTaskExecution(taskName1, LocalDateTime.now().minusHours(1), Collections.emptyList(), "ext2");

		TaskExecution latest2 = mongoDao.createTaskExecution(taskName2, LocalDateTime.now().minusHours(1), Collections.emptyList(), "ext3");

		List<TaskExecution> latestExecutions = mongoDao.getLatestTaskExecutionsByTaskNames(taskName1, taskName2);
		assertThat(latestExecutions).hasSize(2);

		TaskExecution retrieved1 = latestExecutions.stream()
				.filter(te -> taskName1.equals(te.getTaskName()))
				.findFirst()
				.orElse(null);
		assertThat(retrieved1).isNotNull();
		assertThat(retrieved1.getExecutionId()).isEqualTo(latest1.getExecutionId());

		TaskExecution retrieved2 = latestExecutions.stream()
				.filter(te -> taskName2.equals(te.getTaskName()))
				.findFirst()
				.orElse(null);
		assertThat(retrieved2).isNotNull();
		assertThat(retrieved2.getExecutionId()).isEqualTo(latest2.getExecutionId());
	}

	@Test
	public void testConstructorWithInvalidObject() {
		assertThatThrownBy(() -> new MongoTaskExecutionDao("not a MongoOperations", taskProperties))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Provided object is not a MongoOperations instance");
	}

	@Test
	public void testConstructorWithNullMongoOperations() {
		assertThatThrownBy(() -> new MongoTaskExecutionDao((MongoOperations) null, taskProperties))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("mongoOperations must not be null");
	}

	@Test
	public void testConstructorWithNullTaskProperties() {
		assertThatThrownBy(() -> new MongoTaskExecutionDao(mongoOperations, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("taskProperties must not be null");
	}

	@Test
	public void testTablePrefixInCollectionNames() {
		TaskProperties customTaskProperties = new TaskProperties();
		customTaskProperties.setTablePrefix("CUSTOM_");
		MongoTaskExecutionDao customDao = new MongoTaskExecutionDao(mongoOperations, customTaskProperties);

		TaskExecution taskExecution = customDao.createTaskExecution("prefixTest", LocalDateTime.now(),
				Collections.emptyList(), "ext123");

		// Verify that collections are created with custom prefix
		boolean executionCollectionExists = mongoOperations.collectionExists("CUSTOM_task_executions");
		boolean paramsCollectionExists = mongoOperations.collectionExists("CUSTOM_task_execution_parameters");
		boolean sequenceCollectionExists = mongoOperations.collectionExists("CUSTOM_task_sequence");

		assertThat(executionCollectionExists).isTrue();
		assertThat(sequenceCollectionExists).isTrue();

		// Clean up custom collections
		mongoOperations.getCollection("CUSTOM_task_executions").drop();
		mongoOperations.getCollection("CUSTOM_task_execution_parameters").drop();
		mongoOperations.getCollection("CUSTOM_task_sequence").drop();
	}

	// Validation tests for improved input checking
	@Test
	public void testCreateTaskExecutionWithNullTaskName() {
		assertThatThrownBy(() -> mongoDao.createTaskExecution(null, LocalDateTime.now(),
				Collections.emptyList(), "ext123"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("taskName must not be null or empty");
	}

	@Test
	public void testCreateTaskExecutionWithEmptyTaskName() {
		assertThatThrownBy(() -> mongoDao.createTaskExecution("", LocalDateTime.now(),
				Collections.emptyList(), "ext123"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("taskName must not be null or empty");
	}

	@Test
	public void testCreateTaskExecutionWithNullStartTime() {
		assertThatThrownBy(() -> mongoDao.createTaskExecution("testTask", null,
				Collections.emptyList(), "ext123"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("startTime must not be null");
	}

	@Test
	public void testCompleteTaskExecutionWithNullEndTime() {
		TaskExecution task = mongoDao.createTaskExecution("testTask", LocalDateTime.now(),
				Collections.emptyList(), "ext123");

		assertThatThrownBy(() -> mongoDao.completeTaskExecution(task.getExecutionId(), 0, null, "completed"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("endTime must not be null");
	}

	@Test
	public void testCompleteTaskExecutionWithNonExistentId() {
		assertThatThrownBy(() -> mongoDao.completeTaskExecution(999L, 0, LocalDateTime.now(), "completed"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Invalid TaskExecution, ID 999 not found.");
	}

	@Test
	public void testUpdateExternalExecutionIdWithNonExistentId() {
		assertThatThrownBy(() -> mongoDao.updateExternalExecutionId(999L, "newExt"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Invalid TaskExecution, ID 999 not found.");
	}

	@Test
	public void testGetLatestTaskExecutionsByTaskNamesWithEmptyArray() {
		assertThatThrownBy(() -> mongoDao.getLatestTaskExecutionsByTaskNames())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("At least 1 task name must be provided.");
	}

	@Test
	public void testGetLatestTaskExecutionsByTaskNamesWithNullElements() {
		assertThatThrownBy(() -> mongoDao.getLatestTaskExecutionsByTaskNames("validTask", null, "anotherValidTask"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Task names must not contain any empty elements");
	}
}
