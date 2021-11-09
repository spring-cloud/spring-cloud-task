/*
 * Copyright 2021-2021 the original author or authors.
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.deployer.spi.task.LaunchState;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.task.launcher.configuration.TaskConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

public class TaskLauncherFunctionTests {

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

	@Test
	public void testProcessorFromFunction() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(
				TaskLauncherSinkTestApplication.class)).web(WebApplicationType.NONE).run(
			"--spring.jmx.enabled=false")) {

			InputDestination source = context.getBean(InputDestination.class);
			TaskLaunchRequest request = new TaskLaunchRequest(VALID_URL, Collections.emptyList(),
				Collections.emptyMap(), null, "TESTAPP1");
			GenericMessage<TaskLaunchRequest> message = new GenericMessage<>(request);
			source.send(message);
			TaskConfiguration.TestTaskLauncher target = context.getBean(TaskConfiguration.TestTaskLauncher.class);
			assertThat(target.status(DEFAULT_STATUS).getState())
				.isEqualTo(LaunchState.complete);
		}
	}

	@Test
	public void testSuccessWithParams() throws Exception {
		List<String> commandLineArgs = new ArrayList<>();
		commandLineArgs.add(PARAM1);
		commandLineArgs.add(PARAM2);
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(
				TaskLauncherSinkTestApplication.class)).web(WebApplicationType.NONE).run(
			"--spring.jmx.enabled=false")) {
			TaskConfiguration.TestTaskLauncher testTaskLauncher = launchTaskString(VALID_URL,
				commandLineArgs, null, context);
			verifySuccessWithParams(testTaskLauncher);

			testTaskLauncher = launchTaskByteArray(VALID_URL, commandLineArgs, null, context);
			verifySuccessWithParams(testTaskLauncher);

			testTaskLauncher = launchTaskTaskLaunchRequest(VALID_URL, commandLineArgs, null, context);
			verifySuccessWithParams(testTaskLauncher);
		}
	}

	@Test
	public void testSuccessWithAppName() throws Exception {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(
				TaskLauncherSinkTestApplication.class)).web(WebApplicationType.NONE).run(
			"--spring.jmx.enabled=false")) {
			TaskConfiguration.TestTaskLauncher testTaskLauncher = launchTaskString(VALID_URL,
				null, APP_NAME, context);
			verifySuccessWithAppName(testTaskLauncher);

			testTaskLauncher = launchTaskByteArray(VALID_URL, null, APP_NAME, context);
			verifySuccessWithAppName(testTaskLauncher);

			testTaskLauncher = launchTaskTaskLaunchRequest(VALID_URL, null, APP_NAME, context);
			verifySuccessWithAppName(testTaskLauncher);
		}
	}

	@Test
	public void testInvalidJar() throws Exception {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(
				TaskLauncherSinkTestApplication.class)).web(WebApplicationType.NONE).run(
			"--spring.jmx.enabled=false")) {
			TaskConfiguration.TestTaskLauncher testTaskLauncher = launchTaskTaskLaunchRequest(
				INVALID_URL, null, APP_NAME, context);
			verifySuccessWithAppName(testTaskLauncher);
		}
	}

	@Test
	public void testNoRun() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(
				TaskLauncherSinkTestApplication.class)).web(WebApplicationType.NONE).run(
			"--spring.jmx.enabled=false")) {
			TaskConfiguration.TestTaskLauncher testTaskLauncher = context
				.getBean(TaskConfiguration.TestTaskLauncher.class);
			assertThat(testTaskLauncher.status(DEFAULT_STATUS).getState())
				.isEqualTo(LaunchState.unknown);
		}
	}

	private void verifySuccessWithAppName(
		TaskConfiguration.TestTaskLauncher testTaskLauncher) {
		assertThat(testTaskLauncher.status(DEFAULT_STATUS).getState())
			.isEqualTo(LaunchState.complete);
		assertThat(testTaskLauncher.getCommandlineArguments().size()).isEqualTo(0);
		assertThat(testTaskLauncher.getApplicationName()).isEqualTo(APP_NAME);
	}

	private String getStringTaskLaunchRequest(String artifactURL,
		List<String> commandLineArgs, String applicationName) throws Exception {
		TaskLaunchRequest request = new TaskLaunchRequest(artifactURL, commandLineArgs,
			this.properties, null, applicationName);
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(request);
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

	private TaskConfiguration.TestTaskLauncher launchTaskString(String artifactURL,
		List<String> commandLineArgs, String applicationName,
		ConfigurableApplicationContext context) throws Exception {
		TaskConfiguration.TestTaskLauncher testTaskLauncher = context
			.getBean(TaskConfiguration.TestTaskLauncher.class);
		String stringRequest = getStringTaskLaunchRequest(artifactURL, commandLineArgs,
			applicationName);
		GenericMessage<String> message = new GenericMessage<>(stringRequest);
		InputDestination source = context.getBean(InputDestination.class);
		source.send(message);
		return testTaskLauncher;
	}

	private TaskConfiguration.TestTaskLauncher launchTaskByteArray(String artifactURL,
		List<String> commandLineArgs, String applicationName,
		ConfigurableApplicationContext context) throws Exception {
		TaskConfiguration.TestTaskLauncher testTaskLauncher = context
			.getBean(TaskConfiguration.TestTaskLauncher.class);
		String stringRequest = getStringTaskLaunchRequest(artifactURL, commandLineArgs,
			applicationName);
		GenericMessage<byte[]> message = new GenericMessage<>(stringRequest.getBytes());
		InputDestination source = context.getBean(InputDestination.class);
		source.send(message);
		return testTaskLauncher;
	}

	private TaskConfiguration.TestTaskLauncher launchTaskTaskLaunchRequest(
		String artifactURL, List<String> commandLineArgs, String applicationName,
		ConfigurableApplicationContext context)
		throws Exception {
		TaskConfiguration.TestTaskLauncher testTaskLauncher = context
			.getBean(TaskConfiguration.TestTaskLauncher.class);
		TaskLaunchRequest request = new TaskLaunchRequest(artifactURL, commandLineArgs,
			this.properties, null, applicationName);
		GenericMessage<TaskLaunchRequest> message = new GenericMessage<>(request);
		InputDestination source = context.getBean(InputDestination.class);
		source.send(message);
		return testTaskLauncher;
	}

	@SpringBootApplication
	@Import({TaskLauncherSink.class})
	public static class TaskLauncherSinkTestApplication {
	}
}
