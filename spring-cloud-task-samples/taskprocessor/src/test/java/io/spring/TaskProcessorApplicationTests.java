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

package io.spring;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.task.launcher.TaskLaunchRequest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */

public class TaskProcessorApplicationTests {

	private static final String DEFAULT_PAYLOAD = "hello";

	private final ObjectMapper objectMapper = new ObjectMapper();
	private ConfigurableApplicationContext applicationContext;

	@BeforeEach
	public void setup() {
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@AfterEach
	public void tearDown() {
		if (this.applicationContext != null && this.applicationContext.isActive()) {
			this.applicationContext.close();
		}
	}

	@Test
	public void test() throws IOException {
		Map<String, String> properties = new HashMap();
		properties.put("payload", DEFAULT_PAYLOAD);
		TaskLaunchRequest expectedRequest = new TaskLaunchRequest(
			"maven://org.springframework.cloud.task.app:"
				+ "timestamp-task:jar:1.0.1.RELEASE", null, properties,
			null, null);
		List<Message<byte[]>> result = testListener("output", 1);

		TaskLaunchRequest tlq = objectMapper.readValue(result.get(0).getPayload(), TaskLaunchRequest.class);
		assertThat(tlq).isEqualTo(expectedRequest);
	}


	private List<Message<byte[]>> testListener(String bindingName, int numberToRead) {
		List<Message<byte[]>> results = new ArrayList<>();
		this.applicationContext = new SpringApplicationBuilder()
			.sources(TestChannelBinderConfiguration
				.getCompleteConfiguration(TaskProcessorTestApplication.class)).web(WebApplicationType.NONE)
			.run();

		InputDestination input = this.applicationContext.getBean(InputDestination.class);
		OutputDestination target = this.applicationContext.getBean(OutputDestination.class);
		input.send(new GenericMessage<>(DEFAULT_PAYLOAD.getBytes(StandardCharsets.UTF_8)));
		for (int i = 0; i < numberToRead; i++) {
			results.add(target.receive(10000, bindingName));
		}
		return results;
	}

	@SpringBootApplication
	@Import({TaskProcessor.class})
	public static class TaskProcessorTestApplication {
	}
}
