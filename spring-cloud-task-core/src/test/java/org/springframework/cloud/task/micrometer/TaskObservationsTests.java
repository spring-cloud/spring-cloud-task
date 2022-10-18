/*
 * Copyright 2019-2022 the original author or authors.
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

package org.springframework.cloud.task.micrometer;

import java.time.LocalDateTime;
import java.util.ArrayList;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.ObservationRegistryAssert;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.configuration.TaskObservationCloudKeyValues;
import org.springframework.cloud.task.listener.TaskExecutionObservation;
import org.springframework.cloud.task.listener.TaskObservations;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.task.listener.TaskObservations.UNKNOWN;

/**
 * @author Christian Tzolov
 * @author Glenn Renfro
 */
public class TaskObservationsTests {

	/**
	 * Prefix for the spring cloud task project.
	 */
	public static final String PREFIX = "spring.cloud.task";

	private TaskObservations taskObservations;

	private SimpleMeterRegistry simpleMeterRegistry;

	private ObservationRegistry observationRegistry;

	@BeforeEach
	public void before() {
		this.simpleMeterRegistry = new SimpleMeterRegistry();
		this.observationRegistry = TestObservationRegistry.create();
		ObservationHandler<Observation.Context> timerObservationHandler = new DefaultMeterObservationHandler(
				this.simpleMeterRegistry);
		this.observationRegistry.observationConfig().observationHandler(timerObservationHandler);
		this.taskObservations = new TaskObservations(this.observationRegistry, null, null);
	}

	@AfterEach
	public void after() {
		this.simpleMeterRegistry.clear();
		ObservationRegistryAssert.assertThat(this.observationRegistry).doesNotHaveAnyRemainingCurrentObservation();
	}

	@Test
	public void successfulTaskTest() {

		TaskExecution taskExecution = startupObservationForBasicTests("myTask72", 123L);

		LongTaskTimer longTaskTimer = initializeBasicTest("myTask72", "123");

		// Finish Task
		taskObservations.onTaskEnd(taskExecution);

		verifyDefaultKeyValues();
		TaskExecutionObservation.TASK_ACTIVE.getDefaultConvention();
		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags("spring.cloud.task", Tags
				.of(TaskExecutionObservation.TaskKeyValues.TASK_STATUS.asString(), TaskObservations.STATUS_SUCCESS));

		verifyLongTaskTimerAfterStop(longTaskTimer, "myTask72", "123");
	}

