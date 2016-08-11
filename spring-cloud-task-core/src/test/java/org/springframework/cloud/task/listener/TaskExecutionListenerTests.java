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

import java.util.ArrayList;
import java.util.Date;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.task.listener.annotation.AfterTask;
import org.springframework.cloud.task.listener.annotation.BeforeTask;
import org.springframework.cloud.task.listener.annotation.FailedTask;
import org.springframework.cloud.task.listener.annotation.TaskListenerExecutorFactoryBean;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.util.TestDefaultConfiguration;
import org.springframework.cloud.task.util.TestListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the TaskExecutionListener invocations occur at the appropriate task
 * lifecycle stages.
 *
 * @author Glenn Renfro
 */
public class TaskExecutionListenerTests {
	private AnnotationConfigApplicationContext context;

	private static final String EXCEPTION_MESSAGE = "This was expected";

	@After
	public void tearDown() {
		if(context != null && context.isActive()) {
			context.close();
		}
	}

	/**
	 * Verify that if a TaskExecutionListener Bean is present that the onTaskStartup method
	 * is called.
	 */
	@Test
	public void testTaskCreate() {
		setupContextForTaskExecutionListener();
		DefaultTaskListenerConfiguration.TestTaskExecutionListener taskExecutionListener =
				context.getBean(DefaultTaskListenerConfiguration.TestTaskExecutionListener.class);
		TaskExecution taskExecution = new TaskExecution(0, null, "wombat",
				new Date(), new Date(), null, new ArrayList<String>(), null);
		verifyListenerResults(true, false, false, taskExecution,taskExecutionListener);
	}

	/**
	 * Verify that if a TaskExecutionListener Bean is present that the onTaskEnd method
	 * is called.
	 */
	@Test
	public void testTaskUpdate() {
		setupContextForTaskExecutionListener();
		DefaultTaskListenerConfiguration.TestTaskExecutionListener taskExecutionListener =
				context.getBean(DefaultTaskListenerConfiguration.TestTaskExecutionListener.class);
		context.publishEvent(new ApplicationReadyEvent(new SpringApplication(), new String[0], context));

		TaskExecution taskExecution = new TaskExecution(0, 0, "wombat",
				new Date(), new Date(), null, new ArrayList<String>(), null);
		verifyListenerResults(true, true, false, taskExecution,taskExecutionListener);
	}

	/**
	 * Verify that if a TaskExecutionListener Bean is present that the onTaskFailed method
	 * is called.
	 */
	@Test
	public void testTaskFail() {
		RuntimeException exception = new RuntimeException(EXCEPTION_MESSAGE);
		setupContextForTaskExecutionListener();
		SpringApplication application = new SpringApplication();
		DefaultTaskListenerConfiguration.TestTaskExecutionListener taskExecutionListener =
				context.getBean(DefaultTaskListenerConfiguration.TestTaskExecutionListener.class);
		context.publishEvent(new ApplicationFailedEvent(application, new String[0], context, exception));
		context.publishEvent(new ApplicationReadyEvent(application, new String[0], context));

		TaskExecution taskExecution = new TaskExecution(0, 1, "wombat", new Date(),
				new Date(), null, new ArrayList<String>(), null);
		verifyListenerResults(true, true, true, taskExecution,taskExecutionListener);
	}

	/**
	 * Verify that if a bean has a @BeforeTask annotation present that the associated
	 * method is called.
	 */
	@Test
	public void testAnnotationCreate() throws Exception {
		setupContextForAnnotatedListener();
		DefaultAnnotationConfiguration.AnnotatedTaskListener annotatedListener =
				context.getBean(DefaultAnnotationConfiguration.AnnotatedTaskListener.class);
		TaskExecution taskExecution = new TaskExecution(0, null, "wombat",
				new Date(), new Date(), null, new ArrayList<String>(), null);
		verifyListenerResults(true, false, false, taskExecution,annotatedListener);
	}

	/**
	 * Verify that if a bean has a @AfterTask annotation present that the associated
	 * method is called.
	 */
	@Test
	public void testAnnotationUpdate() {
		setupContextForAnnotatedListener();
		DefaultAnnotationConfiguration.AnnotatedTaskListener annotatedListener =
				context.getBean(DefaultAnnotationConfiguration.AnnotatedTaskListener.class);
		context.publishEvent(new ApplicationReadyEvent(new SpringApplication(), new String[0], context));

		TaskExecution taskExecution = new TaskExecution(0, 0, "wombat",
				new Date(), new Date(), null, new ArrayList<String>(), null);
		verifyListenerResults(true, true, false, taskExecution,annotatedListener);
	}

