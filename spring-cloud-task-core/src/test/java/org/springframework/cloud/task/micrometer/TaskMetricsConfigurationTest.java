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
import io.micrometer.core.instrument.Timer;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.task.configuration.TaskMetricsConfiguration;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
@RunWith(Enclosed.class)
public class TaskMetricsConfigurationTest {

	@TestPropertySource(properties = { "spring.cloud.task.name=myTask",
			"spring.cloud.task.executionid=666" })
	@Import(TaskMetricsConfiguration.class)
	public static class TestPresetTagValues extends AbstractMicrometerTest {

		@Autowired
		TaskMetricsConfiguration.MicrometerTaskExecutionListener micrometerTaskExecutionListener;

		@Test
		public void testTaskTimers() {
			long executionId = 666L;
			String taskName = "myTask";
			int exitCode = 0;
			TaskExecution taskExecution = new TaskExecution(executionId, exitCode,
					taskName, new Date(), new Date(), null, new ArrayList<>(), null, null,
					-1L);
			micrometerTaskExecutionListener.onTaskStartup(taskExecution);
			micrometerTaskExecutionListener.onTaskEnd(taskExecution);

			// Test Timer
			Timer taskTimer = this.simpleMeterRegistry.find("spring.cloud.task").timer();
			assertThat(taskTimer).isNotNull();
			assertThat(taskTimer.count()).isEqualTo(1L);
			assertThat(taskTimer.getId().getTag("task.name")).isEqualTo("myTask");
			assertThat(taskTimer.getId().getTag("task.execution.id")).isEqualTo("666");
			assertThat(taskTimer.getId().getTag("task.external.execution.id"))
					.isEqualTo("unknown");
			assertThat(taskTimer.getId().getTag("task.parent.execution.id"))
					.isEqualTo("-1");
			assertThat(taskTimer.getId().getTag("task.exit.code")).isEqualTo("0");
			assertThat(taskTimer.getId().getTag("task.status")).isEqualTo("SUCCESS");

			// Test Long Task Timer
			LongTaskTimer longTaskTimer = this.simpleMeterRegistry
					.find("spring.cloud.task.active").longTaskTimer();
			assertThat(longTaskTimer).isNotNull();
			assertThat(longTaskTimer.activeTasks()).isEqualTo(0);
			assertThat(longTaskTimer.getId().getTag("task.name")).isEqualTo("myTask");
			assertThat(longTaskTimer.getId().getTag("task.execution.id"))
					.isEqualTo("666");
		}

	}

}
