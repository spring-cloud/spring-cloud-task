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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.JdbcTaskRepositoryFactoryBean;
import org.springframework.cloud.task.util.TestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
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

	@Test
	public void testSingleTaskExecutionNoParam() {
		TaskExecution expectedTaskExecution =
				TestUtils.createAndStoreTaskExecutionNoParams(taskRepository);
		TaskExecution actualTaskExecution = taskExecutionFromDB(db,
				expectedTaskExecution.getExecutionId());
		TestUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	public void testSingleTaskExecutionWithParam() {
		TaskExecution expectedTaskExecution =
				TestUtils.createAndStoreTaskExecutionWithParams(taskRepository);
		TaskExecution actualTaskExecution = taskExecutionFromDB(
				db, expectedTaskExecution.getExecutionId());
		TestUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	public void testUpdateSingleTaskExecution() {
		TaskExecution expectedTaskExecution =
				TestUtils.createAndStoreTaskExecutionNoParams(taskRepository);
		expectedTaskExecution = TestUtils.updateTaskExecution(taskRepository,
				expectedTaskExecution.getExecutionId());
		TaskExecution actualTaskExecution = taskExecutionFromDB(
				db, expectedTaskExecution.getExecutionId());
		TestUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@After
	public void tearDown() {
		db.shutdown();
	}

	private TaskExecution taskExecutionFromDB(DataSource dataSource,
													String taskExecutionId){
		String sql = "SELECT * FROM TASK_EXECUTION WHERE TASK_EXECUTION_ID = '"
				+ taskExecutionId + "'";

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
		assertEquals("only one row should be returned", 1, rows.size());
		TaskExecution taskExecution = new TaskExecution();
		for (Map row : rows) {
			taskExecution.setExecutionId((String)row.get("TASK_EXECUTION_ID"));
			taskExecution.setStartTime((Date) row.get("START_TIME"));
			taskExecution.setEndTime((Date) row.get("END_TIME"));
			taskExecution.setExitCode((Integer)row.get("EXIT_CODE"));
			taskExecution.setExitMessage((String)row.get("EXIT_MESSAGE"));
			taskExecution.setStatusCode((String)row.get("STATUS_CODE"));
			taskExecution.setTaskName((String)row.get("TASK_NAME"));
		}
		populateParams(dataSource, taskExecution);
		return taskExecution;
	}
	private void populateParams(DataSource dataSource, TaskExecution taskExecution){
		String sql = "SELECT * FROM TASK_EXECUTION_PARAMS WHERE TASK_EXECUTION_ID = '"
				+ taskExecution.getExecutionId() + "'";

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
		List<String> params = new ArrayList<>();
		for (Map row : rows) {
			params.add((String) row.get("TASK_PARAM"));
		}
		taskExecution.setParameters(params);
	}

}

