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

package io.spring;

import java.util.HashMap;
import java.util.Map;

import io.spring.configuration.TaskSinkConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.deployer.spi.task.LaunchState;
import org.springframework.cloud.stream.annotation.Bindings;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.task.launcher.TaskLaunchRequest;
import org.springframework.cloud.task.launcher.TaskLauncherSink;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Glenn Renfro
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TaskSinkApplication.class)
public class TaskSinkApplicationTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	@Bindings(TaskLauncherSink.class)
	private Sink sink;

	@Test
	public void testLaunch() {
		assertNotNull(this.sink.input());

		TaskSinkConfiguration.TestTaskLauncher testTaskLauncher =
				(TaskSinkConfiguration.TestTaskLauncher) context.getBean(TaskSinkConfiguration.TestTaskLauncher.class);

		Map<String, String> properties = new HashMap();
		properties.put("server.port", "0");
		TaskLaunchRequest request = new TaskLaunchRequest("maven://org.springframework.cloud.task.app:"
				+ "timestamp-task:jar:1.0.0.BUILD-SNAPSHOT", null, properties, null);
		GenericMessage<TaskLaunchRequest> message = new GenericMessage<TaskLaunchRequest>(request);
		this.sink.input().send(message);
		assertEquals(LaunchState.complete, testTaskLauncher.status("TESTSTATUS").getState());
	}
}
