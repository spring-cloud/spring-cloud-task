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

import java.util.ArrayList;
import java.util.Date;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.observation.TimerObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.core.tck.ObservationRegistryAssert;
import io.micrometer.core.tck.TestObservationRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.task.listener.TaskExecutionMeters;
import org.springframework.cloud.task.listener.TaskMetrics;
import org.springframework.cloud.task.repository.TaskExecution;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 * @author Glenn Renfro
 */
public class TaskMetricsTests {

	private TaskMetrics taskMetrics;

	private SimpleMeterRegistry simpleMeterRegistry;

	private ObservationRegistry observationRegistry;

	@BeforeEach
	public void before() {
		this.simpleMeterRegistry = new SimpleMeterRegistry();
		this.observationRegistry = TestObservationRegistry.create();
		ObservationHandler<Observation.Context> timerObservationHandler = new TimerObservationHandler(this.simpleMeterRegistry);
		this.observationRegistry.observationConfig().observationHandler(timerObservationHandler);
		this.taskMetrics = new TaskMetrics(this.observationRegistry);
	}

	@AfterEach
	public void after() {
		this.simpleMeterRegistry.clear();
		ObservationRegistryAssert.assertThat(this.observationRegistry).doesNotHaveAnyRemainingCurrentObservation();
	}

	@Test
	public void successfulTask() {

		TaskExecution taskExecution = new TaskExecution(123L, 0, "myTask72", new Date(),
				new Date(), null, new ArrayList<>(), null, null, -1L);

		// Start Task
		taskMetrics.onTaskStartup(taskExecution);

		// Test Long Task Timer while the task is running.
		LongTaskTimer longTaskTimer = simpleMeterRegistry
				.find(TaskExecutionMeters.TASK_ACTIVE.getName()).longTaskTimer();
		assertThat(longTaskTimer)
				.withFailMessage("LongTask timer should be created on Task start")
				.isNotNull();
		assertThat(longTaskTimer.activeTasks()).isEqualTo(1);
		assertThat(longTaskTimer.getId().getTag(TaskExecutionMeters.TaskTags.TASK_NAME_TAG.getKeyName()))
				.isEqualTo("myTask72");
		assertThat(longTaskTimer.getId().getTag(TaskExecutionMeters.TaskTags.TASK_EXECUTION_ID_TAG.getKeyName()))
				.isEqualTo("123");

		// Finish Task
		taskMetrics.onTaskEnd(taskExecution);

		// Test Timer
		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TaskExecutionMeters.TASK_ACTIVE.getPrefix(),
				Tags.of(TaskExecutionMeters.TaskTags.TASK_NAME_TAG.getKeyName(), "myTask72"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TaskExecutionMeters.TASK_ACTIVE.getPrefix(),
				Tags.of(TaskExecutionMeters.TaskTags.TASK_EXECUTION_ID_TAG.getKeyName(), "123"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TaskExecutionMeters.TASK_ACTIVE.getPrefix(),
				Tags.of(TaskExecutionMeters.TaskTags.TASK_PARENT_EXECUTION_ID_TAG.getKeyName(), "-1"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TaskExecutionMeters.TASK_ACTIVE.getPrefix(),
				Tags.of(TaskExecutionMeters.TaskTags.TASK_EXIT_CODE_TAG.getKeyName(), "0"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TaskExecutionMeters.TASK_ACTIVE.getPrefix(),
				Tags.of(TaskExecutionMeters.TaskTags.TASK_EXCEPTION_TAG.getKeyName(), "none"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TaskExecutionMeters.TASK_ACTIVE.getPrefix(),
				Tags.of(TaskExecutionMeters.TaskTags.TASK_STATUS_TAG.getKeyName(), TaskMetrics.STATUS_SUCCESS));

		// Test Long Task Timer after the task has completed.
		assertThat(longTaskTimer.activeTasks()).isEqualTo(0);
		assertThat(longTaskTimer.getId().getTag(TaskExecutionMeters.TaskTags.TASK_NAME_TAG.getKeyName()))
				.isEqualTo("myTask72");
		assertThat(longTaskTimer.getId().getTag(TaskExecutionMeters.TaskTags.TASK_EXECUTION_ID_TAG.getKeyName()))
				.isEqualTo("123");
	}

	@Test
	public void failingTask() {

		TaskExecution taskExecution = new TaskExecution(123L, 0, "myTask", new Date(),
				new Date(), null, new ArrayList<>(), null, null, -1L);

		// Start Task
		taskMetrics.onTaskStartup(taskExecution);

		// Test Long Task Timer while the task is running.
		LongTaskTimer longTaskTimer = simpleMeterRegistry
				.find(TaskExecutionMeters.TASK_ACTIVE.getName()).longTaskTimer();
		assertThat(longTaskTimer)
				.withFailMessage("LongTask timer should be created on Task start")
				.isNotNull();
		assertThat(longTaskTimer.activeTasks()).isEqualTo(1);
		assertThat(longTaskTimer.getId().getTag(TaskExecutionMeters.TaskTags.TASK_NAME_TAG.getKeyName()))
				.isEqualTo("myTask");
		assertThat(longTaskTimer.getId().getTag(TaskExecutionMeters.TaskTags.TASK_EXECUTION_ID_TAG.getKeyName()))
				.isEqualTo("123");

		taskMetrics.onTaskFailed(new RuntimeException("Test"));

		// Finish Task. TaskLifecycleListen calls onTaskEnd after the onTaskFailed. Make
		// sure that the counter status
		// is not affected by this.
		taskExecution.setExitCode(1);
		taskMetrics.onTaskEnd(taskExecution);

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TaskExecutionMeters.TASK_ACTIVE.getPrefix(),
				Tags.of(TaskExecutionMeters.TaskTags.TASK_NAME_TAG.getKeyName(), "myTask"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TaskExecutionMeters.TASK_ACTIVE.getPrefix(),
				Tags.of(TaskExecutionMeters.TaskTags.TASK_EXECUTION_ID_TAG.getKeyName(), "123"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TaskExecutionMeters.TASK_ACTIVE.getPrefix(),
				Tags.of(TaskExecutionMeters.TaskTags.TASK_PARENT_EXECUTION_ID_TAG.getKeyName(), "-1"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TaskExecutionMeters.TASK_ACTIVE.getPrefix(),
				Tags.of(TaskExecutionMeters.TaskTags.TASK_EXIT_CODE_TAG.getKeyName(), "1"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TaskExecutionMeters.TASK_ACTIVE.getPrefix(),
				Tags.of(TaskExecutionMeters.TaskTags.TASK_EXCEPTION_TAG.getKeyName(), "RuntimeException"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TaskExecutionMeters.TASK_ACTIVE.getPrefix(),
				Tags.of(TaskExecutionMeters.TaskTags.TASK_STATUS_TAG.getKeyName(), TaskMetrics.STATUS_FAILURE));

		// Test Long Task Timer after the task has completed.
		assertThat(longTaskTimer.activeTasks()).isEqualTo(0);
		assertThat(longTaskTimer.getId().getTag(TaskExecutionMeters.TaskTags.TASK_NAME_TAG.getKeyName()))
				.isEqualTo("myTask");
		assertThat(longTaskTimer.getId().getTag(TaskExecutionMeters.TaskTags.TASK_EXECUTION_ID_TAG.getKeyName()))
				.isEqualTo("123");
	}

}
