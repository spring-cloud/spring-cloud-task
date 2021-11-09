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

package org.springframework.cloud.task.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

/**
 * @author Michael Minella
 * @author Ilayaperumal Gopinathan
 */
public class TaskEventTests {

	private static final String TASK_NAME = "taskEventTest";

	private final ObjectMapper objectMapper = new ObjectMapper();

	private ConfigurableApplicationContext applicationContext;

	@AfterEach
	public void tearDown() {
		if (this.applicationContext != null && this.applicationContext.isActive()) {
			this.applicationContext.close();
		}
	}

	@Test
	public void testTaskEventListener() throws Exception {
		List<Message<byte[]>> result = testListener(
			"--spring.cloud.task.batch.events.itemWriteEventBindingName=task-events",
			"task-events", 2);
		TaskExecution taskExecution = this.objectMapper.readValue(result.get(0).getPayload(),
			TaskExecution.class);
		Assertions.assertThat(taskExecution.getTaskName()).isEqualTo(TASK_NAME)
			.as(String.format("Task name should be '%s'", TASK_NAME));
		taskExecution = this.objectMapper.readValue(result.get(1).getPayload(),
			TaskExecution.class);
		Assertions.assertThat(taskExecution.getTaskName()).isEqualTo(TASK_NAME)
			.as(String.format("Task name should be '%s'", TASK_NAME));
	}

	private List<Message<byte[]>> testListener(String channelBinding, String bindingName, int numberToRead) {
		List<Message<byte[]>> results = new ArrayList<>();
		this.applicationContext = new SpringApplicationBuilder()
			.sources(TestChannelBinderConfiguration
				.getCompleteConfiguration(BatchExecutionEventTests.BatchEventsApplication.class)).web(WebApplicationType.NONE).build()
			.run(getCommandLineParams(channelBinding));
		OutputDestination target = this.applicationContext.getBean(OutputDestination.class);
		for (int i = 0; i < numberToRead; i++) {
			results.add(target.receive(10000, bindingName));
		}
		return results;
	}

	private String[] getCommandLineParams(String sinkChannelParam) {
		return new String[]{"--spring.cloud.task.closecontext_enable=false",
			"--spring.cloud.task.name=" + TASK_NAME,
			"--spring.main.web-environment=false",
			"--spring.cloud.stream.defaultBinder=rabbit",
			"foo=" + UUID.randomUUID(), sinkChannelParam};
	}

	@EnableTask
	@Configuration
	public static class TaskEventsConfiguration {

	}
}
