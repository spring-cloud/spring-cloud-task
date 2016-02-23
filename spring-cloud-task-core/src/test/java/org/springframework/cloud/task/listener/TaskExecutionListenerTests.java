/*
 * Copyright 2016 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.util.TestDefaultConfiguration;
import org.springframework.cloud.task.util.TestDefaultListenerConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

/**
 * Verifies that the TaskExecutionListener invocations occur at the appropriate task
 * lifecycle stages.
 *
 * @author Glenn Renfro
 */
public class TaskExecutionListenerTests {
	private AnnotationConfigApplicationContext context;

	private static final String EXCEPTION_MESSAGE = "This was expected";

	@Before
	public void setUp() {
		context = new AnnotationConfigApplicationContext();
		context.setId("testTask");
		context.register(TestDefaultListenerConfiguration.class,
				TestDefaultConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
	}

	@After
	public void tearDown() {
		context.close();
	}

	@Test
	public void testTaskCreate() {
		context.refresh();
		TestDefaultListenerConfiguration.TestTaskExecutionListener taskExecutionListener =
				context.getBean(TestDefaultListenerConfiguration.TestTaskExecutionListener.class);
		TaskExecution taskExecution = new TaskExecution(0, null, "wombat",
				new Date(), new Date(), null, new ArrayList<String>());
		verifyListenerResults(true, false, false, taskExecution,taskExecutionListener);
	}

	@Test
	public void testTaskUpdate() {
		context.refresh();
		TestDefaultListenerConfiguration.TestTaskExecutionListener taskExecutionListener =
				context.getBean(TestDefaultListenerConfiguration.TestTaskExecutionListener.class);
		context.publishEvent(new ContextClosedEvent(context));

		TaskExecution taskExecution = new TaskExecution(0, 0, "wombat",
				new Date(), new Date(), null, new ArrayList<String>());
		verifyListenerResults(true, true, false, taskExecution,taskExecutionListener);
	}

	@Test
	public void testTaskFail() {
		RuntimeException exception = new RuntimeException(EXCEPTION_MESSAGE);
		context.refresh();
		context.publishEvent(new ApplicationFailedEvent(new SpringApplication(), new String[0], context, exception));
		context.publishEvent(new ContextClosedEvent(context));
		TestDefaultListenerConfiguration.TestTaskExecutionListener taskExecutionListener =
				context.getBean(TestDefaultListenerConfiguration.TestTaskExecutionListener.class);

		TaskExecution taskExecution = new TaskExecution(0, 1, "wombat", new Date(),
				new Date(), null, new ArrayList<String>());
		verifyListenerResults(true, true, true, taskExecution,taskExecutionListener);
	}

	private void verifyListenerResults (boolean isTaskStartup, boolean isTaskEnd,
				boolean isTaskFailed, TaskExecution taskExecution,
				TestDefaultListenerConfiguration.TestTaskExecutionListener actualListener){
		assertEquals(isTaskStartup,actualListener.isTaskStartup());
		assertEquals(isTaskEnd,actualListener.isTaskEnd());
		assertEquals(isTaskFailed,actualListener.isTaskFailed());
		if(isTaskFailed){
			assertEquals(TestDefaultListenerConfiguration.TestTaskExecutionListener.END_MESSAGE, actualListener.getTaskExecution().getExitMessage());
			assertNotNull(actualListener.getThrowable());
			assertTrue(actualListener.getThrowable() instanceof RuntimeException);
		}
		else if(isTaskEnd){
			assertEquals(TestDefaultListenerConfiguration.TestTaskExecutionListener.END_MESSAGE, actualListener.getTaskExecution().getExitMessage());
			assertNull(actualListener.getThrowable());
		}
		else {
			assertEquals(TestDefaultListenerConfiguration.TestTaskExecutionListener.START_MESSAGE, actualListener.getTaskExecution().getExitMessage());
			assertNull(actualListener.getThrowable());
		}

		assertEquals(taskExecution.getExecutionId(), actualListener.getTaskExecution().getExecutionId());
		assertEquals(taskExecution.getExitCode(), actualListener.getTaskExecution().getExitCode());
	}
}
