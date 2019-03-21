/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.task.listener;

import java.util.ArrayList;
import java.util.Date;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.task.listener.annotation.AfterTask;
import org.springframework.cloud.task.listener.annotation.BeforeTask;
import org.springframework.cloud.task.listener.annotation.FailedTask;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.util.TestDefaultConfiguration;
import org.springframework.cloud.task.util.TestListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the TaskExecutionListener invocations occur at the appropriate task
 * lifecycle stages.
 *
 * @author Glenn Renfro
 */
public class TaskExecutionListenerTests {

	private static final String EXCEPTION_MESSAGE = "This was expected";

	private static boolean beforeTaskDidFireOnError = false;

	private static boolean endTaskDidFireOnError = false;

	private static boolean failedTaskDidFireOnError = false;

	private AnnotationConfigApplicationContext context;

	@BeforeTask
	public void setup() {
		beforeTaskDidFireOnError = false;
		endTaskDidFireOnError = false;
		failedTaskDidFireOnError = false;
	}

	@After
	public void tearDown() {
		if (this.context != null && this.context.isActive()) {
			this.context.close();
		}
	}

	/**
	 * Verify that if a TaskExecutionListener Bean is present that the onTaskStartup
	 * method is called.
	 */
	@Test
	public void testTaskCreate() {
		setupContextForTaskExecutionListener();
		DefaultTaskListenerConfiguration.TestTaskExecutionListener taskExecutionListener = this.context
				.getBean(
						DefaultTaskListenerConfiguration.TestTaskExecutionListener.class);
		TaskExecution taskExecution = new TaskExecution(0, null, "wombat", new Date(),
				new Date(), null, new ArrayList<>(), null, null);
		verifyListenerResults(false, false, taskExecution, taskExecutionListener);
	}

	/**
	 * Verify that if a LifecycleProcessor executes all TaskExecutionListeners if
	 * BeforeTask throws exception.
	 */
	@Test
	public void testBeforeTaskErrorCreate() {
		boolean exceptionFired = false;
		try {
			setupContextForBeforeTaskErrorAnnotatedListener();
		}
		catch (Exception exception) {
			exceptionFired = true;
		}
		assertThat(exceptionFired).as("Exception should have fired").isTrue();
		assertThat(beforeTaskDidFireOnError)
				.as("BeforeTask Listener should have executed").isTrue();
		assertThat(endTaskDidFireOnError).as("EndTask Listener should have executed")
				.isTrue();
		assertThat(failedTaskDidFireOnError)
				.as("FailedTask Listener should have executed").isTrue();
	}

	/**
	 * Verify that if a LifecycleProcessor executes AfterTask TaskExecutionListeners if
	 * FailedTask throws exception.
	 */
	@Test
	public void testFailedTaskErrorCreate() {
		boolean exceptionFired = false;
		try {
			setupContextForFailedTaskErrorAnnotatedListener();
		}
		catch (Exception exception) {
			exceptionFired = true;
		}
		assertThat(exceptionFired).as("Exception should have fired").isTrue();
		assertThat(endTaskDidFireOnError).as("EndTask Listener should have executed")
				.isTrue();
		assertThat(failedTaskDidFireOnError)
				.as("FailedTask Listener should not have executed").isTrue();
	}

	/**
	 * Verify that if a LifecycleProcessor stores the correct exit code if AfterTask
	 * listener fails.
	 */
	@Test
	public void testAfterTaskErrorCreate() {
		setupContextForAfterTaskErrorAnnotatedListener();
		AfterTaskErrorAnnotationConfiguration.AnnotatedTaskListener taskExecutionListener = this.context
				.getBean(
						AfterTaskErrorAnnotationConfiguration.AnnotatedTaskListener.class);
		this.context.publishEvent(new ApplicationReadyEvent(new SpringApplication(),
				new String[0], this.context));

		assertThat(taskExecutionListener.isTaskStartup()).isTrue();
		assertThat(taskExecutionListener.isTaskEnd()).isTrue();
		assertThat(taskExecutionListener.getTaskExecution().getExitMessage())
				.isEqualTo(TestListener.END_MESSAGE);
		assertThat(taskExecutionListener.getTaskExecution().getErrorMessage().contains(
				"Failed to process @BeforeTask or @AfterTask annotation because: AfterTaskFailure"))
						.isTrue();
		assertThat(taskExecutionListener.getThrowable()).isNull();
	}

