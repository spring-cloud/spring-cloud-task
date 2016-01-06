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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

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
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.configuration.TestConfiguration;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.cloud.task.util.TestVerifierUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * @author Glenn Renfro
 */
@RunWith(Parameterized.class)
public class SimpleTaskExplorerTests {

	private AnnotationConfigApplicationContext context;

	@Autowired
	private TaskExecutionDao dao;

	@Autowired
	private TaskExplorer taskExplorer;

	private DaoType testType;

	@Parameterized.Parameters
	public static Collection<Object> data() {
		return Arrays.asList(new Object[]{
				DaoType.jdbc, DaoType.map });
	}

	public SimpleTaskExplorerTests(DaoType testType) {
		this.testType = testType;
	}

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Before
	public void testDefaultContext() throws Exception {

		if (testType == DaoType.jdbc) {
			initializeJdbcExplorerTest();
		}
		else {
			initializeMapExplorerTest();
		}

		taskExplorer = new SimpleTaskExplorer(dao);
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void getTaskExecution() {
		Map<String, TaskExecution> expectedResults = createSampleDataSet(5);
		for (String taskExecutionId : expectedResults.keySet()) {
			TaskExecution actualTaskExecution =
					taskExplorer.getTaskExecution(taskExecutionId);
			assertNotNull(String.format(
					"expected a taskExecution but got null for test type %s", testType),
					actualTaskExecution);
			TestVerifierUtils.verifyTaskExecution(expectedResults.get(taskExecutionId),
					actualTaskExecution);
		}
	}

	@Test
	public void taskExecutionNotFound() {
		Map<String, TaskExecution> expectedResults = createSampleDataSet(5);

		TaskExecution actualTaskExecution =
				taskExplorer.getTaskExecution("NO_EXECUTION_PRESENT");
		assertNull(String.format(
				"expected null for actualTaskExecution %s", testType),
				actualTaskExecution);
	}

	@Test
	public void getTaskCountByTaskName() {
		Map<String, TaskExecution> expectedResults = createSampleDataSet(5);
		for (Map.Entry<String, TaskExecution> entry : expectedResults.entrySet()) {
			String taskName = entry.getValue().getTaskName();
			assertEquals(String.format(
					"task count for task name did not match expected result for testType %s",
					testType),
					1, taskExplorer.getTaskExecutionCountByTaskName(taskName));
		}
	}

	@Test
	public void getTaskCount() {
		Map<String, TaskExecution> expectedResults = createSampleDataSet(33);
		assertEquals(String.format(
				"task count did not match expected result for test Type %s",
				testType),
				33, taskExplorer.getTaskExecutionCount());
	}

	@Test
	public void findRunningTasks() {
		final int TEST_COUNT = 2;
		final int COMPLETE_COUNT = 5;
		final String TASK_NAME = "FOOBAR";

		Map<String, TaskExecution> expectedResults = new HashMap<>();
		//Store completed jobs
		for (int i = 0; i < COMPLETE_COUNT; i++) {
			createAndSaveTaskExecution();
		}

		for (int i = 0; i < TEST_COUNT; i++) {
			TaskExecution expectedTaskExecution = new TaskExecution();
			expectedTaskExecution.setStartTime(new Date());
			expectedTaskExecution.setExecutionId(UUID.randomUUID().toString());
			expectedTaskExecution.setTaskName(TASK_NAME);
			dao.saveTaskExecution(expectedTaskExecution);
			expectedResults.put(expectedTaskExecution.getExecutionId(), expectedTaskExecution);
		}
		Set<TaskExecution> actualResults = taskExplorer.findRunningTaskExecutions(TASK_NAME);
		assertEquals(String.format(
				"Running task count for task name did not match expected result for testType %s",
				testType), TEST_COUNT, actualResults.size());

		for (TaskExecution result : actualResults) {
			assertTrue(String.format(
					"result returned from repo %s not expected for testType %s",
					result.getExecutionId(), testType),
					expectedResults.containsKey(result.getExecutionId()));
			assertNull(String.format("result had non null for endTime for the testType %s",
					testType), result.getEndTime());
		}
	}

	@Test
	public void findTasksByName() {
		final int TEST_COUNT = 5;
		final int COMPLETE_COUNT = 7;
		final int RESULT_SET_SIZE = 3;
		final String TASK_NAME = "FOOBAR";

		Map<String, TaskExecution> expectedResults = new HashMap<>();
		//Store completed jobs
		for (int i = 0; i < COMPLETE_COUNT; i++) {
			createAndSaveTaskExecution();
		}

		for (int i = 0; i < TEST_COUNT; i++) {
			TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoParam();
			expectedTaskExecution.setTaskName(TASK_NAME);
			dao.saveTaskExecution(expectedTaskExecution);
			expectedResults.put(expectedTaskExecution.getExecutionId(), expectedTaskExecution);
		}
		List<TaskExecution> resultSet = taskExplorer.getTaskExecutionsByName(TASK_NAME, 1, RESULT_SET_SIZE);
		assertEquals(String.format(
				"Running task count for task name did not match expected result for testType %s",
				testType), RESULT_SET_SIZE, resultSet.size());

		for (TaskExecution result : resultSet) {
			assertTrue(String.format("result returned from %s repo %s not expected",
					testType, result.getExecutionId()),
					expectedResults.containsKey(result.getExecutionId()));
			assertEquals(
					String.format("taskName for taskExecution is incorrect for testType %s",
							testType), TASK_NAME, result.getTaskName());
		}
	}

	@Test
	public void getTaskNames() {
		final int TEST_COUNT = 5;
		Set<String> expectedResults = new HashSet<>();
		for (int i = 0; i < TEST_COUNT; i++) {
			TaskExecution expectedTaskExecution = createAndSaveTaskExecution();
			expectedResults.add(expectedTaskExecution.getTaskName());
		}
		List<String> actualTaskNames = taskExplorer.getTaskNames();
		for (String taskName : actualTaskNames) {
			assertTrue(String.format("taskName was not in expected results for testType %s",
					testType), expectedResults.contains(taskName));
		}
	}

	@Test
	public void findAllExecutionsOffBoundry() {
		Pageable pageable = new PageRequest(0, 10);
		verifyPageResults(pageable, 103);
	}

	@Test
	public void findAllExecutionsOffBoundryByOne() {
		Pageable pageable = new PageRequest(0, 10);
		verifyPageResults(pageable, 101);
	}

	@Test
	public void findAllExecutionsOnBoundry() {
		Pageable pageable = new PageRequest(0, 10);
		verifyPageResults(pageable, 100);
	}

	@Test
	public void findAllExecutionsNoResult() {
		Pageable pageable = new PageRequest(0, 10);
		verifyPageResults(pageable, 0);
	}

	private void verifyPageResults(Pageable pageable, int totalNumberOfExecs) {
		Map<String, TaskExecution> expectedResults = createSampleDataSet(totalNumberOfExecs);
		List<String> sortedExecIds = getSortedOfTaskExecIds(expectedResults);
		Iterator<String> expectedTaskExecutionIter = sortedExecIds.iterator();
		//Verify pageable totals
		Page taskPage = taskExplorer.findAll(pageable);
		int pagesExpected = (int) Math.ceil(totalNumberOfExecs / ((double) pageable.getPageSize()));
		assertEquals("actual page count return was not the expected total",
				pagesExpected,
				taskPage.getTotalPages());
		assertEquals("actual element count was not the expected count", totalNumberOfExecs,
				taskPage.getTotalElements());

		//Verify pagination
		Pageable actualPageable = new PageRequest(0, pageable.getPageSize());
		boolean hasMorePages = taskPage.hasContent();
		int pageNumber = 0;
		int elementCount = 0;
		while (hasMorePages) {
			taskPage = taskExplorer.findAll(actualPageable);
			hasMorePages = taskPage.hasNext();
			List<TaskExecution> actualTaskExecutions = taskPage.getContent();
			int expectedPageSize = pageable.getPageSize();
			if (!hasMorePages && pageable.getPageSize() != actualTaskExecutions.size()) {
				expectedPageSize = totalNumberOfExecs % pageable.getPageSize();
			}
			assertEquals(
					String.format("Element count on page did not match on the %n page",
							pageNumber), expectedPageSize, actualTaskExecutions.size());
			for (TaskExecution actualExecution : actualTaskExecutions) {
				assertEquals(String.format("Element on page %n did not match expected",
						pageNumber), expectedTaskExecutionIter.next(),
						actualExecution.getExecutionId());
				TestVerifierUtils.verifyTaskExecution(
						expectedResults.get(actualExecution.getExecutionId()),
						actualExecution);
				elementCount++;
			}
			actualPageable = taskPage.nextPageable();
			pageNumber++;
		}
		//Verify actual totals
		assertEquals("Pages processed did not equal expected", pagesExpected, pageNumber);
		assertEquals("Elements processed did not equal expected,", totalNumberOfExecs, elementCount);
	}

	private TaskExecution createAndSaveTaskExecution() {
		TaskExecution taskExecution = TestVerifierUtils.createSampleTaskExecution();
		dao.saveTaskExecution(taskExecution);
		return taskExecution;
	}

	private void initializeJdbcExplorerTest() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();

		context.getAutowireCapableBeanFactory().autowireBeanProperties(this,
				AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
	}

	private void initializeMapExplorerTest() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();

		context.getAutowireCapableBeanFactory().autowireBeanProperties(this,
				AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
	}

	private Map<String, TaskExecution> createSampleDataSet(int count){
		Map<String, TaskExecution> expectedResults = new HashMap<>();
		for (int i = 0; i < count; i++) {
			TaskExecution expectedTaskExecution = createAndSaveTaskExecution();
			expectedResults.put(expectedTaskExecution.getExecutionId(),
					expectedTaskExecution);
		}
		return expectedResults;
	}

	private List<String> getSortedOfTaskExecIds(Map<String, TaskExecution> taskExecutionMap){
		List<String> sortedExecIds = new ArrayList<>(taskExecutionMap.size());
		TreeSet sortedSet = getTreeSet();
		sortedSet.addAll(taskExecutionMap.values());
		Iterator <TaskExecution> iterator = sortedSet.descendingIterator();
		while(iterator.hasNext()){
			sortedExecIds.add(iterator.next().getExecutionId());
		}
		return sortedExecIds;
	}

	private TreeSet getTreeSet(){
		return new TreeSet<TaskExecution>(new Comparator<TaskExecution>() {
			@Override
			public int compare(TaskExecution e1, TaskExecution e2) {
				int result = e1.getStartTime().compareTo(e2.getStartTime());
				if (result == 0){
					result = e1.getExecutionId().compareTo(e2.getExecutionId());
				}
				return result;
			}
		});
	}

	private enum DaoType{jdbc, map}
}
