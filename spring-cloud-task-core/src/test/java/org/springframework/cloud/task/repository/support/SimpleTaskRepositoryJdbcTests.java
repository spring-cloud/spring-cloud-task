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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.util.TestUtils;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

/**
 * Tests for the SimpleTaskRepository that uses JDBC as a datastore.
 *
 * @author Glenn Renfro.
 */
public class SimpleTaskRepositoryJdbcTests {

	private EmbeddedDatabase db;

	private TaskRepository taskRepository;

	@Before
	public void setUp() {
		db = new EmbeddedDatabaseBuilder()
				.generateUniqueName(true)
				.addScripts("task-schema.sql")
				.build();
		JdbcTaskRepositoryFactoryBean factoryBean =
				new JdbcTaskRepositoryFactoryBean(db);
		taskRepository = factoryBean.getObject();

	}

	@After
	public void tearDown() {
		db.shutdown();
	}

	@Test
	public void testCreateTaskExecutionNoParam() {
		TaskExecution expectedTaskExecution =
				TestUtils.createAndStoreTaskExecutionNoParams(taskRepository);
		TaskExecution actualTaskExecution = TestUtils.getTaskExecutionFromDB(db,
				expectedTaskExecution.getExecutionId());
		TestUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	public void testCreateTaskExecutionWithParam() {
		TaskExecution expectedTaskExecution =
				TestUtils.createAndStoreTaskExecutionWithParams(taskRepository);
		TaskExecution actualTaskExecution = TestUtils.getTaskExecutionFromDB(
				db, expectedTaskExecution.getExecutionId());
		TestUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	public void testUpdateTaskExecution() {
		TaskExecution expectedTaskExecution =
				TestUtils.createAndStoreTaskExecutionNoParams(taskRepository);
		expectedTaskExecution = TestUtils.updateTaskExecution(taskRepository,
				expectedTaskExecution.getExecutionId());
		TaskExecution actualTaskExecution = TestUtils.getTaskExecutionFromDB(
				db, expectedTaskExecution.getExecutionId());
		TestUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}


}