	/**
	 * Verify that if a TaskExecutionListener Bean is present that the onTaskEnd method is
	 * called.
	 */
	@Test
	public void testTaskUpdate() {
		setupContextForTaskExecutionListener();
		DefaultTaskListenerConfiguration.TestTaskExecutionListener taskExecutionListener = this.context
				.getBean(
						DefaultTaskListenerConfiguration.TestTaskExecutionListener.class);
		this.context.publishEvent(new ApplicationReadyEvent(new SpringApplication(),
				new String[0], this.context));

		TaskExecution taskExecution = new TaskExecution(0, 0, "wombat", new Date(),
				new Date(), null, new ArrayList<>(), null, null);
		verifyListenerResults(true, false, taskExecution, taskExecutionListener);
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
		DefaultTaskListenerConfiguration.TestTaskExecutionListener taskExecutionListener = this.context
				.getBean(
						DefaultTaskListenerConfiguration.TestTaskExecutionListener.class);
		this.context.publishEvent(new ApplicationFailedEvent(application, new String[0],
				this.context, exception));
		this.context.publishEvent(
				new ApplicationReadyEvent(application, new String[0], this.context));

		TaskExecution taskExecution = new TaskExecution(0, 1, "wombat", new Date(),
				new Date(), null, new ArrayList<>(), null, null);
		verifyListenerResults(true, true, taskExecution, taskExecutionListener);
	}

	/**
	 * Verify that if a bean has a @BeforeTask annotation present that the associated
	 * method is called.
	 */
	@Test
	public void testAnnotationCreate() {
		setupContextForAnnotatedListener();
		DefaultAnnotationConfiguration.AnnotatedTaskListener annotatedListener = this.context
				.getBean(DefaultAnnotationConfiguration.AnnotatedTaskListener.class);
		TaskExecution taskExecution = new TaskExecution(0, null, "wombat", new Date(),
				new Date(), null, new ArrayList<>(), null, null);
		verifyListenerResults(false, false, taskExecution, annotatedListener);
	}

	/**
	 * Verify that if a bean has a @AfterTask annotation present that the associated
	 * method is called.
	 */
	@Test
	public void testAnnotationUpdate() {
		setupContextForAnnotatedListener();
		DefaultAnnotationConfiguration.AnnotatedTaskListener annotatedListener = this.context
				.getBean(DefaultAnnotationConfiguration.AnnotatedTaskListener.class);
		this.context.publishEvent(new ApplicationReadyEvent(new SpringApplication(),
				new String[0], this.context));

		TaskExecution taskExecution = new TaskExecution(0, 0, "wombat", new Date(),
				new Date(), null, new ArrayList<>(), null, null);
		verifyListenerResults(true, false, taskExecution, annotatedListener);
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
		DefaultAnnotationConfiguration.AnnotatedTaskListener annotatedListener = this.context
				.getBean(DefaultAnnotationConfiguration.AnnotatedTaskListener.class);
		this.context.publishEvent(new ApplicationFailedEvent(application, new String[0],
				this.context, exception));
		this.context.publishEvent(
				new ApplicationReadyEvent(application, new String[0], this.context));

		TaskExecution taskExecution = new TaskExecution(0, 1, "wombat", new Date(),
				new Date(), null, new ArrayList<>(), null, null);
		verifyListenerResults(true, true, taskExecution, annotatedListener);
	}

	private void verifyListenerResults(boolean isTaskEnd, boolean isTaskFailed,
			TaskExecution taskExecution, TestListener actualListener) {
		assertThat(actualListener.isTaskStartup()).isTrue();
		assertThat(actualListener.isTaskEnd()).isEqualTo(isTaskEnd);
		assertThat(actualListener.isTaskFailed()).isEqualTo(isTaskFailed);
		if (isTaskFailed) {
			assertThat(actualListener.getTaskExecution().getExitMessage())
					.isEqualTo(TestListener.END_MESSAGE);
			assertThat(actualListener.getThrowable()).isNotNull();
			assertThat(actualListener.getThrowable() instanceof RuntimeException)
					.isTrue();
			assertThat(actualListener.getTaskExecution().getErrorMessage()
					.startsWith("java.lang.RuntimeException: This was expected"))
							.isTrue();
		}
		else if (isTaskEnd) {
			assertThat(actualListener.getTaskExecution().getExitMessage())
					.isEqualTo(TestListener.END_MESSAGE);
			assertThat(actualListener.getTaskExecution().getErrorMessage())
					.isEqualTo(taskExecution.getErrorMessage());
			assertThat(actualListener.getThrowable()).isNull();
		}
		else {
			assertThat(actualListener.getTaskExecution().getExitMessage())
					.isEqualTo(TestListener.START_MESSAGE);
			assertThat(actualListener.getTaskExecution().getErrorMessage()).isNull();
			assertThat(actualListener.getThrowable()).isNull();
		}

		assertThat(actualListener.getTaskExecution().getExecutionId())
				.isEqualTo(taskExecution.getExecutionId());
		assertThat(actualListener.getTaskExecution().getExitCode())
				.isEqualTo(taskExecution.getExitCode());
		assertThat(actualListener.getTaskExecution().getExternalExecutionId())
				.isEqualTo(taskExecution.getExternalExecutionId());
	}

