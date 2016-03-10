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

package org.springframework.cloud.task.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A sink stream application that launches a tasks.
 *
 * @author Glenn Renfro
 */

@EnableBinding(Sink.class)
public class TaskLauncherSink {

	private final static Logger logger = LoggerFactory.getLogger(TaskLauncherSink.class);

	@Autowired
	public TaskLauncher taskLauncher;

	/**
	 * Launches a task upon the receipt of a valid TaskLaunchRequest.
	 * @param message is a TaskLaunchRequest containing the information required to launch
	 * a task.
	 */
	@ServiceActivator(inputChannel = Sink.INPUT)
	public void taskLauncherSink(Message<?> message) {
		Assert.isInstanceOf(TaskLaunchRequest.class, message.getPayload(),
				"Message Payload did not contain a object of type TaskLaunchRequest");
		launchTask((TaskLaunchRequest) message.getPayload());
	}

	private void launchTask(TaskLaunchRequest taskLaunchRequest) {
		Assert.notNull(taskLauncher, "TaskLauncher has not been initialized");
		logger.info("Launching Task for the following resource " + taskLaunchRequest);
		MavenResource resource = new MavenResource.Builder()
				.setArtifactId(taskLaunchRequest.getArtifact())
				.setGroupId(taskLaunchRequest.getTaskGroupId())
				.setVersion(taskLaunchRequest.getTaskVersion())
				.setExtension(taskLaunchRequest.getTaskExtension())
				.setClassifier(taskLaunchRequest.getTaskClassifier())
				.build();
		AppDefinition definition = new AppDefinition(taskLaunchRequest.getArtifact(), taskLaunchRequest.getProperties());
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource);
		taskLauncher.launch(request);
	}

}