	@Test
	public void defaultTaskTest() {

		TaskExecution taskExecution = new TaskExecution(123L, 0, null, LocalDateTime.now(), LocalDateTime.now(), null,
				new ArrayList<>(), null, null, null);

		// Start Task
		taskObservations.onTaskStartup(taskExecution);

		LongTaskTimer longTaskTimer = initializeBasicTest(UNKNOWN, "123");

		// Finish Task
		taskObservations.onTaskEnd(taskExecution);

		// Test Timer
		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_NAME.asString(), UNKNOWN));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_EXECUTION_ID.asString(), "123"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_PARENT_EXECUTION_ID.asString(), UNKNOWN));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_EXTERNAL_EXECUTION_ID.asString(), UNKNOWN));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_EXIT_CODE.asString(), "0"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX, Tags
				.of(TaskExecutionObservation.TaskKeyValues.TASK_STATUS.asString(), TaskObservations.STATUS_SUCCESS));

		verifyLongTaskTimerAfterStop(longTaskTimer, "unknown", "123");

	}

	@Test
	public void failingTask() {

		TaskExecution taskExecution = startupObservationForBasicTests("myTask72", 123L);

		LongTaskTimer longTaskTimer = initializeBasicTest("myTask72", "123");

		taskObservations.onTaskFailed(new RuntimeException("Test"));

		// Finish Task. TaskLifecycleListen calls onTaskEnd after the onTaskFailed. Make
		// sure that the counter status
		// is not affected by this.
		taskExecution.setExitCode(1);
		taskObservations.onTaskEnd(taskExecution);

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_NAME.asString(), "myTask72"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_EXECUTION_ID.asString(), "123"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_PARENT_EXECUTION_ID.asString(), "-1"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_EXIT_CODE.asString(), "1"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX, Tags
				.of(TaskExecutionObservation.TaskKeyValues.TASK_STATUS.asString(), TaskObservations.STATUS_FAILURE));

		verifyLongTaskTimerAfterStop(longTaskTimer, "myTask72", "123");
	}

	@Test
	public void taskWithCloudKeyValues() {
		final String APPLICATION_ID = "123";
		final String APPLICATION_NAME = "APP123";
		final String SPACE_ID = "123";
		final String SPACE_NAME = "SPACE123";
		final String APPLICATION_VERSION = "APPV123";
		final String INSTANCE_INDEX = "55";
		final String ORGANIZATION_NAME = "ORG123";
		TaskObservationCloudKeyValues taskObservationCloudKeyValues = new TaskObservationCloudKeyValues();
		taskObservationCloudKeyValues.setApplicationId(APPLICATION_ID);
		taskObservationCloudKeyValues.setApplicationName(APPLICATION_NAME);
		taskObservationCloudKeyValues.setSpaceId(SPACE_ID);
		taskObservationCloudKeyValues.setSpaceName(SPACE_NAME);
		taskObservationCloudKeyValues.setApplicationVersion(APPLICATION_VERSION);
		taskObservationCloudKeyValues.setInstanceIndex(INSTANCE_INDEX);
		taskObservationCloudKeyValues.setOrganizationName(ORGANIZATION_NAME);
		this.taskObservations = new TaskObservations(this.observationRegistry, taskObservationCloudKeyValues, null);

		TaskExecution taskExecution = startupObservationForBasicTests("myTask72", 123L);

		LongTaskTimer longTaskTimer = initializeBasicTest("myTask72", "123");

		// Finish Task
		taskObservations.onTaskEnd(taskExecution);

		verifyDefaultKeyValues();

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_CF_ORG_NAME.asString(), ORGANIZATION_NAME));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_CF_SPACE_ID.asString(), SPACE_ID));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_CF_SPACE_NAME.asString(), SPACE_NAME));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_CF_APP_NAME.asString(), APPLICATION_NAME));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_CF_APP_ID.asString(), APPLICATION_ID));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_CF_APP_VERSION.asString(), APPLICATION_VERSION));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_CF_INSTANCE_INDEX.asString(), INSTANCE_INDEX));

		// Test Timer
		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_NAME.asString(), "myTask72"));

		verifyLongTaskTimerAfterStop(longTaskTimer, "myTask72", "123");
	}

	@Test
	public void testCloudVariablesUninitialized() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(CloudConfigurationForDefaultValues.class));
		applicationContextRunner.run((context) -> {
			TaskObservationCloudKeyValues taskObservationCloudKeyValues = context
					.getBean(TaskObservationCloudKeyValues.class);

			assertThat(taskObservationCloudKeyValues).as("taskObservationCloudKeyValues should not be null")
					.isNotNull();

			this.taskObservations = new TaskObservations(this.observationRegistry, taskObservationCloudKeyValues, null);

			TaskExecution taskExecution = startupObservationForBasicTests("myTask72", 123L);

			LongTaskTimer longTaskTimer = initializeBasicTest("myTask72", "123");

			// Finish Task
			taskObservations.onTaskEnd(taskExecution);

			verifyDefaultKeyValues();

			MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
					Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_CF_ORG_NAME.asString(), "default"));

			MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
					Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_CF_SPACE_ID.asString(), UNKNOWN));

			MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
					Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_CF_SPACE_NAME.asString(), UNKNOWN));

			MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
					Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_CF_APP_NAME.asString(), UNKNOWN));

			MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
					Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_CF_APP_ID.asString(), UNKNOWN));

			MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
					Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_CF_APP_VERSION.asString(), UNKNOWN));

			MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
					Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_CF_INSTANCE_INDEX.asString(), "0"));

			// Test Timer
			MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
					Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_NAME.asString(), "myTask72"));

			verifyLongTaskTimerAfterStop(longTaskTimer, "myTask72", "123");
		});
	}

	private TaskExecution startupObservationForBasicTests(String taskName, long taskExecutionId) {
		TaskExecution taskExecution = new TaskExecution(taskExecutionId, 0, taskName, LocalDateTime.now(),
				LocalDateTime.now(), null, new ArrayList<>(), null, "-1", -1L);

		// Start Task
		taskObservations.onTaskStartup(taskExecution);
		return taskExecution;
	}

	private LongTaskTimer initializeBasicTest(String taskName, String executionId) {
		// Test Long Task Timer while the task is running.
		LongTaskTimer longTaskTimer = simpleMeterRegistry
				.find(TaskExecutionObservation.TASK_ACTIVE.getPrefix() + ".active").longTaskTimer();
		System.out.println(simpleMeterRegistry.getMetersAsString());
		assertThat(longTaskTimer).withFailMessage("LongTask timer should be created on Task start").isNotNull();
		assertThat(longTaskTimer.activeTasks()).isEqualTo(1);
		assertThat(longTaskTimer.getId().getTag(TaskExecutionObservation.TaskKeyValues.TASK_NAME.asString()))
				.isEqualTo(taskName);
		assertThat(longTaskTimer.getId().getTag(TaskExecutionObservation.TaskKeyValues.TASK_EXECUTION_ID.asString()))
				.isEqualTo(executionId);
		return longTaskTimer;
	}

	private void verifyDefaultKeyValues() {
		// Test Timer
		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_NAME.asString(), "myTask72"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_EXECUTION_ID.asString(), "123"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_PARENT_EXECUTION_ID.asString(), "-1"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX,
				Tags.of(TaskExecutionObservation.TaskKeyValues.TASK_EXIT_CODE.asString(), "0"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry).hasTimerWithNameAndTags(PREFIX, Tags
				.of(TaskExecutionObservation.TaskKeyValues.TASK_STATUS.asString(), TaskObservations.STATUS_SUCCESS));
	}

	private void verifyLongTaskTimerAfterStop(LongTaskTimer longTaskTimer, String taskName, String executionId) {
		// Test Long Task Timer after the task has completed.
		assertThat(longTaskTimer.activeTasks()).isEqualTo(0);
		assertThat(longTaskTimer.getId().getTag(TaskExecutionObservation.TaskKeyValues.TASK_NAME.asString()))
				.isEqualTo(taskName);
		assertThat(longTaskTimer.getId().getTag(TaskExecutionObservation.TaskKeyValues.TASK_EXECUTION_ID.asString()))
				.isEqualTo(executionId);
	}

	@Configuration
	static class CloudConfigurationForDefaultValues {

		@Bean
		public TaskObservationCloudKeyValues taskObservationCloudKeyValues() {
			return new TaskObservationCloudKeyValues();
		}

	}

}
