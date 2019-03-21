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

package org.springframework.cloud.task.listener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.rabbit.config.RabbitServiceAutoConfiguration;
import org.springframework.cloud.stream.binder.test.junit.rabbit.RabbitTestSupport;
import org.springframework.cloud.stream.config.BindingServiceConfiguration;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael Minella
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TaskEventTests.ListenerBinding.class })
public class TaskEventTests {

	private static final String TASK_NAME = "taskEventTest";

	@ClassRule
	public static RabbitTestSupport rabbitTestSupport = new RabbitTestSupport();

	// Count for two task execution events per task
	static CountDownLatch latch = new CountDownLatch(2);

	@Test
	public void testTaskEventListener() throws Exception {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(TaskEventAutoConfiguration.class,
						PropertyPlaceholderAutoConfiguration.class,
						RabbitServiceAutoConfiguration.class,
						SimpleTaskAutoConfiguration.class,
						BindingServiceConfiguration.class))
				.withUserConfiguration(TaskEventsConfiguration.class)
				.withPropertyValues("--spring.cloud.task.closecontext_enabled=false",
						"--spring.cloud.task.name=" + TASK_NAME,
						"--spring.main.web-environment=false",
						"--spring.cloud.stream.defaultBinder=rabbit",
						"--spring.cloud.stream.bindings.task-events.destination=test");
		applicationContextRunner.run((context) -> {
			assertThat(context.getBean("taskEventListener")).isNotNull();
			assertThat(
					context.getBean(TaskEventAutoConfiguration.TaskEventChannels.class))
							.isNotNull();
		});
		assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
	}

	@EnableTask
	@Configuration
	public static class TaskEventsConfiguration {

	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:/org/springframework/cloud/task/listener/sink-channel.properties")
	@EnableAutoConfiguration
	public static class ListenerBinding {

		@StreamListener(Sink.INPUT)
		public void receive(TaskExecution execution) {
			Assertions.assertThat(execution.getTaskName()).isEqualTo(TASK_NAME)
					.as(String.format("Task name should be '%s'", TASK_NAME));
			latch.countDown();
		}

		@Bean
		public CommandLineRunner commandLineRunner() {
			return new CommandLineRunner() {
				@Override
				public void run(String... args) throws Exception {
					System.out.println("Hello World");
				}
			};
		}

	}

}
