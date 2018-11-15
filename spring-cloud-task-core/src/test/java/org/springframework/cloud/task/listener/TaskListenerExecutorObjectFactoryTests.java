/*
 *  Copyright 2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.listener;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the {@link TaskListenerExecutorObjectFactory} retrieves the
 * {@link TaskListenerExecutor}.
 *
 * @author Glenn Renfro
 * @since 2.1.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { TaskListenerExecutorObjectFactoryTests.TaskExecutionListenerConfiguration.class })
@DirtiesContext
public class TaskListenerExecutorObjectFactoryTests {

	public static final String BEFORE_LISTENER = "BEFORE LISTENER";

	public static final String AFTER_LISTENER = "AFTER LISTENER";

	public static final String FAIL_LISTENER = "FAIL LISTENER";

	public static List<TaskExecution> taskExecutionListenerResults = new ArrayList<>(3);

	@Autowired
	private ConfigurableApplicationContext context;

	private TaskListenerExecutor taskListenerExecutor;

	private TaskListenerExecutorObjectFactory taskListenerExecutorObjectFactory;

	@Before
	public void setup() {
		taskExecutionListenerResults.clear();
		this.taskListenerExecutorObjectFactory = new TaskListenerExecutorObjectFactory(this.context);
		this.taskListenerExecutor = this.taskListenerExecutorObjectFactory.getObject();
	}

	@Test
	public void verifyTaskStartupListener() {
		this.taskListenerExecutor.onTaskStartup(createSampleTaskExecution(BEFORE_LISTENER));
		validateSingleEntry(BEFORE_LISTENER);
	}

	@Test
	public void verifyTaskFailedListener() {
		this.taskListenerExecutor.onTaskFailed(createSampleTaskExecution(FAIL_LISTENER),
				new IllegalStateException("oops"));
		validateSingleEntry(FAIL_LISTENER);
	}

	@Test
	public void verifyTaskEndListener() {
		this.taskListenerExecutor.onTaskEnd(createSampleTaskExecution(AFTER_LISTENER));
		validateSingleEntry(AFTER_LISTENER);
	}

	@Test
	public void verifyAllListener() {
		this.taskListenerExecutor.onTaskStartup(createSampleTaskExecution(BEFORE_LISTENER));
		this.taskListenerExecutor.onTaskFailed(createSampleTaskExecution(FAIL_LISTENER),
				new IllegalStateException("oops"));
		this.taskListenerExecutor.onTaskEnd(createSampleTaskExecution(AFTER_LISTENER));
		assertThat(taskExecutionListenerResults.size()).isEqualTo(3);
		assertThat(taskExecutionListenerResults.get(0).getTaskName()).isEqualTo(BEFORE_LISTENER);
		assertThat(taskExecutionListenerResults.get(1).getTaskName()).isEqualTo(FAIL_LISTENER);
		assertThat(taskExecutionListenerResults.get(2).getTaskName()).isEqualTo(AFTER_LISTENER);
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

	@Configuration
	public static class TaskExecutionListenerConfiguration {

		@Bean
		public TaskRunComponent taskRunComponent() {
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
