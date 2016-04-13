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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.deployer.spi.task.LaunchState;
import org.springframework.cloud.stream.annotation.Bindings;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.task.launcher.configuration.TaskConfiguration;
import org.springframework.cloud.task.launcher.util.TaskLauncherSinkApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {TaskLauncherSinkApplication.class, TaskConfiguration.class} )
public class TaskLauncherSinkTests {

	private final static String DEFAULT_STATUS = "test_status";

	@Autowired
	private ApplicationContext context;

	@Autowired
	@Bindings(TaskLauncherSink.class)
	private Sink sink;

	@Test
	public void testSuccess() {
		TaskConfiguration.TestTaskLauncher testTaskLauncher =
				 context.getBean(TaskConfiguration.TestTaskLauncher.class);

		Map<String, String> properties = new HashMap<>();
		properties.put("server.port", "0");
		TaskLaunchRequest request = new TaskLaunchRequest("timestamp-task",
				"org.springframework.cloud.task.module","1.0.0.BUILD-SNAPSHOT", "jar",
				"exec", properties);
		GenericMessage<TaskLaunchRequest> message = new GenericMessage<>(request);
		this.sink.input().send(message);
		assertEquals(LaunchState.complete, testTaskLauncher.status(DEFAULT_STATUS).getState());
	}

	@Test
	public void testNoRun() {
		TaskConfiguration.TestTaskLauncher testTaskLauncher =
				 context.getBean(TaskConfiguration.TestTaskLauncher.class);

		assertEquals(LaunchState.unknown, testTaskLauncher.status(DEFAULT_STATUS).getState());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoTaskLauncher() {
		Map<String, String> properties = new HashMap<>();
		properties.put("server.port", "0");
		TaskLauncherSink sink = new TaskLauncherSink();
		sink.taskLauncherSink(new TaskLaunchRequest("timestamp-task",
				"org.springframework.cloud.task.module","1.0.0.BUILD-SNAPSHOT", "jar",
				"exec", properties));
	}
}
