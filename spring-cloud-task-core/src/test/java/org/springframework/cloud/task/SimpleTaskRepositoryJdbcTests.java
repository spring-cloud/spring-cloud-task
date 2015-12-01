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

package org.springframework.cloud.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.JdbcTaskRepositoryFactoryBean;
import org.springframework.cloud.task.util.JdbcTestUtils;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

/**
 * Tests for the SimpleTaskRepository that uses JDBC as a datastore.
 * @author Glenn Renfro.
 */
public class SimpleTaskRepositoryJdbcTests {

	private EmbeddedDatabase db;


	private TaskRepository taskRepository;
	private TaskExecution taskExecution;

	@Before
	public void setUp() {
		// creates an HSQL in-memory database populated from default scripts
		// classpath:schema.sql and classpath:data.sql
		db = new EmbeddedDatabaseBuilder()
				.generateUniqueName(true)
				.addScripts("task-schema.sql")
				.build();
		JdbcTaskRepositoryFactoryBean factoryBean =
				new JdbcTaskRepositoryFactoryBean(db);

		int exitCode = 55;
		Date startTime = new Date();
		Date endTime = new Date();
		taskRepository = factoryBean.getObject();
		String executionId = UUID.randomUUID().toString();
		String taskName = UUID.randomUUID().toString();
		String exitMessage = UUID.randomUUID().toString();
		String statusCode = UUID.randomUUID().toString().substring(0,9);

		taskExecution = new TaskExecution(executionId, exitCode, taskName,
				startTime, endTime,  statusCode,
				exitMessage, new ArrayList<String>());
	}

	@Test
	public void testSingleTaskExecutionNoParam() {
		taskRepository.createTaskExecution(taskExecution);
		JdbcTestUtils.verifyTaskExecution(db, taskExecution);
	}

	@After
	public void tearDown() {
		db.shutdown();
	}
}
