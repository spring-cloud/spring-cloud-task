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

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.annotation.EnableTask;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.util.TestDBUtils;
import org.springframework.cloud.task.util.TestVerifierUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DuplicateKeyException;

/**
 * Executes unit tests on JdbcTaskExecutionDao.
 *
 * @author Glenn Renfro
 */
public class JdbcTaskExecutionDaoTests {

	private DataSource dataSource;

	private AnnotationConfigApplicationContext context;

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		dataSource = this.context.getBean(DataSource.class);
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void saveTaskExecution() {
		JdbcTaskExecutionDao dao = new JdbcTaskExecutionDao(dataSource);
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoParam();
		dao.saveTaskExecution(expectedTaskExecution);

		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				TestDBUtils.getTaskExecutionFromDB(dataSource, expectedTaskExecution.getExecutionId()));
	}

	@Test(expected = DuplicateKeyException.class)
	public void duplicateSaveTaskExecution() {
		JdbcTaskExecutionDao dao = new JdbcTaskExecutionDao(dataSource);
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoParam();
		dao.saveTaskExecution(expectedTaskExecution);
		dao.saveTaskExecution(expectedTaskExecution);
	}

	@Test
	public void updateTaskExecution() {
		JdbcTaskExecutionDao dao = new JdbcTaskExecutionDao(dataSource);

		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoParam();
		dao.saveTaskExecution(expectedTaskExecution);
		dao.updateTaskExecution(expectedTaskExecution);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				TestDBUtils.getTaskExecutionFromDB(dataSource, expectedTaskExecution.getExecutionId()));
	}

	@Test(expected = IllegalStateException.class)
	public void updateTaskExecutionWithNoCreate() {
		JdbcTaskExecutionDao dao = new JdbcTaskExecutionDao(dataSource);

		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoParam();
		dao.updateTaskExecution(expectedTaskExecution);
	}

	@EnableTask
	protected static class TestConfiguration {
	}
}
