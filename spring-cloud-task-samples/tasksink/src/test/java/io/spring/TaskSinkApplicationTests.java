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

package io.spring;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.task.launcher.TaskLaunchRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * @author Glenn Renfro
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TaskSinkApplication.class)
public class TaskSinkApplicationTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	private Sink sink;

	@Test
	public void testLaunch() throws IOException {
		assertNotNull(this.sink.input());

		TaskLauncher testTaskLauncher =
				 context.getBean(TaskLauncher.class);

		Map<String, String> properties = new HashMap();
		properties.put("server.port", "0");
		TaskLaunchRequest request = new TaskLaunchRequest(
				"maven://org.springframework.cloud.task.app:"
				+ "timestamp-task:jar:1.0.1.RELEASE", null, properties,
				null, null);
		GenericMessage<TaskLaunchRequest> message = new GenericMessage<TaskLaunchRequest>(request);
		this.sink.input().send(message);

		ArgumentCaptor<AppDeploymentRequest> deploymentRequest = ArgumentCaptor.forClass(AppDeploymentRequest.class);

		verify(testTaskLauncher).launch(deploymentRequest.capture());

		AppDeploymentRequest actualRequest = deploymentRequest.getValue();

		assertTrue(actualRequest.getCommandlineArguments().isEmpty());
		assertEquals("0", actualRequest.getDefinition().getProperties().get("server.port"));
		assertTrue(actualRequest.getResource().toString()
				.contains("maven://org.springframework.cloud.task.app:timestamp-task:jar:1.0.1.RELEASE"));
	}
}
