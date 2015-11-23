/*
 *
 *  * Copyright 2015 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.cloud.task;

import ch.qos.logback.core.Appender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.util.LoggerTestUtils;
import org.springframework.cloud.task.util.TestDefaultConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Verifies that the LoggerRepository has correct prefixes written to logs.
 * @author Glenn Renfro
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestDefaultConfiguration.class)
public class LoggerTaskRepositoryTests {

	@Autowired
	TaskRepository taskRepository;

	TaskExecution taskExecution;

	@Before
	public void setup(){
		taskExecution = new TaskExecution();
	}

	@Test
	public void testCreateTaskExecution() {
		final Appender mockAppender = LoggerTestUtils.getMockAppender();
		taskRepository.createTaskExecution(taskExecution);
		LoggerTestUtils.verifyLogEntryExists(mockAppender, "Creating: TaskExecution{executionId=");
	}

	@Test
	public void testTaskUpdate() {
		final Appender mockAppender = LoggerTestUtils.getMockAppender();
		taskRepository.update(taskExecution);
		LoggerTestUtils.verifyLogEntryExists(mockAppender, "Updating: TaskExecution{executionId=");
	}

}
