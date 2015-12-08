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

import ch.qos.logback.core.Appender;
import org.aspectj.lang.JoinPoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.cloud.task.configuration.TaskHandler;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.util.TestDefaultConfiguration;
import org.springframework.cloud.task.util.TestVerifierUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Verifies that the TaskHandler Methods record the appropriate log header entries and
 * result codes.
 *
 * @author Glenn Renfro
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestDefaultConfiguration.class, PropertyPlaceholderAutoConfiguration.class})
public class TaskHandlerDefaultTests {

	@Autowired
	private TaskHandler taskHandler;

	@Autowired
	private JoinPoint joinPoint;

	@Test
	public void testTaskException() {
		taskHandler.beforeCommandLineRunner(joinPoint);
		final Appender mockAppender = TestVerifierUtils.getMockAppender();
		taskHandler.logExceptionCommandLineRunner(joinPoint);
		TestVerifierUtils.verifyLogEntryExists(mockAppender,
				"Updating: TaskExecution{executionId='" +
						taskHandler.getTaskExecution().getExecutionId());
		TaskExecution taskExecution = taskHandler.getTaskExecution();
		assertEquals("exit code for exception should be 1", taskExecution.getExitCode(),
				1);
	}

	@Test
	public void testTaskCreate() {
		final Appender mockAppender = TestVerifierUtils.getMockAppender();
		taskHandler.beforeCommandLineRunner(joinPoint);
		TestVerifierUtils.verifyLogEntryExists(mockAppender,
				"Creating: TaskExecution{executionId='" +
						taskHandler.getTaskExecution().getExecutionId());
		assertEquals("Create should report that exit code is zero",
				0, taskHandler.getTaskExecution().getExitCode());

	}

	@Test
	public void testTaskUpdate() {
		taskHandler.beforeCommandLineRunner(joinPoint);
		final Appender mockAppender = TestVerifierUtils.getMockAppender();
		taskHandler.afterReturnCommandLineRunner(joinPoint);
		TestVerifierUtils.verifyLogEntryExists(mockAppender,
				"Updating: TaskExecution{executionId='" +
						taskHandler.getTaskExecution().getExecutionId());
		assertEquals("Update should report that exit code is zero",
				0, taskHandler.getTaskExecution().getExitCode());
	}

}
