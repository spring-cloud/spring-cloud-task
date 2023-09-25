/*
 * Copyright 2018-2021 the original author or authors.
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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.listener.annotation.AfterTask;
import org.springframework.cloud.task.listener.annotation.BeforeTask;
import org.springframework.cloud.task.listener.annotation.FailedTask;
import org.springframework.cloud.task.listener.annotation.TaskListenerExecutor;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the {@link TaskListenerExecutorObjectFactory} retrieves the
 * {@link TaskListenerExecutor}.
 *
 * @author Glenn Renfro
 * @author Isik Erhan
 * @since 2.1.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { TaskListenerExecutorObjectFactoryTests.TaskExecutionListenerConfiguration.class })
@DirtiesContext
public class TaskListenerExecutorObjectFactoryTests {

	/**
	 * Task name constant for the Before TaskListener tests.
	 */
	public static final String BEFORE_LISTENER = "BEFORE LISTENER";

	/**
	 * Task name constant for the After TaskListener tests.
	 */
	public static final String AFTER_LISTENER = "AFTER LISTENER";

	/**
	 * Task name constant for the Fail TaskListener tests.
	 */
	public static final String FAIL_LISTENER = "FAIL LISTENER";

	/**
	 * Collection of the task execution listeners that were fired.
	 */
	public static List<TaskExecution> taskExecutionListenerResults = new ArrayList<>(3);

	private TaskListenerExecutor taskListenerExecutor;

	private TaskListenerExecutorObjectFactory taskListenerExecutorObjectFactory;

	public void setup(ConfigurableApplicationContext context) {
		taskExecutionListenerResults.clear();
		this.taskListenerExecutorObjectFactory = new TaskListenerExecutorObjectFactory(context);
		this.taskListenerExecutor = this.taskListenerExecutorObjectFactory.getObject();
	}

	@Test
	public void verifyTaskStartupListener() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TaskExecutionListenerConfiguration.class);

		applicationContextRunner.run((context) -> {
			setup(context);

			this.taskListenerExecutor.onTaskStartup(createSampleTaskExecution(BEFORE_LISTENER));
			validateSingleEntry(BEFORE_LISTENER);
		});
	}

	@Test
	public void verifyTaskFailedListener() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TaskExecutionListenerConfiguration.class);

		applicationContextRunner.run((context) -> {
			setup(context);

			this.taskListenerExecutor.onTaskFailed(createSampleTaskExecution(FAIL_LISTENER),
					new IllegalStateException("oops"));
			validateSingleEntry(FAIL_LISTENER);
		});
	}

	@Test
	public void verifyTaskEndListener() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TaskExecutionListenerConfiguration.class);

		applicationContextRunner.run((context) -> {
			setup(context);

			this.taskListenerExecutor.onTaskEnd(createSampleTaskExecution(AFTER_LISTENER));
			validateSingleEntry(AFTER_LISTENER);
		});
	}

	@Test
	public void verifyAllListener() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TaskExecutionListenerConfiguration.class);

		applicationContextRunner.run((context) -> {
			setup(context);

			this.taskListenerExecutor.onTaskStartup(createSampleTaskExecution(BEFORE_LISTENER));
			this.taskListenerExecutor.onTaskFailed(createSampleTaskExecution(FAIL_LISTENER),
					new IllegalStateException("oops"));
			this.taskListenerExecutor.onTaskEnd(createSampleTaskExecution(AFTER_LISTENER));
			assertThat(taskExecutionListenerResults.size()).isEqualTo(3);
			assertThat(taskExecutionListenerResults.get(0).getTaskName()).isEqualTo(BEFORE_LISTENER);
			assertThat(taskExecutionListenerResults.get(1).getTaskName()).isEqualTo(FAIL_LISTENER);
			assertThat(taskExecutionListenerResults.get(2).getTaskName()).isEqualTo(AFTER_LISTENER);
		});
	}

	@Test
	public void verifyTaskStartupListenerWithMultipleInstances() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TaskExecutionListenerMultipleInstanceConfiguration.class);

		applicationContextRunner.run((context) -> {
			setup(context);

			this.taskListenerExecutor.onTaskStartup(createSampleTaskExecution(BEFORE_LISTENER));
			validateSingleEventWithMultipleInstances(BEFORE_LISTENER);
		});
	}

	@Test
	public void verifyTaskFailedListenerWithMultipleInstances() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TaskExecutionListenerMultipleInstanceConfiguration.class);

		applicationContextRunner.run((context) -> {
			setup(context);

			this.taskListenerExecutor.onTaskFailed(createSampleTaskExecution(FAIL_LISTENER),
					new IllegalStateException("oops"));
			validateSingleEventWithMultipleInstances(FAIL_LISTENER);
		});
	}

	@Test
	public void verifyTaskEndListenerWithMultipleInstances() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TaskExecutionListenerMultipleInstanceConfiguration.class);

		applicationContextRunner.run((context) -> {
			setup(context);

			this.taskListenerExecutor.onTaskEnd(createSampleTaskExecution(AFTER_LISTENER));
			validateSingleEventWithMultipleInstances(AFTER_LISTENER);
		});
	}

	@Test
	public void verifyAllListenerWithMultipleInstances() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TaskExecutionListenerMultipleInstanceConfiguration.class);

		applicationContextRunner.run((context) -> {
			setup(context);

			this.taskListenerExecutor.onTaskStartup(createSampleTaskExecution(BEFORE_LISTENER));
			this.taskListenerExecutor.onTaskFailed(createSampleTaskExecution(FAIL_LISTENER),
					new IllegalStateException("oops"));
			this.taskListenerExecutor.onTaskEnd(createSampleTaskExecution(AFTER_LISTENER));
			assertThat(taskExecutionListenerResults.size()).isEqualTo(6);
			assertThat(taskExecutionListenerResults.get(0).getTaskName()).isEqualTo(BEFORE_LISTENER);
			assertThat(taskExecutionListenerResults.get(1).getTaskName()).isEqualTo(BEFORE_LISTENER);
			assertThat(taskExecutionListenerResults.get(2).getTaskName()).isEqualTo(FAIL_LISTENER);
			assertThat(taskExecutionListenerResults.get(3).getTaskName()).isEqualTo(FAIL_LISTENER);
			assertThat(taskExecutionListenerResults.get(4).getTaskName()).isEqualTo(AFTER_LISTENER);
			assertThat(taskExecutionListenerResults.get(5).getTaskName()).isEqualTo(AFTER_LISTENER);
		});
	}

	private TaskExecution createSampleTaskExecution(String taskName) {
		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName(taskName);
		return taskExecution;
	}

	private void validateSingleEntry(String event) {
		assertThat(taskExecutionListenerResults.size()).isEqualTo(1);
		assertThat(taskExecutionListenerResults.get(0).getTaskName()).isEqualTo(event);
	}

	private void validateSingleEventWithMultipleInstances(String event) {
		assertThat(taskExecutionListenerResults.size()).isEqualTo(2);
		assertThat(taskExecutionListenerResults).allSatisfy(task -> assertThat(task.getTaskName()).isEqualTo(event));
	}

	@Configuration
	public static class TaskExecutionListenerConfiguration {

		@Bean
		public TaskRunComponent taskRunComponent() {
			return new TaskRunComponent();
		}

	}

	@Configuration
	public static class TaskExecutionListenerMultipleInstanceConfiguration {

		@Bean
		public TaskRunComponent taskRunComponent() {
			return new TaskRunComponent();
		}

		@Bean
		public TaskRunComponent otherTaskRunComponent() {
			return new TaskRunComponent();
		}

	}

	public static class TaskRunComponent {

		@BeforeTask
		public void initBeforeListener(TaskExecution taskExecution) {
			TaskListenerExecutorObjectFactoryTests.taskExecutionListenerResults.add(taskExecution);
		}

		@AfterTask
		public void initAfterListener(TaskExecution taskExecution) {
			TaskListenerExecutorObjectFactoryTests.taskExecutionListenerResults.add(taskExecution);
		}

		@FailedTask
		public void initFailedListener(TaskExecution taskExecution, Throwable exception) {
			TaskListenerExecutorObjectFactoryTests.taskExecutionListenerResults.add(taskExecution);
		}

	}

}
