/*
 * Copyright 2016 the original author or authors.
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

package io.spring.configuration;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.LaunchState;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Glenn Renfro
 */
@Configuration
public class TaskSinkConfiguration {

	@Bean
	public TaskLauncher taskLauncher() {
		return new TestTaskLauncher();
	}

	public class TestTaskLauncher implements TaskLauncher {

		public static final String LAUNCH_ID = "TEST_LAUNCH_ID";

		private LaunchState state = LaunchState.unknown;

		@Override
		public String launch(AppDeploymentRequest request) {
			state = LaunchState.complete;
			return null;
		}

		@Override
		public void cancel(String id) {

		}

		@Override
		public TaskStatus status(String id) {
			String taskLaunchId = LAUNCH_ID;
			TaskStatus taskStatus = new TaskStatus(taskLaunchId, state, null);
			return taskStatus;
		}
	}

}
