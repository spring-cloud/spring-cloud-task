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

import ch.qos.logback.core.Appender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.util.TaskExecutionCreator;
import org.springframework.cloud.task.util.TestDefaultConfiguration;
import org.springframework.cloud.task.util.TestVerifierUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Verifies that the SimpleTaskRepository has correct prefixes written to logs.
 * @author Glenn Renfro
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestDefaultConfiguration.class)
public class SimpleTaskRepositoryLoggerTests {

	@Autowired
	private TaskRepository taskRepository;

	@Test
	public void testCreateTaskExecution() {
		final Appender mockAppender = TestVerifierUtils.getMockAppender();
		TaskExecution expectedTaskExecution =
				TaskExecutionCreator.createAndStoreTaskExecutionNoParams(taskRepository);
		TestVerifierUtils.verifyLogEntryExists(mockAppender,
				"Creating: TaskExecution{executionId='" + expectedTaskExecution.getExecutionId());
	}

	@Test
	public void testTaskUpdate() {
		final Appender mockAppender = TestVerifierUtils.getMockAppender();
		TaskExecution expectedTaskExecution =
				TaskExecutionCreator.createAndStoreTaskExecutionNoParams(taskRepository);
		TaskExecutionCreator.updateTaskExecution(taskRepository,
				expectedTaskExecution.getExecutionId());
		TestVerifierUtils.verifyLogEntryExists(mockAppender,
				"Updating: TaskExecution{executionId='"
						+ expectedTaskExecution.getExecutionId());
	}

}
