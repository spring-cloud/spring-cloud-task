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

package org.springframework.cloud.task.launcher;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.deployer.spi.local.LocalDeployerProperties;
import org.springframework.cloud.deployer.spi.local.LocalTaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates the appropriate Task Launcher Configuration based on the TaskLauncher
 * that is available in the classpath.
 * @author Glenn Renfro
 */

@Configuration
@ConditionalOnClass({TaskLauncher.class})
public class TaskLauncherConfiguration {

	@Configuration
	@ConditionalOnMissingBean(name = "taskLauncher")
	@ConditionalOnClass({LocalTaskLauncher.class})
	protected static class LocalTaskDeployerConfiguration {
		@Bean
		public TaskLauncher taskLauncher() {
			return new LocalTaskLauncher(new LocalDeployerProperties());
		}
	}
}