	/**
	 * Verify that if a bean has a @FailedTask annotation present that the associated
	 * method is called.
	 */
	@Test
	public void testAnnotationFail() {
		RuntimeException exception = new RuntimeException(EXCEPTION_MESSAGE);
		setupContextForAnnotatedListener();
		SpringApplication application = new SpringApplication();
		DefaultAnnotationConfiguration.AnnotatedTaskListener annotatedListener =
				context.getBean(DefaultAnnotationConfiguration.AnnotatedTaskListener.class);
		context.publishEvent(new ApplicationFailedEvent(application, new String[0], context, exception));
		context.publishEvent(new ApplicationReadyEvent(application, new String[0], context));

		TaskExecution taskExecution = new TaskExecution(0, 1, "wombat", new Date(),
				new Date(), null, new ArrayList<String>(), null);
		verifyListenerResults(true, true, true, taskExecution,annotatedListener);
	}

	private void verifyListenerResults (boolean isTaskStartup, boolean isTaskEnd,
				boolean isTaskFailed, TaskExecution taskExecution,
				TestListener actualListener){
		assertEquals(isTaskStartup,actualListener.isTaskStartup());
		assertEquals(isTaskEnd,actualListener.isTaskEnd());
		assertEquals(isTaskFailed,actualListener.isTaskFailed());
		if(isTaskFailed){
			assertEquals(TestListener.END_MESSAGE, actualListener.getTaskExecution().getExitMessage());
			assertNotNull(actualListener.getThrowable());
			assertTrue(actualListener.getThrowable() instanceof RuntimeException);
			assertTrue(actualListener.getTaskExecution().getErrorMessage().startsWith("java.lang.RuntimeException: This was expected"));
		}
		else if(isTaskEnd){
			assertEquals(TestListener.END_MESSAGE, actualListener.getTaskExecution().getExitMessage());
			assertNull(actualListener.getTaskExecution().getErrorMessage());
			assertNull(actualListener.getThrowable());
		}
		else {
			assertEquals(TestListener.START_MESSAGE, actualListener.getTaskExecution().getExitMessage());
			assertNull(actualListener.getTaskExecution().getErrorMessage());
			assertNull(actualListener.getThrowable());
		}

		assertEquals(taskExecution.getExecutionId(), actualListener.getTaskExecution().getExecutionId());
		assertEquals(taskExecution.getExitCode(), actualListener.getTaskExecution().getExitCode());
	}

	private void setupContextForTaskExecutionListener(){
		context = new AnnotationConfigApplicationContext(DefaultTaskListenerConfiguration.class,
				TestDefaultConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		context.setId("testTask");
	}

	private void setupContextForAnnotatedListener(){
		context = new AnnotationConfigApplicationContext(TestDefaultConfiguration.class, DefaultAnnotationConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		context.setId("annotatedTask");
	}

	@Configuration
	public static class DefaultAnnotationConfiguration {

		@Bean
		public AnnotatedTaskListener annotatedTaskListener() {
			return new AnnotatedTaskListener();
		}

		@Bean
		public TaskListenerExecutorFactoryBean taskListenerExecutor(ConfigurableApplicationContext context) throws Exception
		{
			return new TaskListenerExecutorFactoryBean(context);
		}

		public static class AnnotatedTaskListener extends TestListener {

			@BeforeTask
			public void methodA(TaskExecution taskExecution) {
				isTaskStartup = true;
				this.taskExecution = taskExecution;
				this.taskExecution.setExitMessage(START_MESSAGE);
			}

			@AfterTask
			public void methodB(TaskExecution taskExecution) {
				isTaskEnd = true;
				this.taskExecution = taskExecution;
				this.taskExecution.setExitMessage(END_MESSAGE);

			}

			@FailedTask
			public void methodC(TaskExecution taskExecution, Throwable throwable) {
				isTaskFailed = true;
				this.taskExecution = taskExecution;
				this.throwable = throwable;
				this.taskExecution.setExitMessage(ERROR_MESSAGE);
			}
		}
	}

	@Configuration
	public static class DefaultTaskListenerConfiguration {

		@Bean
		public TestTaskExecutionListener taskExecutionListener() {
			return new TestTaskExecutionListener();
		}

		public static class TestTaskExecutionListener extends TestListener implements TaskExecutionListener {

			@Override
			public void onTaskStartup(TaskExecution taskExecution) {
				isTaskStartup = true;
				this.taskExecution = taskExecution;
				this.taskExecution.setExitMessage(START_MESSAGE);
			}

			@Override
			public void onTaskEnd(TaskExecution taskExecution) {
				isTaskEnd = true;
				this.taskExecution = taskExecution;
				this.taskExecution.setExitMessage(END_MESSAGE);
			}

			@Override
			public void onTaskFailed(TaskExecution taskExecution, Throwable throwable) {
				isTaskFailed = true;
				this.taskExecution = taskExecution;
				this.throwable = throwable;
				this.taskExecution.setExitMessage(ERROR_MESSAGE);
			}
		}
	}

}