	private void setupContextForTaskExecutionListener() {
		this.context = new AnnotationConfigApplicationContext(
				DefaultTaskListenerConfiguration.class, TestDefaultConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.setId("testTask");
	}

	private void setupContextForAnnotatedListener() {
		this.context = new AnnotationConfigApplicationContext(
				TestDefaultConfiguration.class, DefaultAnnotationConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.setId("annotatedTask");
	}

	private void setupContextForBeforeTaskErrorAnnotatedListener() {
		this.context = new AnnotationConfigApplicationContext(
				TestDefaultConfiguration.class,
				BeforeTaskErrorAnnotationConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.setId("beforeTaskAnnotatedTask");
	}

	private void setupContextForFailedTaskErrorAnnotatedListener() {
		this.context = new AnnotationConfigApplicationContext(
				TestDefaultConfiguration.class,
				FailedTaskErrorAnnotationConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.setId("failedTaskAnnotatedTask");
	}

	private void setupContextForAfterTaskErrorAnnotatedListener() {
		this.context = new AnnotationConfigApplicationContext(
				TestDefaultConfiguration.class,
				AfterTaskErrorAnnotationConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.setId("afterTaskAnnotatedTask");
	}

	@Configuration
	public static class DefaultAnnotationConfiguration {

		@Bean
		public AnnotatedTaskListener annotatedTaskListener() {
			return new AnnotatedTaskListener();
		}

		public static class AnnotatedTaskListener extends TestListener {

			@BeforeTask
			public void methodA(TaskExecution taskExecution) {
				this.isTaskStartup = true;
				this.taskExecution = taskExecution;
				this.taskExecution.setExitMessage(START_MESSAGE);
			}

			@AfterTask
			public void methodB(TaskExecution taskExecution) {
				this.isTaskEnd = true;
				this.taskExecution = taskExecution;
				this.taskExecution.setExitMessage(END_MESSAGE);

			}

			@FailedTask
			public void methodC(TaskExecution taskExecution, Throwable throwable) {
				this.isTaskFailed = true;
				this.taskExecution = taskExecution;
				this.throwable = throwable;
				this.taskExecution.setExitMessage(ERROR_MESSAGE);
			}

		}

	}

	@Configuration
	public static class BeforeTaskErrorAnnotationConfiguration {

		@Bean
		public AnnotatedTaskListener annotatedTaskListener() {
			return new AnnotatedTaskListener();
		}

		@Bean
		public CommandLineRunner commandLineRunner() {
			return args -> System.out.println("I was run");
		}

		public static class AnnotatedTaskListener {

			@BeforeTask
			public void methodA(TaskExecution taskExecution) {
				beforeTaskDidFireOnError = true;
				throw new TaskExecutionException("BeforeTaskFailure");
			}

			@AfterTask
			public void methodB(TaskExecution taskExecution) {
				endTaskDidFireOnError = true;
			}

			@FailedTask
			public void methodC(TaskExecution taskExecution, Throwable throwable) {
				failedTaskDidFireOnError = true;
			}

		}

	}

	@Configuration
	public static class FailedTaskErrorAnnotationConfiguration {

		@Bean
		public AnnotatedTaskListener annotatedTaskListener() {
			return new AnnotatedTaskListener();
		}

		public static class AnnotatedTaskListener {

			@BeforeTask
			public void methodA(TaskExecution taskExecution) {
				beforeTaskDidFireOnError = true;
				throw new TaskExecutionException("BeforeTaskFailure");
			}

			@AfterTask
			public void methodB(TaskExecution taskExecution) {
				endTaskDidFireOnError = true;
			}

			@FailedTask
			public void methodC(TaskExecution taskExecution, Throwable throwable) {
				failedTaskDidFireOnError = true;
				throw new TaskExecutionException("FailedTaskFailure");
			}

		}

	}

	@Configuration
	public static class AfterTaskErrorAnnotationConfiguration {

		@Bean
		public AnnotatedTaskListener annotatedTaskListener() {
			return new AnnotatedTaskListener();
		}

		public static class AnnotatedTaskListener extends TestListener {

			@BeforeTask
			public void methodA(TaskExecution taskExecution) {
				this.isTaskStartup = true;
			}

			@AfterTask
			public void methodB(TaskExecution taskExecution) {
				this.isTaskEnd = true;
				this.taskExecution = taskExecution;
				this.taskExecution.setExitMessage(END_MESSAGE);
				throw new TaskExecutionException("AfterTaskFailure");
			}

		}

	}

	@Configuration
	public static class DefaultTaskListenerConfiguration {

		@Bean
		public TestTaskExecutionListener taskExecutionListener() {
			return new TestTaskExecutionListener();
		}

		public static class TestTaskExecutionListener extends TestListener
				implements TaskExecutionListener {

			@Override
			public void onTaskStartup(TaskExecution taskExecution) {
				this.isTaskStartup = true;
				this.taskExecution = taskExecution;
				this.taskExecution.setExitMessage(START_MESSAGE);
			}

			@Override
			public void onTaskEnd(TaskExecution taskExecution) {
				this.isTaskEnd = true;
				this.taskExecution = taskExecution;
				this.taskExecution.setExitMessage(END_MESSAGE);
			}

			@Override
			public void onTaskFailed(TaskExecution taskExecution, Throwable throwable) {
				this.isTaskFailed = true;
				this.taskExecution = taskExecution;
				this.throwable = throwable;
				this.taskExecution.setExitMessage(ERROR_MESSAGE);
			}

		}

	}

}
