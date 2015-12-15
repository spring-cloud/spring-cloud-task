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

package org.springframework.cloud.task.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ch.qos.logback.core.Appender;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.cloud.task.util.TestDefaultConfiguration;
import org.springframework.cloud.task.util.TestVerifierUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

/**
 * Verifies that the TaskLifecycleListener Methods record the appropriate log header entries and
 * result codes.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 */
public class TaskLifecycleListenerTests {

	@Autowired
	private TaskLifecycleListener listener;

	@Test
	public void testTaskCreate() {
		AnnotationConfigApplicationContext createContext = new AnnotationConfigApplicationContext();
		createContext.register(TestDefaultConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
		final Appender mockAppender = TestVerifierUtils.getMockAppender();
		createContext.refresh();
		this.listener = createContext.getBean(TaskLifecycleListener.class);
		TestVerifierUtils.verifyLogEntryExists(mockAppender,
				"Creating: TaskExecution{executionId='" +
						listener.getTaskExecution().getExecutionId());
		assertEquals("Create should report that exit code is zero",
				0, listener.getTaskExecution().getExitCode());

	}

	@Test
	public void testTaskUpdate() {
		AnnotationConfigApplicationContext createContext = new AnnotationConfigApplicationContext();
		createContext.register(TestDefaultConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
		createContext.refresh();
		final Appender mockAppender = TestVerifierUtils.getMockAppender();
		this.listener = createContext.getBean(TaskLifecycleListener.class);
		this.listener.onApplicationEvent(new ContextClosedEvent(createContext));
		TestVerifierUtils.verifyLogEntryExists(mockAppender,
				"Updating: TaskExecution{executionId='" +
						listener.getTaskExecution().getExecutionId());
		assertEquals("Update should report that exit code is zero",
				0, listener.getTaskExecution().getExitCode());
	}

	@Test
	public void testTaskFailedUpdate() {
		AnnotationConfigApplicationContext createContext = new AnnotationConfigApplicationContext();
		createContext.register(TestDefaultConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
		createContext.refresh();
		final Appender mockAppender = TestVerifierUtils.getMockAppender();
		this.listener = createContext.getBean(TaskLifecycleListener.class);
		listener.onApplicationEvent(new ApplicationFailedEvent(new SpringApplication(), new String[]{}, createContext, new RuntimeException("This was expected")));
		TestVerifierUtils.verifyLogEntryExists(mockAppender,
				"Updating: TaskExecution{executionId='" +
						listener.getTaskExecution().getExecutionId());
		assertEquals("Update should report that exit code is zero",
				1, listener.getTaskExecution().getExitCode());
		assertTrue("Stack trace missing from exit message", listener.getTaskExecution().getExitMessage().startsWith("java.lang.RuntimeException: This was expected"));
	}
}
