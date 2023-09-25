/*
 * Copyright 2015-2023 the original author or authors.
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.configuration.TestConfiguration;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.util.TestVerifierUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */
public class SimpleTaskExplorerTests {

	private final static String TASK_NAME = "FOOBAR";

	private final static String EXTERNAL_EXECUTION_ID = "123ABC";

	private AnnotationConfigApplicationContext context;

	@Autowired
	private TaskExplorer taskExplorer;

	@Autowired
	private TaskRepository taskRepository;

	public static Collection<Object> data() {
		return Arrays.asList(new Object[] { DaoType.jdbc, DaoType.map });
	}

	public void testDefaultContext(DaoType testType) {
		if (testType == DaoType.jdbc) {
			initializeJdbcExplorerTest();
		}
		else {
			initializeMapExplorerTest();
		}
	}

	@AfterEach
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@ParameterizedTest
	@MethodSource("data")
	public void getTaskExecution(DaoType testType) {
		testDefaultContext(testType);
		Map<Long, TaskExecution> expectedResults = createSampleDataSet(5);
		for (Long taskExecutionId : expectedResults.keySet()) {
			TaskExecution actualTaskExecution = this.taskExplorer.getTaskExecution(taskExecutionId);
			assertThat(actualTaskExecution)
				.as(String.format("expected a taskExecution but got null for test type %s", testType))
				.isNotNull();
			TestVerifierUtils.verifyTaskExecution(expectedResults.get(taskExecutionId), actualTaskExecution);
		}
	}

	@ParameterizedTest
	@MethodSource("data")
	public void taskExecutionNotFound(DaoType testType) {
		testDefaultContext(testType);
		createSampleDataSet(5);

		TaskExecution actualTaskExecution = this.taskExplorer.getTaskExecution(-5);
		assertThat(actualTaskExecution).as(String.format("expected null for actualTaskExecution %s", testType))
			.isNull();
	}

	@ParameterizedTest
	@MethodSource("data")
	public void getTaskCountByTaskName(DaoType testType) {
		testDefaultContext(testType);
		Map<Long, TaskExecution> expectedResults = createSampleDataSet(5);
		for (Map.Entry<Long, TaskExecution> entry : expectedResults.entrySet()) {
			String taskName = entry.getValue().getTaskName();
			assertThat(this.taskExplorer.getTaskExecutionCountByTaskName(taskName))
				.as(String.format("task count for task name did not match expected result for testType %s", testType))
				.isEqualTo(1);
		}
	}

