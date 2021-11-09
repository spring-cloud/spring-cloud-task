/*
 * Copyright 2016-2021 the original author or authors.
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

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A sink stream application that launches a tasks.
 *
 * @author Glenn Renfro
 */

public class TaskLauncherSink {

	private final static Logger logger = LoggerFactory.getLogger(TaskLauncherSink.class);

	// @checkstyle:off
	@Autowired
	public TaskLauncher taskLauncher;

	// @checkstyle:on

	@Autowired
	private DelegatingResourceLoader resourceLoader;

	/**
	 * Launches a task upon the receipt of a valid TaskLaunchRequest.
	 * @return the {@link Consumer} that will retrieve messages from binder.
	 */
	@Bean
	public Consumer<Message<TaskLaunchRequest>> taskLauncherSink() {
		return messagePayload -> {
			launchTask(messagePayload.getPayload());
		};
	}

	private void launchTask(TaskLaunchRequest taskLaunchRequest) {
		Assert.notNull(this.taskLauncher, "TaskLauncher has not been initialized");
		logger.info("Launching Task for the following uri " + taskLaunchRequest.getUri());
		Resource resource = this.resourceLoader.getResource(taskLaunchRequest.getUri());
		AppDefinition definition = new AppDefinition(
				taskLaunchRequest.getApplicationName(),
				taskLaunchRequest.getEnvironmentProperties());
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource,
				taskLaunchRequest.getDeploymentProperties(),
				taskLaunchRequest.getCommandlineArguments());
		this.taskLauncher.launch(request);
	}

}
