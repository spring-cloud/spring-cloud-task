/*
 * Copyright 2016-present the original author or authors.
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

package io.spring.cloud;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.task.batch.listener.support.JobExecutionEvent;
import org.springframework.cloud.task.batch.listener.support.TaskEventProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("DockerRequired")
public class BatchEventsApplicationTests {

	private static final String TASK_NAME = "taskEventTest";

	private ConfigurableApplicationContext applicationContext;

	private ObjectMapper objectMapper;

	private final TaskEventProperties taskEventProperties = new TaskEventProperties();

	@BeforeEach
	public void setup() {
		objectMapper = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
	}

	@AfterEach
	public void tearDown() {
		if (this.applicationContext != null && this.applicationContext.isActive()) {
			this.applicationContext.close();
		}
	}

	@Test
	public void testExecution() throws Exception {
		List<Message<byte[]>> result = testListener(taskEventProperties.getJobExecutionEventBindingName(), 1);
		JobExecutionEvent jobExecutionEvent = this.objectMapper.readValue(result.get(0).getPayload(),
				JobExecutionEvent.class);
		assertThat(jobExecutionEvent.getJobInstance().getJobName()).isEqualTo("job").as("Job name should be job");
	}

	private String[] getCommandLineParams(boolean enableFailJobConfig) {
		String jobConfig = enableFailJobConfig ? "--spring.cloud.task.test.enable-job-configuration=true"
				: "--spring.cloud.task.test.enable-fail-job-configuration=true";
		return new String[] { "--spring.cloud.task.closecontext_enable=false", "--spring.cloud.task.name=" + TASK_NAME,
				"--spring.main.web-environment=false", "--spring.cloud.stream.defaultBinder=rabbit",
				"--spring.cloud.stream.bindings.task-events.destination=test", jobConfig, "foo=" + UUID.randomUUID() };
	}

	private List<Message<byte[]>> testListener(String bindingName, int numberToRead) {
		List<Message<byte[]>> results = new ArrayList<>();
		this.applicationContext = new SpringApplicationBuilder()
			.sources(TestChannelBinderConfiguration.getCompleteConfiguration(BatchEventsTestApplication.class))
			.web(WebApplicationType.NONE)
			.build()
			.run(getCommandLineParams(true));
		OutputDestination target = this.applicationContext.getBean(OutputDestination.class);
		for (int i = 0; i < numberToRead; i++) {
			results.add(target.receive(10000, bindingName));
		}
		return results;
	}

	@SpringBootApplication
	@Import({ BatchEventsApplication.class })
	public static class BatchEventsTestApplication {

	}

}
