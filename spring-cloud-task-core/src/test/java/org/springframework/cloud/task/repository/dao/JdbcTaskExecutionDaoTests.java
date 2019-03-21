/*
 * Copyright 2015 the original author or authors.
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

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.configuration.TestConfiguration;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.util.TestDBUtils;
import org.springframework.cloud.task.util.TestVerifierUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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

	@Before
	public void setup(){
		dao = new JdbcTaskExecutionDao(dataSource);
		dao.setTaskIncrementer(TestDBUtils.getIncrementer(dataSource));
	}

	@Test
	@DirtiesContext
	public void saveTaskExecution() {
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoArg();
		expectedTaskExecution = dao.createTaskExecution(expectedTaskExecution.getTaskName(), expectedTaskExecution.getStartTime(),
				expectedTaskExecution.getArguments());

		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				TestDBUtils.getTaskExecutionFromDB(dataSource, expectedTaskExecution.getExecutionId()));
	}

	@Test
	@DirtiesContext
	public void completeTaskExecution() {
		TaskExecution expectedTaskExecution = TestVerifierUtils.endSampleTaskExecutionNoArg();
		expectedTaskExecution = dao.createTaskExecution(expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments());
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
}