	@ParameterizedTest
	@MethodSource("data")
	public void getTaskCount(DaoType testType) {
		testDefaultContext(testType);
		createSampleDataSet(33);
		assertThat(this.taskExplorer.getTaskExecutionCount())
			.as(String.format("task count did not match expected result for test Type %s", testType))
			.isEqualTo(33);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void getRunningTaskCount(DaoType testType) {
		testDefaultContext(testType);
		createSampleDataSet(33);
		assertThat(this.taskExplorer.getRunningTaskExecutionCount())
			.as(String.format("task count did not match expected result for test Type %s", testType))
			.isEqualTo(33);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void findRunningTasks(DaoType testType) {
		testDefaultContext(testType);
		final int TEST_COUNT = 2;
		final int COMPLETE_COUNT = 5;

		Map<Long, TaskExecution> expectedResults = new HashMap<>();
		// Store completed task executions
		int i = 0;
		for (; i < COMPLETE_COUNT; i++) {
			createAndSaveTaskExecution(i);
		}

		for (; i < (COMPLETE_COUNT + TEST_COUNT); i++) {
			TaskExecution expectedTaskExecution = this.taskRepository.createTaskExecution(getSimpleTaskExecution());
			expectedResults.put(expectedTaskExecution.getExecutionId(), expectedTaskExecution);
		}
		Pageable pageable = PageRequest.of(0, 10);

		Page<TaskExecution> actualResults = this.taskExplorer.findRunningTaskExecutions(TASK_NAME, pageable);
		assertThat(actualResults.getNumberOfElements())
			.as(String.format("Running task count for task name did not match expected result for testType %s",
					testType))
			.isEqualTo(TEST_COUNT);

		for (TaskExecution result : actualResults) {
			assertThat(expectedResults.containsKey(result.getExecutionId()))
				.as(String.format("result returned from repo %s not expected for testType %s", result.getExecutionId(),
						testType))
				.isTrue();
			assertThat(result.getEndTime())
				.as(String.format("result had non null for endTime for the testType %s", testType))
				.isNull();
		}
	}

	@ParameterizedTest
	@MethodSource("data")
	public void findTasksByExternalExecutionId(DaoType testType) {
		testDefaultContext(testType);
		Map<Long, TaskExecution> sampleDataSet = createSampleDataSet(33);
		sampleDataSet.values().forEach(taskExecution -> {
			Page<TaskExecution> taskExecutionsByExecutionId = this.taskExplorer
				.findTaskExecutionsByExecutionId(taskExecution.getExternalExecutionId(), PageRequest.of(0, 5));
			assertThat(taskExecutionsByExecutionId.getTotalElements()).isEqualTo(1);
			assertThat(this.taskExplorer
				.getTaskExecutionCountByExternalExecutionId(taskExecution.getExternalExecutionId())).isEqualTo(1);
			TaskExecution resultTaskExecution = taskExecutionsByExecutionId.getContent().get(0);
			assertThat(resultTaskExecution.getExecutionId()).isEqualTo(taskExecution.getExecutionId());
		});
	}

	@ParameterizedTest
	@MethodSource("data")
	public void findTasksByExternalExecutionIdMultipleEntry(DaoType testType) {
		testDefaultContext(testType);

		testDefaultContext(testType);
		final int SAME_EXTERNAL_ID_COUNT = 2;
		final int UNIQUE_COUNT = 3;

		Map<Long, TaskExecution> expectedResults = new HashMap<>();
		// Store task executions each with a unique external execution id
		int i = 0;
		for (; i < UNIQUE_COUNT; i++) {
			createAndSaveTaskExecution(i);
		}
		// Create task execution with same external execution id
		for (; i < (UNIQUE_COUNT + SAME_EXTERNAL_ID_COUNT); i++) {
			TaskExecution expectedTaskExecution = this.taskRepository.createTaskExecution(getSimpleTaskExecution());
			expectedResults.put(expectedTaskExecution.getExecutionId(), expectedTaskExecution);
		}
		Pageable pageable = PageRequest.of(0, 10);
		Page<TaskExecution> resultSet = this.taskExplorer.findTaskExecutionsByExecutionId(EXTERNAL_EXECUTION_ID,
				pageable);
		assertThat(resultSet.getTotalElements()).isEqualTo(SAME_EXTERNAL_ID_COUNT);
		List<TaskExecution> taskExecutions = resultSet.getContent();
		taskExecutions.forEach(taskExecution -> {
			assertThat(expectedResults.keySet()).contains(taskExecution.getExecutionId());
		});
		assertThat(this.taskExplorer.getTaskExecutionCountByExternalExecutionId(EXTERNAL_EXECUTION_ID))
			.isEqualTo(SAME_EXTERNAL_ID_COUNT);

	}

	@ParameterizedTest
	@MethodSource("data")
	public void findTasksByName(DaoType testType) {
		testDefaultContext(testType);
		final int TEST_COUNT = 5;
		final int COMPLETE_COUNT = 7;

		Map<Long, TaskExecution> expectedResults = new HashMap<>();
		// Store completed task executions
		for (int i = 0; i < COMPLETE_COUNT; i++) {
			createAndSaveTaskExecution(i);
		}

		for (int i = 0; i < TEST_COUNT; i++) {
			TaskExecution expectedTaskExecution = this.taskRepository.createTaskExecution(getSimpleTaskExecution());
			expectedResults.put(expectedTaskExecution.getExecutionId(), expectedTaskExecution);
		}

		Pageable pageable = PageRequest.of(0, 10);
		Page<TaskExecution> resultSet = this.taskExplorer.findTaskExecutionsByName(TASK_NAME, pageable);
		assertThat(resultSet.getNumberOfElements())
			.as(String.format("Running task count for task name did not match expected result for testType %s",
					testType))
			.isEqualTo(TEST_COUNT);

		for (TaskExecution result : resultSet) {
			assertThat(expectedResults.containsKey(result.getExecutionId()))
				.as(String.format("result returned from %s repo %s not expected", testType, result.getExecutionId()))
				.isTrue();
			assertThat(result.getTaskName())
				.as(String.format("taskName for taskExecution is incorrect for testType %s", testType))
				.isEqualTo(TASK_NAME);
		}
	}

	@ParameterizedTest
	@MethodSource("data")
	public void getTaskNames(DaoType testType) {
		testDefaultContext(testType);
		final int TEST_COUNT = 5;
		Set<String> expectedResults = new HashSet<>();
		for (int i = 0; i < TEST_COUNT; i++) {
			TaskExecution expectedTaskExecution = createAndSaveTaskExecution(i);
			expectedResults.add(expectedTaskExecution.getTaskName());
		}
		List<String> actualTaskNames = this.taskExplorer.getTaskNames();
		for (String taskName : actualTaskNames) {
			assertThat(expectedResults.contains(taskName))
				.as(String.format("taskName was not in expected results for testType %s", testType))
				.isTrue();
		}
	}

	@ParameterizedTest
	@MethodSource("data")
	public void findAllExecutionsOffBoundry(DaoType testType) {
		testDefaultContext(testType);
		Pageable pageable = PageRequest.of(0, 10);
		verifyPageResults(pageable, 103);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void findAllExecutionsOffBoundryByOne(DaoType testType) {
		testDefaultContext(testType);
		Pageable pageable = PageRequest.of(0, 10);
		verifyPageResults(pageable, 101);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void findAllExecutionsOnBoundry(DaoType testType) {
		testDefaultContext(testType);
		Pageable pageable = PageRequest.of(0, 10);
		verifyPageResults(pageable, 100);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void findAllExecutionsNoResult(DaoType testType) {
		testDefaultContext(testType);
		Pageable pageable = PageRequest.of(0, 10);
		verifyPageResults(pageable, 0);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void findTasksForInvalidJob(DaoType testType) {
		testDefaultContext(testType);
		assertThat(this.taskExplorer.getTaskExecutionIdByJobExecutionId(55555L)).isNull();
	}

	@ParameterizedTest
	@MethodSource("data")
	public void findJobsExecutionIdsForInvalidTask(DaoType testType) {
		testDefaultContext(testType);
		assertThat(this.taskExplorer.getJobExecutionIdsByTaskExecutionId(555555L).size()).isEqualTo(0);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void getLatestTaskExecutionForTaskName(DaoType testType) {
		testDefaultContext(testType);
		Map<Long, TaskExecution> expectedResults = createSampleDataSet(5);
		for (Map.Entry<Long, TaskExecution> taskExecutionMapEntry : expectedResults.entrySet()) {
			TaskExecution latestTaskExecution = this.taskExplorer
				.getLatestTaskExecutionForTaskName(taskExecutionMapEntry.getValue().getTaskName());
			assertThat(latestTaskExecution)
				.as(String.format("expected a taskExecution but got null for test type %s", testType))
				.isNotNull();
			TestVerifierUtils.verifyTaskExecution(expectedResults.get(latestTaskExecution.getExecutionId()),
					latestTaskExecution);
		}
	}

	@ParameterizedTest
	@MethodSource("data")
	public void getLatestTaskExecutionsByTaskNames(DaoType testType) {
		testDefaultContext(testType);
		Map<Long, TaskExecution> expectedResults = createSampleDataSet(5);

		final List<String> taskNamesAsList = new ArrayList<>();

		for (TaskExecution taskExecution : expectedResults.values()) {
			taskNamesAsList.add(taskExecution.getTaskName());
		}

		final List<TaskExecution> latestTaskExecutions = this.taskExplorer
			.getLatestTaskExecutionsByTaskNames(taskNamesAsList.toArray(new String[taskNamesAsList.size()]));

		for (TaskExecution latestTaskExecution : latestTaskExecutions) {
			assertThat(latestTaskExecution)
				.as(String.format("expected a taskExecution but got null for test type %s", testType))
				.isNotNull();
			TestVerifierUtils.verifyTaskExecution(expectedResults.get(latestTaskExecution.getExecutionId()),
					latestTaskExecution);
		}
	}

	private void verifyPageResults(Pageable pageable, int totalNumberOfExecs) {
		Map<Long, TaskExecution> expectedResults = createSampleDataSet(totalNumberOfExecs);
		List<Long> sortedExecIds = getSortedOfTaskExecIds(expectedResults);
		Iterator<Long> expectedTaskExecutionIter = sortedExecIds.iterator();
		// Verify pageable totals
		Page<TaskExecution> taskPage = this.taskExplorer.findAll(pageable);
		int pagesExpected = (int) Math.ceil(totalNumberOfExecs / ((double) pageable.getPageSize()));
		assertThat(taskPage.getTotalPages()).as("actual page count return was not the expected total")
			.isEqualTo(pagesExpected);
		assertThat(taskPage.getTotalElements()).as("actual element count was not the expected count")
			.isEqualTo(totalNumberOfExecs);

		// Verify pagination
		Pageable actualPageable = PageRequest.of(0, pageable.getPageSize());
		boolean hasMorePages = taskPage.hasContent();
		int pageNumber = 0;
		int elementCount = 0;
		while (hasMorePages) {
			taskPage = this.taskExplorer.findAll(actualPageable);
			hasMorePages = taskPage.hasNext();
			List<TaskExecution> actualTaskExecutions = taskPage.getContent();
			int expectedPageSize = pageable.getPageSize();
			if (!hasMorePages && pageable.getPageSize() != actualTaskExecutions.size()) {
				expectedPageSize = totalNumberOfExecs % pageable.getPageSize();
			}
			assertThat(actualTaskExecutions.size())
				.as(String.format("Element count on page did not match on the %n page", pageNumber))
				.isEqualTo(expectedPageSize);
			for (TaskExecution actualExecution : actualTaskExecutions) {
				assertThat(actualExecution.getExecutionId())
					.as(String.format("Element on page %n did not match expected", pageNumber))
					.isEqualTo((long) expectedTaskExecutionIter.next());
				TestVerifierUtils.verifyTaskExecution(expectedResults.get(actualExecution.getExecutionId()),
						actualExecution);
				elementCount++;
			}
			actualPageable = taskPage.nextPageable();
			pageNumber++;
		}
		// Verify actual totals
		assertThat(pageNumber).as("Pages processed did not equal expected").isEqualTo(pagesExpected);
		assertThat(elementCount).as("Elements processed did not equal expected,").isEqualTo(totalNumberOfExecs);
	}

	private TaskExecution createAndSaveTaskExecution(int i) {
		TaskExecution taskExecution = TestVerifierUtils.createSampleTaskExecution(i);
		taskExecution = this.taskRepository.createTaskExecution(taskExecution);
		return taskExecution;
	}

	private void initializeJdbcExplorerTest() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();

		this.context.getAutowireCapableBeanFactory()
			.autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
	}

	private void initializeMapExplorerTest() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();

		this.context.getAutowireCapableBeanFactory()
			.autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
	}

	private Map<Long, TaskExecution> createSampleDataSet(int count) {
		Map<Long, TaskExecution> expectedResults = new HashMap<>();
		for (int i = 0; i < count; i++) {
			TaskExecution expectedTaskExecution = createAndSaveTaskExecution(i);
			expectedResults.put(expectedTaskExecution.getExecutionId(), expectedTaskExecution);
		}
		return expectedResults;
	}

	private List<Long> getSortedOfTaskExecIds(Map<Long, TaskExecution> taskExecutionMap) {
		List<Long> sortedExecIds = new ArrayList<>(taskExecutionMap.size());
		TreeSet<TaskExecution> sortedSet = getTreeSet();
		sortedSet.addAll(taskExecutionMap.values());
		Iterator<TaskExecution> iterator = sortedSet.descendingIterator();
		while (iterator.hasNext()) {
			sortedExecIds.add(iterator.next().getExecutionId());
		}
		return sortedExecIds;
	}

	private TreeSet<TaskExecution> getTreeSet() {
		return new TreeSet<>(new Comparator<TaskExecution>() {
			@Override
			public int compare(TaskExecution e1, TaskExecution e2) {
				int result = e1.getStartTime().compareTo(e2.getStartTime());
				if (result == 0) {
					result = Long.valueOf(e1.getExecutionId()).compareTo(e2.getExecutionId());
				}
				return result;
			}
		});
	}

	private TaskExecution getSimpleTaskExecution() {
		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName(TASK_NAME);
		taskExecution.setStartTime(LocalDateTime.now());
		taskExecution.setExternalExecutionId(EXTERNAL_EXECUTION_ID);
		return taskExecution;
	}

	private enum DaoType {

		jdbc, map

	}

	@Configuration
	public static class DataSourceConfiguration {

	}

	@Configuration
	public static class EmptyConfiguration {

	}

}
