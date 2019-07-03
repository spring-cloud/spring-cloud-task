/*
 * Copyright 2019-2019 the original author or authors.
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
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.task.listener.TaskMetrics;
import org.springframework.cloud.task.repository.TaskExecution;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
public class TaskMetricsTests {

	private TaskMetrics taskMetrics;

	private SimpleMeterRegistry simpleMeterRegistry;

	@Before
	public void before() {
		Metrics.globalRegistry.getMeters().forEach(Metrics.globalRegistry::remove);
		simpleMeterRegistry = new SimpleMeterRegistry();
		Metrics.addRegistry(simpleMeterRegistry);
		taskMetrics = new TaskMetrics();
	}

	@After
	public void after() {
		Metrics.globalRegistry.getMeters().forEach(Metrics.globalRegistry::remove);
	}

	@Test
	public void successfulTask() {

		TaskExecution taskExecution = new TaskExecution(123L, 0, "myTask72", new Date(),
				new Date(), null, new ArrayList<>(), null, null, -1L);

		// Start Task
		taskMetrics.onTaskStartup(taskExecution);

		//// Test Long Task Timer while the task is running.
		LongTaskTimer longTaskTimer = simpleMeterRegistry
				.find(TaskMetrics.SPRING_CLOUD_TASK_ACTIVE_METER).longTaskTimer();
		assertThat(longTaskTimer)
				.withFailMessage("LongTask timer should be created on Task start")
				.isNotNull();
		// assertThat(longTaskTimer.activeTasks()).isEqualTo(1);
		assertThat(longTaskTimer.getId().getTag(TaskMetrics.TASK_NAME_TAG))
				.isEqualTo("myTask72");
		assertThat(longTaskTimer.getId().getTag(TaskMetrics.TASK_EXECUTION_ID_TAG))
				.isEqualTo("123");

		// Finish Task
		taskMetrics.onTaskEnd(taskExecution);

		// Test Timer
		Timer taskTimer = simpleMeterRegistry.find(TaskMetrics.SPRING_CLOUD_TASK_METER)
				.timer();
		assertThat(taskTimer).isNotNull();
		// assertThat(taskTimer.count()).isEqualTo(1L);
		assertThat(taskTimer.getId().getTag(TaskMetrics.TASK_NAME_TAG))
				.isEqualTo("myTask72");
		assertThat(taskTimer.getId().getTag(TaskMetrics.TASK_EXECUTION_ID_TAG))
				.isEqualTo("123");
		assertThat(taskTimer.getId().getTag(TaskMetrics.TASK_EXTERNAL_EXECUTION_ID_TAG))
				.isEqualTo("unknown");
		assertThat(taskTimer.getId().getTag(TaskMetrics.TASK_PARENT_EXECUTION_ID_TAG))
				.isEqualTo("-1");
		assertThat(taskTimer.getId().getTag(TaskMetrics.TASK_EXIT_CODE_TAG))
				.isEqualTo("0");
		assertThat(taskTimer.getId().getTag(TaskMetrics.TASK_EXCEPTION_TAG))
				.isEqualTo("none");
		assertThat(taskTimer.getId().getTag(TaskMetrics.TASK_STATUS_TAG))
				.isEqualTo(TaskMetrics.STATUS_SUCCESS);

		// Test Long Task Timer after the task has completed.
		// LongTaskTimer longTaskTimer = simpleMeterRegistry
		// .find(TaskMetrics.SPRING_CLOUD_TASK_ACTIVE_METER).longTaskTimer();
		assertThat(longTaskTimer.activeTasks()).isEqualTo(0);
		assertThat(longTaskTimer.getId().getTag(TaskMetrics.TASK_NAME_TAG))
				.isEqualTo("myTask72");
		assertThat(longTaskTimer.getId().getTag(TaskMetrics.TASK_EXECUTION_ID_TAG))
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
				.find(TaskMetrics.SPRING_CLOUD_TASK_ACTIVE_METER).longTaskTimer();
		assertThat(longTaskTimer)
				.withFailMessage("LongTask timer should be created on Task start")
				.isNotNull();
		assertThat(longTaskTimer.activeTasks()).isEqualTo(1);
		assertThat(longTaskTimer.getId().getTag(TaskMetrics.TASK_NAME_TAG))
				.isEqualTo("myTask");
		assertThat(longTaskTimer.getId().getTag(TaskMetrics.TASK_EXECUTION_ID_TAG))
				.isEqualTo("123");

		taskMetrics.onTaskFailed(new RuntimeException("Test"));

		// Finish Task. TaskLifecycleListen calls onTaskEnd after the onTaskFailed. Make
		// sure that the counter status
		// is not affected by this.
		taskMetrics.onTaskEnd(taskExecution);

		Timer taskTimer = simpleMeterRegistry.find(TaskMetrics.SPRING_CLOUD_TASK_METER)
				.timer();
		assertThat(taskTimer).isNotNull();

		assertThat(taskTimer).isNotNull();
		// assertThat(taskTimer.count()).isEqualTo(1L);
		assertThat(taskTimer.getId().getTag(TaskMetrics.TASK_NAME_TAG))
				.isEqualTo("myTask");
		assertThat(taskTimer.getId().getTag(TaskMetrics.TASK_EXECUTION_ID_TAG))
				.isEqualTo("123");
		assertThat(taskTimer.getId().getTag(TaskMetrics.TASK_EXTERNAL_EXECUTION_ID_TAG))
				.isEqualTo("unknown");
		assertThat(taskTimer.getId().getTag(TaskMetrics.TASK_PARENT_EXECUTION_ID_TAG))
				.isEqualTo("-1");
		assertThat(taskTimer.getId().getTag(TaskMetrics.TASK_EXIT_CODE_TAG))
				.isEqualTo("0");
		assertThat(taskTimer.getId().getTag(TaskMetrics.TASK_EXCEPTION_TAG))
				.isEqualTo("RuntimeException");
		assertThat(taskTimer.getId().getTag(TaskMetrics.TASK_STATUS_TAG))
				.isEqualTo(TaskMetrics.STATUS_FAILURE);

		// Test Long Task Timer after the task has completed.
		assertThat(longTaskTimer.activeTasks()).isEqualTo(0);
		assertThat(longTaskTimer.getId().getTag(TaskMetrics.TASK_NAME_TAG))
				.isEqualTo("myTask");
		assertThat(longTaskTimer.getId().getTag(TaskMetrics.TASK_EXECUTION_ID_TAG))
				.isEqualTo("123");
	}

}
