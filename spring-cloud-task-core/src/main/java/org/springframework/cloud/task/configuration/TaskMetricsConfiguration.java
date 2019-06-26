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

package org.springframework.cloud.task.configuration;

import java.io.Closeable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

import org.springframework.cloud.task.listener.TaskExecutionListener;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Christian Tzolov
 */
@Configuration
public class TaskMetricsConfiguration {

	private static final String METRICS_PREFIX = "spring.cloud.";

	private static final String STATUS_SUCCESS = "SUCCESS";

	private static final String STATUS_FAILURE = "FAILURE";

	@Bean
	public MicrometerTaskExecutionListener getTaskExecutionListener() {
		return new MicrometerTaskExecutionListener();
	}

	static class TaskExecutionKey {

		private final TaskExecution taskExecution;

		TaskExecutionKey(TaskExecution taskExecution) {
			this.taskExecution = taskExecution;
		}

		public String getTaskName() {
			return this.taskExecution.getTaskName();
		}

		public Long getExecutionId() {
			return this.taskExecution.getExecutionId();
		}

		public TaskExecution getTaskExecution() {
			return taskExecution;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || this.getClass() != o.getClass()) {
				return false;
			}
			TaskExecutionKey that = (TaskExecutionKey) o;
			return Objects.equals(getTaskName(), that.getTaskName())
					&& Objects.equals(getExecutionId(), that.getExecutionId());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getTaskName(), getExecutionId());
		}

	}

	public static class MicrometerTaskExecutionListener
			implements TaskExecutionListener, Closeable {

		private ConcurrentHashMap<TaskExecutionKey, Timer.Sample> taskSamples = new ConcurrentHashMap();

		private ConcurrentHashMap<TaskExecutionKey, LongTaskTimer.Sample> longTaskSamples = new ConcurrentHashMap();

		@Override
		public void onTaskStartup(TaskExecution taskExecution) {
			LongTaskTimer ongTaskTimer = LongTaskTimer
					.builder(METRICS_PREFIX + "task.active")
					.description("Long task duration").tags(taskTags(taskExecution))
					.register(Metrics.globalRegistry);

			this.longTaskSamples.put(taskKey(taskExecution), ongTaskTimer.start());
			this.taskSamples.put(taskKey(taskExecution),
					Timer.start(Metrics.globalRegistry));
		}

		@Override
		public void onTaskEnd(TaskExecution taskExecution) {
			this.finishTask(taskExecution, STATUS_SUCCESS);
			this.finishLongTask(taskExecution);
		}

		@Override
		public void onTaskFailed(TaskExecution taskExecution, Throwable throwable) {
			this.finishTask(taskExecution, STATUS_FAILURE);
			this.finishLongTask(taskExecution);
		}

		private void finishLongTask(TaskExecution taskExecution) {
			LongTaskTimer.Sample longTaskSample = this.longTaskSamples
					.remove(taskKey(taskExecution));
			if (longTaskSample != null) {
				longTaskSample.stop();
			}
		}

		private void finishTask(TaskExecution taskExecution, String status) {
			Timer.Sample taskSample = this.taskSamples.remove(taskKey(taskExecution));

			taskSample.stop(taskTimer(taskExecution, status));

		}

		private Timer taskTimer(TaskExecution taskExecution, String status) {
			return Timer.builder(METRICS_PREFIX + "task").description("Task duration")
					.tags(taskTags(taskExecution))
					.tag("task.exit.code", "" + taskExecution.getExitCode())
					.tag("task.status", status).register(Metrics.globalRegistry);
		}

		private Tags taskTags(TaskExecution taskExecution) {
			return Tags.of("task.name", taskExecution.getTaskName())
					.and("task.execution.id", "" + taskExecution.getExecutionId())
					.and("task.parent.execution.id",
							"" + taskExecution.getParentExecutionId())
					.and("task.external.execution.id",
							(taskExecution.getExternalExecutionId() == null) ? "unknown"
									: "" + taskExecution.getExternalExecutionId());
		}

		private TaskExecutionKey taskKey(TaskExecution taskExecution) {
			return new TaskExecutionKey(taskExecution);
		}

		@Override
		public void close() {
			if (this.taskSamples != null) {
				for (Map.Entry<TaskExecutionKey, Timer.Sample> sampleEntry : this.taskSamples
						.entrySet()) {
					sampleEntry.getValue().stop(taskTimer(
							sampleEntry.getKey().getTaskExecution(), STATUS_SUCCESS));
				}
				this.taskSamples.clear();
			}

			if (this.longTaskSamples != null) {
				for (LongTaskTimer.Sample sample : this.longTaskSamples.values()) {
					sample.stop();
				}
				this.longTaskSamples.clear();
			}
		}

	}

}
