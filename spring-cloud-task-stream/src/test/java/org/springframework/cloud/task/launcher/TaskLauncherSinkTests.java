/*
 * Copyright 2016-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.spi.task.LaunchState;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.task.launcher.configuration.TaskConfiguration;
import org.springframework.cloud.task.launcher.util.TaskLauncherSinkApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TaskLauncherSinkApplication.class, TaskConfiguration.class })
public class TaskLauncherSinkTests {

	private final static String TASK_NAME_PREFIX = "Task-";

	private final static String APP_NAME = "MY_APP_NAME";

	private final static String PARAM1 = "FOO";

	private final static String PARAM2 = "BAR";

	private final static String VALID_URL = "maven://org.springframework.cloud.task.app:"
			+ "timestamp-task:jar:1.0.1.RELEASE";

	private final static String INVALID_URL = "maven://not.real.group:"
			+ "invalid:jar:1.0.0.BUILD-SNAPSHOT";

	private final static String DEFAULT_STATUS = "test_status";

	private Map<String, String> properties;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private Sink sink;

	@Before
	public void setup() {
		this.properties = new HashMap<>();
		this.properties.put("server.port", "0");
	}

	@Test
	public void testSuccessWithParams() throws Exception {
		List<String> commandLineArgs = new ArrayList<>();
		commandLineArgs.add(PARAM1);
		commandLineArgs.add(PARAM2);

		TaskConfiguration.TestTaskLauncher testTaskLauncher = launchTaskString(VALID_URL,
				commandLineArgs, null);
		verifySuccessWithParams(testTaskLauncher);

		testTaskLauncher = launchTaskByteArray(VALID_URL, commandLineArgs, null);
		verifySuccessWithParams(testTaskLauncher);

		testTaskLauncher = launchTaskTaskLaunchRequest(VALID_URL, commandLineArgs, null);
		verifySuccessWithParams(testTaskLauncher);
	}

	private void verifySuccessWithParams(
			TaskConfiguration.TestTaskLauncher testTaskLauncher) {
		assertThat(testTaskLauncher.status(DEFAULT_STATUS).getState())
				.isEqualTo(LaunchState.complete);
		assertThat(testTaskLauncher.getCommandlineArguments().size()).isEqualTo(2);
		assertThat(testTaskLauncher.getCommandlineArguments().get(0)).isEqualTo(PARAM1);
		assertThat(testTaskLauncher.getCommandlineArguments().get(1)).isEqualTo(PARAM2);
		assertThat(testTaskLauncher.getApplicationName().startsWith(TASK_NAME_PREFIX))
				.isTrue();
	}

	@Test
	public void testSuccessWithAppName() throws Exception {
		TaskConfiguration.TestTaskLauncher testTaskLauncher = launchTaskString(VALID_URL,
				null, APP_NAME);
		verifySuccessWithAppName(testTaskLauncher);

		testTaskLauncher = launchTaskByteArray(VALID_URL, null, APP_NAME);
		verifySuccessWithAppName(testTaskLauncher);

		testTaskLauncher = launchTaskTaskLaunchRequest(VALID_URL, null, APP_NAME);
		verifySuccessWithAppName(testTaskLauncher);
	}

	@Test
	public void testInvalidJar() throws Exception {
		TaskConfiguration.TestTaskLauncher testTaskLauncher = launchTaskTaskLaunchRequest(
				INVALID_URL, null, APP_NAME);
		verifySuccessWithAppName(testTaskLauncher);
	}

	private void verifySuccessWithAppName(
			TaskConfiguration.TestTaskLauncher testTaskLauncher) {
		assertThat(testTaskLauncher.status(DEFAULT_STATUS).getState())
				.isEqualTo(LaunchState.complete);
		assertThat(testTaskLauncher.getCommandlineArguments().size()).isEqualTo(0);
		assertThat(testTaskLauncher.getApplicationName()).isEqualTo(APP_NAME);
	}

	@Test
	public void testSuccessNoParams() throws Exception {
		TaskConfiguration.TestTaskLauncher testTaskLauncher = launchTaskString(VALID_URL,
				null, null);
		verifySuccessWithNoParams(testTaskLauncher);

		testTaskLauncher = launchTaskByteArray(VALID_URL, null, null);
		verifySuccessWithNoParams(testTaskLauncher);

		testTaskLauncher = launchTaskTaskLaunchRequest(VALID_URL, null, null);
		verifySuccessWithNoParams(testTaskLauncher);
	}

	private void verifySuccessWithNoParams(
			TaskConfiguration.TestTaskLauncher testTaskLauncher) {
		assertThat(testTaskLauncher.status(DEFAULT_STATUS).getState())
				.isEqualTo(LaunchState.complete);
		assertThat(testTaskLauncher.getCommandlineArguments().size()).isEqualTo(0);
		assertThat(testTaskLauncher.getApplicationName().startsWith(TASK_NAME_PREFIX))
				.isTrue();
	}

	@Test
	public void testNoRun() {
		TaskConfiguration.TestTaskLauncher testTaskLauncher = this.context
				.getBean(TaskConfiguration.TestTaskLauncher.class);
		assertThat(testTaskLauncher.status(DEFAULT_STATUS).getState())
				.isEqualTo(LaunchState.unknown);
	}

	private TaskConfiguration.TestTaskLauncher launchTaskString(String artifactURL,
			List<String> commandLineArgs, String applicationName) throws Exception {
		TaskConfiguration.TestTaskLauncher testTaskLauncher = this.context
				.getBean(TaskConfiguration.TestTaskLauncher.class);
		String stringRequest = getStringTaskLaunchRequest(artifactURL, commandLineArgs,
				applicationName);
		GenericMessage<String> message = new GenericMessage<>(stringRequest);

		this.sink.input().send(message);
		return testTaskLauncher;
	}

	private TaskConfiguration.TestTaskLauncher launchTaskByteArray(String artifactURL,
			List<String> commandLineArgs, String applicationName) throws Exception {
		TaskConfiguration.TestTaskLauncher testTaskLauncher = this.context
				.getBean(TaskConfiguration.TestTaskLauncher.class);
		String stringRequest = getStringTaskLaunchRequest(artifactURL, commandLineArgs,
				applicationName);
		GenericMessage<byte[]> message = new GenericMessage<>(stringRequest.getBytes());

		this.sink.input().send(message);
		return testTaskLauncher;
	}

	private String getStringTaskLaunchRequest(String artifactURL,
			List<String> commandLineArgs, String applicationName) throws Exception {
		TaskLaunchRequest request = new TaskLaunchRequest(artifactURL, commandLineArgs,
				this.properties, null, applicationName);
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(request);
	}

	private TaskConfiguration.TestTaskLauncher launchTaskTaskLaunchRequest(
			String artifactURL, List<String> commandLineArgs, String applicationName)
			throws Exception {
		TaskConfiguration.TestTaskLauncher testTaskLauncher = this.context
				.getBean(TaskConfiguration.TestTaskLauncher.class);
		TaskLaunchRequest request = new TaskLaunchRequest(artifactURL, commandLineArgs,
				this.properties, null, applicationName);
		GenericMessage<TaskLaunchRequest> message = new GenericMessage<>(request);

		this.sink.input().send(message);
		return testTaskLauncher;
	}

}
