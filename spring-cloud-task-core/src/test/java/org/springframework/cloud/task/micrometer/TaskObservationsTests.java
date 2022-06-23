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

import org.springframework.cloud.task.listener.TaskObservations;
import org.springframework.cloud.task.repository.TaskExecution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.task.listener.TaskObservations.TASK_ACTIVE_NAME;
import static org.springframework.cloud.task.listener.TaskObservations.TASK_EXCEPTION;
import static org.springframework.cloud.task.listener.TaskObservations.TASK_EXECUTION_ID;
import static org.springframework.cloud.task.listener.TaskObservations.TASK_EXIT_CODE;
import static org.springframework.cloud.task.listener.TaskObservations.TASK_NAME;
import static org.springframework.cloud.task.listener.TaskObservations.TASK_PARENT_EXECUTION_ID;
import static org.springframework.cloud.task.listener.TaskObservations.TASK_PREFIX;
import static org.springframework.cloud.task.listener.TaskObservations.TASK_STATUS;

/**
 * @author Christian Tzolov
 * @author Glenn Renfro
 */
public class TaskObservationsTests {

	private TaskObservations taskObservations;

	private SimpleMeterRegistry simpleMeterRegistry;

	private ObservationRegistry observationRegistry;

	@BeforeEach
	public void before() {
		this.simpleMeterRegistry = new SimpleMeterRegistry();
		this.observationRegistry = TestObservationRegistry.create();
		ObservationHandler<Observation.Context> timerObservationHandler = new TimerObservationHandler(this.simpleMeterRegistry);
		this.observationRegistry.observationConfig().observationHandler(timerObservationHandler);
		this.taskObservations = new TaskObservations(this.observationRegistry, null);
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
		taskObservations.onTaskStartup(taskExecution);

		// Test Long Task Timer while the task is running.
		LongTaskTimer longTaskTimer = simpleMeterRegistry
				.find(TASK_ACTIVE_NAME).longTaskTimer();
		assertThat(longTaskTimer)
				.withFailMessage("LongTask timer should be created on Task start")
				.isNotNull();
		assertThat(longTaskTimer.activeTasks()).isEqualTo(1);
		assertThat(longTaskTimer.getId().getTag(TASK_NAME))
				.isEqualTo("myTask72");
		assertThat(longTaskTimer.getId().getTag(TASK_EXECUTION_ID))
				.isEqualTo("123");

		// Finish Task
		taskObservations.onTaskEnd(taskExecution);

		// Test Timer
		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TASK_PREFIX,
				Tags.of(TASK_NAME, "myTask72"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TASK_PREFIX,
				Tags.of(TASK_EXECUTION_ID, "123"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TASK_PREFIX,
				Tags.of(TASK_PARENT_EXECUTION_ID, "-1"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TASK_PREFIX,
				Tags.of(TASK_EXIT_CODE, "0"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TASK_PREFIX,
				Tags.of(TASK_EXCEPTION, "none"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TASK_PREFIX,
				Tags.of(TASK_STATUS, TaskObservations.STATUS_SUCCESS));

		// Test Long Task Timer after the task has completed.
		assertThat(longTaskTimer.activeTasks()).isEqualTo(0);
		assertThat(longTaskTimer.getId().getTag(TASK_NAME))
				.isEqualTo("myTask72");
		assertThat(longTaskTimer.getId().getTag(TASK_EXECUTION_ID))
				.isEqualTo("123");
	}

	@Test
	public void failingTask() {

		TaskExecution taskExecution = new TaskExecution(123L, 0, "myTask", new Date(),
				new Date(), null, new ArrayList<>(), null, null, -1L);

		// Start Task
		taskObservations.onTaskStartup(taskExecution);

		// Test Long Task Timer while the task is running.
		LongTaskTimer longTaskTimer = simpleMeterRegistry
				.find(TASK_ACTIVE_NAME).longTaskTimer();
		assertThat(longTaskTimer)
				.withFailMessage("LongTask timer should be created on Task start")
				.isNotNull();
		assertThat(longTaskTimer.activeTasks()).isEqualTo(1);
		assertThat(longTaskTimer.getId().getTag(TASK_NAME))
				.isEqualTo("myTask");
		assertThat(longTaskTimer.getId().getTag(TASK_EXECUTION_ID))
				.isEqualTo("123");

		taskObservations.onTaskFailed(new RuntimeException("Test"));

		// Finish Task. TaskLifecycleListen calls onTaskEnd after the onTaskFailed. Make
		// sure that the counter status
		// is not affected by this.
		taskExecution.setExitCode(1);
		taskObservations.onTaskEnd(taskExecution);

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TASK_PREFIX,
				Tags.of(TASK_NAME, "myTask"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TASK_PREFIX,
				Tags.of(TASK_EXECUTION_ID, "123"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TASK_PREFIX,
				Tags.of(TASK_PARENT_EXECUTION_ID, "-1"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TASK_PREFIX,
				Tags.of(TASK_EXIT_CODE, "1"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TASK_PREFIX,
				Tags.of(TASK_EXCEPTION, "RuntimeException"));

		MeterRegistryAssert.assertThat(this.simpleMeterRegistry)
			.hasTimerWithNameAndTags(TASK_PREFIX,
				Tags.of(TASK_STATUS, TaskObservations.STATUS_FAILURE));

		// Test Long Task Timer after the task has completed.
		assertThat(longTaskTimer.activeTasks()).isEqualTo(0);
		assertThat(longTaskTimer.getId().getTag(TASK_NAME))
				.isEqualTo("myTask");
		assertThat(longTaskTimer.getId().getTag(TASK_EXECUTION_ID))
				.isEqualTo("123");
	}

}
