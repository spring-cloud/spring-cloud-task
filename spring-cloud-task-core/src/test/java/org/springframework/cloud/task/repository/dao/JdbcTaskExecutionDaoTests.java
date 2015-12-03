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

package org.springframework.cloud.task.repository.dao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.util.TestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

/**
 * Executes unit tests on JdbcTaskExecutionDao.
 * @author Glenn Renfro
 */
public class JdbcTaskExecutionDaoTests {

	private EmbeddedDatabase db;

	private JdbcTemplate jdbcTemplate;

	@Before
	public void setup() {
		db = new EmbeddedDatabaseBuilder()
				.generateUniqueName(true)
				.addScripts("task-schema.sql")
				.build();

 		jdbcTemplate = new JdbcTemplate(db) ;
	}


	@After
	public void tearDown() {
		db.shutdown();
	}

	@Test
	public void saveTaskExecution(){
		JdbcTaskExecutionDao dao = new JdbcTaskExecutionDao(jdbcTemplate);
		TaskExecution expectedTaskExecution = TestUtils.createSampleTaskExecutionNoParam();
		dao.saveTaskExecution(expectedTaskExecution);

		TestUtils.verifyTaskExecution(expectedTaskExecution,
				TestUtils.getTaskExecutionFromDB(db, expectedTaskExecution.getExecutionId()));
	}

	@Test
	public void updateTaskExecution(){
		JdbcTaskExecutionDao dao = new JdbcTaskExecutionDao(jdbcTemplate);

		TaskExecution expectedTaskExecution = TestUtils.createSampleTaskExecutionNoParam();
		dao.saveTaskExecution(expectedTaskExecution);
		dao.updateTaskExecution(expectedTaskExecution);
		TestUtils.verifyTaskExecution(expectedTaskExecution,
				TestUtils.getTaskExecutionFromDB(db, expectedTaskExecution.getExecutionId()));
	}

}
