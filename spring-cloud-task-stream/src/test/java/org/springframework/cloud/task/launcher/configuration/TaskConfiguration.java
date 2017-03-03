/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.launcher.configuration;

import java.util.List;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.task.LaunchState;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Glenn Renfro
 */

@Configuration
public class TaskConfiguration {

	@Bean
	public TaskLauncher taskLauncher(){
		return new TestTaskLauncher();
	}

	public static class TestTaskLauncher implements TaskLauncher{

		public static final String LAUNCH_ID = "TEST_LAUNCH_ID";

		private LaunchState state = LaunchState.unknown;

		private List<String> commandlineArguments;

		private String applicationName;

		@Override
		public String launch(AppDeploymentRequest request) {
			state = LaunchState.complete;
			this.commandlineArguments = request.getCommandlineArguments();
			this.applicationName = request.getDefinition().getName();
			return null;
		}

		@Override
		public void cancel(String id) {

		}

		@Override
		public TaskStatus status(String id) {
			return new TaskStatus(LAUNCH_ID, state, null);
		}

		@Override
		public void cleanup(String id) {
			throw new UnsupportedOperationException("Cleanup is not supported");
		}

		@Override
		public void destroy(String appName) {
			throw new UnsupportedOperationException("Destroy is not supported");
		}

		@Override
		public RuntimeEnvironmentInfo environmentInfo() {
			throw new UnsupportedOperationException("environmentInfo is not supported");
		}

		public List<String> getCommandlineArguments() {
			return commandlineArguments;
		}

		public String getApplicationName() {
			return applicationName;
		}
	}
}
