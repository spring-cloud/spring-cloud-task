/*
 * Copyright 2015-2020 the original author or authors.
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
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Executes unit tests on JdbcTaskExecutionDao.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Michael Minella
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
		classes = { TestConfiguration.class, EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class })
public class JdbcTaskExecutionDaoTests extends BaseTaskExecutionDaoTestCases {

	@Autowired
	TaskRepository repository;

	@Autowired
	private DataSource dataSource;

	@Before
	public void setup() {
		final JdbcTaskExecutionDao dao = new JdbcTaskExecutionDao(this.dataSource);
		dao.setTaskIncrementer(TestDBUtils.getIncrementer(this.dataSource));
		super.dao = dao;
	}

	@Test
	@DirtiesContext
	public void testStartTaskExecution() {
		TaskExecution expectedTaskExecution = this.dao.createTaskExecution(null, null,
				new ArrayList<>(0), null);

		expectedTaskExecution.setArguments(
				Collections.singletonList("foo=" + UUID.randomUUID().toString()));
		expectedTaskExecution.setStartTime(new Date());
		expectedTaskExecution.setTaskName(UUID.randomUUID().toString());

		this.dao.startTaskExecution(expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getTaskName(), expectedTaskExecution.getStartTime(),
				expectedTaskExecution.getArguments(),
				expectedTaskExecution.getExternalExecutionId());

		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				TestDBUtils.getTaskExecutionFromDB(this.dataSource,
						expectedTaskExecution.getExecutionId()));
	}

	@Test
	@DirtiesContext
	public void createTaskExecution() {
		TaskExecution expectedTaskExecution = TestVerifierUtils
				.createSampleTaskExecutionNoArg();
		expectedTaskExecution = this.dao.createTaskExecution(
				expectedTaskExecution.getTaskName(), expectedTaskExecution.getStartTime(),
				expectedTaskExecution.getArguments(),
				expectedTaskExecution.getExternalExecutionId());

		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				TestDBUtils.getTaskExecutionFromDB(this.dataSource,
						expectedTaskExecution.getExecutionId()));
	}

	@Test
	@DirtiesContext
	public void createEmptyTaskExecution() {
		TaskExecution expectedTaskExecution = this.dao.createTaskExecution(null, null,
				new ArrayList<>(0), null);

		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				TestDBUtils.getTaskExecutionFromDB(this.dataSource,
						expectedTaskExecution.getExecutionId()));
	}

	@Test
	@DirtiesContext
	public void completeTaskExecution() {
		TaskExecution expectedTaskExecution = TestVerifierUtils
				.endSampleTaskExecutionNoArg();
		expectedTaskExecution = this.dao.createTaskExecution(
				expectedTaskExecution.getTaskName(), expectedTaskExecution.getStartTime(),
				expectedTaskExecution.getArguments(),
				expectedTaskExecution.getExternalExecutionId());
		this.dao.completeTaskExecution(expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getExitCode(), expectedTaskExecution.getEndTime(),
				expectedTaskExecution.getExitMessage());
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				TestDBUtils.getTaskExecutionFromDB(this.dataSource,
						expectedTaskExecution.getExecutionId()));
	}

	@Test(expected = IllegalStateException.class)
	@DirtiesContext
	public void completeTaskExecutionWithNoCreate() {
		JdbcTaskExecutionDao dao = new JdbcTaskExecutionDao(this.dataSource);

		TaskExecution expectedTaskExecution = TestVerifierUtils
				.endSampleTaskExecutionNoArg();
		dao.completeTaskExecution(expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getExitCode(), expectedTaskExecution.getEndTime(),
				expectedTaskExecution.getExitMessage());
	}

	@Test
	@DirtiesContext
	public void testFindAllPageableSort() {
		initializeRepositoryNotInOrder();
		Sort sort = Sort.by(new Sort.Order(Sort.Direction.ASC, "EXTERNAL_EXECUTION_ID"));
		Iterator<TaskExecution> iter = getPageIterator(0, 2, sort);
		TaskExecution taskExecution = iter.next();
		assertThat(taskExecution.getTaskName()).isEqualTo("FOO2");
		taskExecution = iter.next();
		assertThat(taskExecution.getTaskName()).isEqualTo("FOO3");

		iter = getPageIterator(1, 2, sort);
		taskExecution = iter.next();
		assertThat(taskExecution.getTaskName()).isEqualTo("FOO1");
	}

	@Test
	@DirtiesContext
	public void testFindAllDefaultSort() {
		initializeRepository();
		Iterator<TaskExecution> iter = getPageIterator(0, 2, null);
		TaskExecution taskExecution = iter.next();
		assertThat(taskExecution.getTaskName()).isEqualTo("FOO1");
		taskExecution = iter.next();
		assertThat(taskExecution.getTaskName()).isEqualTo("FOO2");

		iter = getPageIterator(1, 2, null);
		taskExecution = iter.next();
		assertThat(taskExecution.getTaskName()).isEqualTo("FOO3");
	}

	@Test
	@DirtiesContext
	public void testStartExecutionWithNullExternalExecutionIdExisting() {
		TaskExecution expectedTaskExecution = initializeTaskExecutionWithExternalExecutionId();

		this.dao.startTaskExecution(expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getTaskName(), expectedTaskExecution.getStartTime(),
				expectedTaskExecution.getArguments(), null);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				TestDBUtils.getTaskExecutionFromDB(this.dataSource,
						expectedTaskExecution.getExecutionId()));
	}

	@Test
	@DirtiesContext
	public void testStartExecutionWithNullExternalExecutionIdNonExisting() {
		TaskExecution expectedTaskExecution = initializeTaskExecutionWithExternalExecutionId();

		this.dao.startTaskExecution(expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getTaskName(), expectedTaskExecution.getStartTime(),
				expectedTaskExecution.getArguments(), "BAR");
		expectedTaskExecution.setExternalExecutionId("BAR");
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				TestDBUtils.getTaskExecutionFromDB(this.dataSource,
						expectedTaskExecution.getExecutionId()));
	}

	@Test
	@DirtiesContext
	public void testFindRunningTaskExecutions() {
		initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		assertThat(this.dao
				.findRunningTaskExecutions("FOO1",
						PageRequest.of(1, Integer.MAX_VALUE, Sort.by("START_TIME")))
				.getTotalElements()).isEqualTo(4);
	}

	@Test
	@DirtiesContext
	public void testFindRunningTaskExecutionsIllegalSort() {
		initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		assertThatThrownBy(
				() -> this.dao
						.findRunningTaskExecutions("FOO1",
								PageRequest.of(1, Integer.MAX_VALUE,
										Sort.by("ILLEGAL_SORT")))
						.getTotalElements()).isInstanceOf(IllegalArgumentException.class)
								.hasMessage("Invalid sort option selected: ILLEGAL_SORT");
	}

	@Test
	@DirtiesContext
	public void testFindRunningTaskExecutionsSortWithDifferentCase() {
		initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		assertThat(this.dao
				.findRunningTaskExecutions("FOO1",
						PageRequest.of(1, Integer.MAX_VALUE, Sort.by("StArT_TiMe")))
				.getTotalElements()).isEqualTo(4);
	}

	private TaskExecution initializeTaskExecutionWithExternalExecutionId() {
		TaskExecution expectedTaskExecution = TestVerifierUtils
				.createSampleTaskExecutionNoArg();
		return this.dao.createTaskExecution(expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(),
				expectedTaskExecution.getArguments(), "FOO1");
	}

	private Iterator<TaskExecution> getPageIterator(int pageNum, int pageSize,
			Sort sort) {
		Pageable pageable = (sort == null) ? PageRequest.of(pageNum, pageSize)
				: PageRequest.of(pageNum, pageSize, sort);
		Page<TaskExecution> page = this.dao.findAll(pageable);
		assertThat(page.getTotalElements()).isEqualTo(3);
		assertThat(page.getTotalPages()).isEqualTo(2);
		return page.iterator();
	}

	private void initializeRepository() {
		this.repository.createTaskExecution(getTaskExecution("FOO3", "externalA"));
		this.repository.createTaskExecution(getTaskExecution("FOO2", "externalB"));
		this.repository.createTaskExecution(getTaskExecution("FOO1", "externalC"));
	}

	private void initializeRepositoryNotInOrder() {
		this.repository.createTaskExecution(getTaskExecution("FOO1", "externalC"));
		this.repository.createTaskExecution(getTaskExecution("FOO2", "externalA"));
		this.repository.createTaskExecution(getTaskExecution("FOO3", "externalB"));
	}

}
