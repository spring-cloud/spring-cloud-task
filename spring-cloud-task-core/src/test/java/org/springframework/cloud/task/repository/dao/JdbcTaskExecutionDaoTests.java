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
import java.util.Iterator;
import java.util.UUID;
import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.configuration.TestConfiguration;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.util.TestDBUtils;
import org.springframework.cloud.task.util.TestVerifierUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

/**
 * Executes unit tests on JdbcTaskExecutionDao.
 *
 * @author Glenn Renfro
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class,
		EmbeddedDataSourceConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class})
public class JdbcTaskExecutionDaoTests {

	@Autowired
	private DataSource dataSource;

	private JdbcTaskExecutionDao dao;

	@Autowired
	TaskRepository repository;

	@Before
	public void setup(){
		dao = new JdbcTaskExecutionDao(dataSource);
		dao.setTaskIncrementer(TestDBUtils.getIncrementer(dataSource));
	}

	@Test
	@DirtiesContext
	public void testStartTaskExecution() {
		TaskExecution expectedTaskExecution = dao.createTaskExecution(null, null,
				new ArrayList<String>(0), null);

		expectedTaskExecution.setArguments(Collections.singletonList("foo=" + UUID.randomUUID().toString()));
		expectedTaskExecution.setStartTime(new Date());
		expectedTaskExecution.setTaskName(UUID.randomUUID().toString());

		dao.startTaskExecution(expectedTaskExecution.getExecutionId(), expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
				expectedTaskExecution.getExternalExecutionId());

		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				TestDBUtils.getTaskExecutionFromDB(dataSource, expectedTaskExecution.getExecutionId()));
	}

	@Test
	@DirtiesContext
	public void createTaskExecution() {
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoArg();
		expectedTaskExecution = dao.createTaskExecution(expectedTaskExecution.getTaskName(), expectedTaskExecution.getStartTime(),
				expectedTaskExecution.getArguments(), expectedTaskExecution.getExternalExecutionId());

		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				TestDBUtils.getTaskExecutionFromDB(dataSource, expectedTaskExecution.getExecutionId()));
	}

	@Test
	@DirtiesContext
	public void createEmptyTaskExecution() {
		TaskExecution expectedTaskExecution = dao.createTaskExecution(null, null,
				new ArrayList<String>(0), null);

		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				TestDBUtils.getTaskExecutionFromDB(dataSource, expectedTaskExecution.getExecutionId()));
	}

	@Test
	@DirtiesContext
	public void completeTaskExecution() {
		TaskExecution expectedTaskExecution = TestVerifierUtils.endSampleTaskExecutionNoArg();
		expectedTaskExecution = dao.createTaskExecution(expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
				expectedTaskExecution.getExternalExecutionId());
		dao.completeTaskExecution(expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getExitCode(), expectedTaskExecution.getEndTime(),
				expectedTaskExecution.getExitMessage());
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				TestDBUtils.getTaskExecutionFromDB(dataSource, expectedTaskExecution.getExecutionId()));
	}

	@Test(expected = IllegalStateException.class)
	@DirtiesContext
	public void completeTaskExecutionWithNoCreate() {
		JdbcTaskExecutionDao dao = new JdbcTaskExecutionDao(dataSource);

		TaskExecution expectedTaskExecution = TestVerifierUtils.endSampleTaskExecutionNoArg();
		dao.completeTaskExecution(expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getExitCode(), expectedTaskExecution.getEndTime(),
				expectedTaskExecution.getExitMessage());
	}

	@Test
	@DirtiesContext
	public void testFindAllPageableSort()  {
		initializeRepositoryNotInOrder();
		Sort sort = Sort.by(new Sort.Order(Sort.Direction.ASC,
				"EXTERNAL_EXECUTION_ID"));
		Iterator<TaskExecution> iter = getPageIterator(0, 2, sort);
		TaskExecution taskExecution = iter.next();
		assertEquals("FOO2", taskExecution.getTaskName());
		taskExecution = iter.next();
		assertEquals("FOO3", taskExecution.getTaskName());

		iter = getPageIterator(1, 2, sort);
		taskExecution = iter.next();
		assertEquals("FOO1", taskExecution.getTaskName());
	}

	@Test
	@DirtiesContext
	public void testFindAllDefaultSort()  {
		initializeRepository();
		Iterator<TaskExecution> iter = getPageIterator(0, 2, null);
		TaskExecution taskExecution = iter.next();
		assertEquals("FOO1", taskExecution.getTaskName());
		taskExecution = iter.next();
		assertEquals("FOO2", taskExecution.getTaskName());

		iter = getPageIterator(1, 2, null);
		taskExecution = iter.next();
		assertEquals("FOO3", taskExecution.getTaskName());
	}

	@Test
	@DirtiesContext
	public void testStartExecutionWithNullExternalExecutionIdExisting() {
		TaskExecution expectedTaskExecution =
				initializeTaskExecutionWithExternalExecutionId();

		dao.startTaskExecution(expectedTaskExecution.getExecutionId(), expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
				null);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				TestDBUtils.getTaskExecutionFromDB(dataSource, expectedTaskExecution.getExecutionId()));
	}

	@Test
	@DirtiesContext
	public void testStartExecutionWithNullExternalExecutionIdNonExisting() {
		TaskExecution expectedTaskExecution =
				initializeTaskExecutionWithExternalExecutionId();

		dao.startTaskExecution(expectedTaskExecution.getExecutionId(), expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
				"BAR");
		expectedTaskExecution.setExternalExecutionId("BAR");
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				TestDBUtils.getTaskExecutionFromDB(dataSource, expectedTaskExecution.getExecutionId()));
	}

	private TaskExecution initializeTaskExecutionWithExternalExecutionId() {
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoArg();
		return this.dao.createTaskExecution(expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
				"FOO1");
	}

	private Iterator<TaskExecution> getPageIterator(int pageNum, int pageSize, Sort sort) {
		Pageable pageable = (sort == null) ?
				PageRequest.of(pageNum, pageSize) :
				PageRequest.of(pageNum, pageSize, sort);
		Page<TaskExecution> page = dao.findAll(pageable);
		assertEquals(3, page.getTotalElements());
		assertEquals(2, page.getTotalPages());
		return page.iterator();
	}

	private void initializeRepository() {
		repository.createTaskExecution(getTaskExecution("FOO3", "externalA"));
		repository.createTaskExecution(getTaskExecution("FOO2", "externalB"));
		repository.createTaskExecution(getTaskExecution("FOO1", "externalC"));
	}

	private void initializeRepositoryNotInOrder() {
		repository.createTaskExecution(getTaskExecution("FOO1", "externalC"));
		repository.createTaskExecution(getTaskExecution("FOO2", "externalA"));
		repository.createTaskExecution(getTaskExecution("FOO3", "externalB"));
	}

	private TaskExecution getTaskExecution(String taskName,
			String externalExecutionId) {
		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName(taskName);
		taskExecution.setExternalExecutionId(externalExecutionId);
		taskExecution.setStartTime(new Date());
		return taskExecution;
	}
}
