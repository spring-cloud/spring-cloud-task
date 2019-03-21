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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
@RunWith(Parameterized.class)
public class SimpleTaskExplorerTests {

	private final static String TASK_NAME = "FOOBAR";

	private final static String EXTERNAL_EXECUTION_ID = "123ABC";

	@Rule
	public ExpectedException expected = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@Autowired
	private TaskExplorer taskExplorer;

	@Autowired
	private TaskRepository taskRepository;

	private DaoType testType;

	public SimpleTaskExplorerTests(DaoType testType) {
		this.testType = testType;
	}

	@Parameterized.Parameters
	public static Collection<Object> data() {
		return Arrays.asList(new Object[] { DaoType.jdbc, DaoType.map });
	}

	@Before
	public void testDefaultContext() throws Exception {

		if (this.testType == DaoType.jdbc) {
			initializeJdbcExplorerTest();
		}
		else {
			initializeMapExplorerTest();
		}
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void getTaskExecution() {
		Map<Long, TaskExecution> expectedResults = createSampleDataSet(5);
		for (Long taskExecutionId : expectedResults.keySet()) {
			TaskExecution actualTaskExecution = this.taskExplorer
					.getTaskExecution(taskExecutionId);
			assertThat(actualTaskExecution).as(String.format(
					"expected a taskExecution but got null for test type %s",
					this.testType)).isNotNull();
			TestVerifierUtils.verifyTaskExecution(expectedResults.get(taskExecutionId),
					actualTaskExecution);
		}
	}

	@Test
	public void taskExecutionNotFound() {
		createSampleDataSet(5);

		TaskExecution actualTaskExecution = this.taskExplorer.getTaskExecution(-5);
		assertThat(actualTaskExecution).as(
				String.format("expected null for actualTaskExecution %s", this.testType))
				.isNull();
	}

	@Test
	public void getTaskCountByTaskName() {
		Map<Long, TaskExecution> expectedResults = createSampleDataSet(5);
		for (Map.Entry<Long, TaskExecution> entry : expectedResults.entrySet()) {
			String taskName = entry.getValue().getTaskName();
			assertThat(this.taskExplorer.getTaskExecutionCountByTaskName(taskName))
					.as(String.format(
							"task count for task name did not match expected result for testType %s",
							this.testType))
					.isEqualTo(1);
		}
	}

	@Test
	public void getTaskCount() {
		createSampleDataSet(33);
		assertThat(this.taskExplorer.getTaskExecutionCount()).as(
				String.format("task count did not match expected result for test Type %s",
						this.testType))
				.isEqualTo(33);
	}

	@Test
	public void getRunningTaskCount() {
		createSampleDataSet(33);
		assertThat(this.taskExplorer.getRunningTaskExecutionCount()).as(
				String.format("task count did not match expected result for test Type %s",
						this.testType))
				.isEqualTo(33);
	}

	@Test
	public void findRunningTasks() {
		final int TEST_COUNT = 2;
		final int COMPLETE_COUNT = 5;

		Map<Long, TaskExecution> expectedResults = new HashMap<>();
		// Store completed jobs
		int i = 0;
		for (; i < COMPLETE_COUNT; i++) {
			createAndSaveTaskExecution(i);
		}

		for (; i < (COMPLETE_COUNT + TEST_COUNT); i++) {
			TaskExecution expectedTaskExecution = this.taskRepository
					.createTaskExecution(getSimpleTaskExecution());
			expectedResults.put(expectedTaskExecution.getExecutionId(),
					expectedTaskExecution);
		}
		Pageable pageable = PageRequest.of(0, 10);

		Page<TaskExecution> actualResults = this.taskExplorer
				.findRunningTaskExecutions(TASK_NAME, pageable);
		assertThat(actualResults.getNumberOfElements()).as(String.format(
				"Running task count for task name did not match expected result for testType %s",
				this.testType)).isEqualTo(TEST_COUNT);

		for (TaskExecution result : actualResults) {
			assertThat(expectedResults.containsKey(result.getExecutionId())).as(String
					.format("result returned from repo %s not expected for testType %s",
							result.getExecutionId(), this.testType))
					.isTrue();
			assertThat(result.getEndTime()).as(String.format(
					"result had non null for endTime for the testType %s", this.testType))
					.isNull();
		}
	}

	@Test
	public void findTasksByName() {
		final int TEST_COUNT = 5;
		final int COMPLETE_COUNT = 7;

		Map<Long, TaskExecution> expectedResults = new HashMap<>();
		// Store completed jobs
		for (int i = 0; i < COMPLETE_COUNT; i++) {
			createAndSaveTaskExecution(i);
		}

		for (int i = 0; i < TEST_COUNT; i++) {
			TaskExecution expectedTaskExecution = this.taskRepository
					.createTaskExecution(getSimpleTaskExecution());
			expectedResults.put(expectedTaskExecution.getExecutionId(),
					expectedTaskExecution);
		}

		Pageable pageable = PageRequest.of(0, 10);
		Page<TaskExecution> resultSet = this.taskExplorer
				.findTaskExecutionsByName(TASK_NAME, pageable);
		assertThat(resultSet.getNumberOfElements()).as(String.format(
				"Running task count for task name did not match expected result for testType %s",
				this.testType)).isEqualTo(TEST_COUNT);

		for (TaskExecution result : resultSet) {
			assertThat(expectedResults.containsKey(result.getExecutionId()))
					.as(String.format("result returned from %s repo %s not expected",
							this.testType, result.getExecutionId()))
					.isTrue();
			assertThat(result.getTaskName()).as(String.format(
					"taskName for taskExecution is incorrect for testType %s",
					this.testType)).isEqualTo(TASK_NAME);
		}
	}

	@Test
	public void getTaskNames() {
		final int TEST_COUNT = 5;
		Set<String> expectedResults = new HashSet<>();
		for (int i = 0; i < TEST_COUNT; i++) {
			TaskExecution expectedTaskExecution = createAndSaveTaskExecution(i);
			expectedResults.add(expectedTaskExecution.getTaskName());
		}
		List<String> actualTaskNames = this.taskExplorer.getTaskNames();
		for (String taskName : actualTaskNames) {
			assertThat(expectedResults.contains(taskName)).as(
					String.format("taskName was not in expected results for testType %s",
							this.testType))
					.isTrue();
		}
	}

	@Test
	public void findAllExecutionsOffBoundry() {
		Pageable pageable = PageRequest.of(0, 10);
		verifyPageResults(pageable, 103);
	}

	@Test
	public void findAllExecutionsOffBoundryByOne() {
		Pageable pageable = PageRequest.of(0, 10);
		verifyPageResults(pageable, 101);
	}

	@Test
	public void findAllExecutionsOnBoundry() {
		Pageable pageable = PageRequest.of(0, 10);
		verifyPageResults(pageable, 100);
	}

	@Test
	public void findAllExecutionsNoResult() {
		Pageable pageable = PageRequest.of(0, 10);
		verifyPageResults(pageable, 0);
	}

	@Test
	public void findTasksForInvalidJob() {
		assertThat(this.taskExplorer.getTaskExecutionIdByJobExecutionId(55555L)).isNull();
	}

	@Test
	public void findJobsExecutionIdsForInvalidTask() {
		assertThat(this.taskExplorer.getJobExecutionIdsByTaskExecutionId(555555L).size())
				.isEqualTo(0);
	}

	@Test
	public void getLatestTaskExecutionForTaskName() {
		Map<Long, TaskExecution> expectedResults = createSampleDataSet(5);
		for (Map.Entry<Long, TaskExecution> taskExecutionMapEntry : expectedResults
				.entrySet()) {
			TaskExecution latestTaskExecution = this.taskExplorer
					.getLatestTaskExecutionForTaskName(
							taskExecutionMapEntry.getValue().getTaskName());
			assertThat(latestTaskExecution).as(String.format(
					"expected a taskExecution but got null for test type %s",
					this.testType)).isNotNull();
			TestVerifierUtils.verifyTaskExecution(
					expectedResults.get(latestTaskExecution.getExecutionId()),
					latestTaskExecution);
		}
	}

	@Test
	public void getLatestTaskExecutionsByTaskNames() {
		Map<Long, TaskExecution> expectedResults = createSampleDataSet(5);

		final List<String> taskNamesAsList = new ArrayList<>();

		for (TaskExecution taskExecution : expectedResults.values()) {
			taskNamesAsList.add(taskExecution.getTaskName());
		}

		final List<TaskExecution> latestTaskExecutions = this.taskExplorer
				.getLatestTaskExecutionsByTaskNames(
						taskNamesAsList.toArray(new String[taskNamesAsList.size()]));

		for (TaskExecution latestTaskExecution : latestTaskExecutions) {
			assertThat(latestTaskExecution).as(String.format(
					"expected a taskExecution but got null for test type %s",
					this.testType)).isNotNull();
			TestVerifierUtils.verifyTaskExecution(
					expectedResults.get(latestTaskExecution.getExecutionId()),
					latestTaskExecution);
		}
	}

	private void verifyPageResults(Pageable pageable, int totalNumberOfExecs) {
		Map<Long, TaskExecution> expectedResults = createSampleDataSet(
				totalNumberOfExecs);
		List<Long> sortedExecIds = getSortedOfTaskExecIds(expectedResults);
		Iterator<Long> expectedTaskExecutionIter = sortedExecIds.iterator();
		// Verify pageable totals
		Page<TaskExecution> taskPage = this.taskExplorer.findAll(pageable);
		int pagesExpected = (int) Math
				.ceil(totalNumberOfExecs / ((double) pageable.getPageSize()));
		assertThat(taskPage.getTotalPages())
				.as("actual page count return was not the expected total")
				.isEqualTo(pagesExpected);
		assertThat(taskPage.getTotalElements())
				.as("actual element count was not the expected count")
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
			assertThat(actualTaskExecutions.size()).as(String.format(
					"Element count on page did not match on the %n page", pageNumber))
					.isEqualTo(expectedPageSize);
			for (TaskExecution actualExecution : actualTaskExecutions) {
				assertThat(actualExecution.getExecutionId())
						.as(String.format("Element on page %n did not match expected",
								pageNumber))
						.isEqualTo((long) expectedTaskExecutionIter.next());
				TestVerifierUtils.verifyTaskExecution(
						expectedResults.get(actualExecution.getExecutionId()),
						actualExecution);
				elementCount++;
			}
			actualPageable = taskPage.nextPageable();
			pageNumber++;
		}
		// Verify actual totals
		assertThat(pageNumber).as("Pages processed did not equal expected")
				.isEqualTo(pagesExpected);
		assertThat(elementCount).as("Elements processed did not equal expected,")
				.isEqualTo(totalNumberOfExecs);
	}

	private TaskExecution createAndSaveTaskExecution(int i) {
		TaskExecution taskExecution = TestVerifierUtils.createSampleTaskExecution(i);
		taskExecution = this.taskRepository.createTaskExecution(taskExecution);
		return taskExecution;
	}

	private void initializeJdbcExplorerTest() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();

		this.context.getAutowireCapableBeanFactory().autowireBeanProperties(this,
				AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
	}

	private void initializeMapExplorerTest() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();

		this.context.getAutowireCapableBeanFactory().autowireBeanProperties(this,
				AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
	}

	private Map<Long, TaskExecution> createSampleDataSet(int count) {
		Map<Long, TaskExecution> expectedResults = new HashMap<>();
		for (int i = 0; i < count; i++) {
			TaskExecution expectedTaskExecution = createAndSaveTaskExecution(i);
			expectedResults.put(expectedTaskExecution.getExecutionId(),
					expectedTaskExecution);
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
					result = Long.valueOf(e1.getExecutionId())
							.compareTo(e2.getExecutionId());
				}
				return result;
			}
		});
	}

	private TaskExecution getSimpleTaskExecution() {
		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName(TASK_NAME);
		taskExecution.setStartTime(new Date());
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
