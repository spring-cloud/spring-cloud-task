/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.cloud.task.listener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.rabbit.config.RabbitServiceAutoConfiguration;
import org.springframework.cloud.stream.binder.test.junit.rabbit.RabbitTestSupport;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Michael Minella
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TaskEventTests.ListenerBinding.class})
public class TaskEventTests {

	@ClassRule
	public static RabbitTestSupport rabbitTestSupport = new RabbitTestSupport();

	// Count for two task execution events per task
	static CountDownLatch latch = new CountDownLatch(2);

	private static final String TASK_NAME = "taskEventTest";

	@Test
	public void testTaskEventListener() throws Exception {
		ConfigurableApplicationContext applicationContext = new SpringApplicationBuilder().sources(new Class[] {TaskEventsConfiguration.class,
				TaskEventAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				RabbitServiceAutoConfiguration.class}).build().run("--spring.cloud.task.closecontext_enabled=false",
				"--spring.cloud.task.name=" + TASK_NAME,
				"--spring.main.web-environment=false",
				"--spring.cloud.stream.defaultBinder=rabbit",
				"--spring.cloud.stream.bindings.task-events.destination=test");
		assertNotNull(applicationContext.getBean("taskEventListener"));
		assertNotNull(applicationContext.getBean(TaskEventAutoConfiguration.TaskEventChannels.class));
		assertTrue(latch.await(1, TimeUnit.SECONDS));
	}

	@Configuration
	@EnableTask
	public static class TaskEventsConfiguration {
	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:/org/springframework/cloud/task/listener/sink-channel.properties")
	@EnableAutoConfiguration
	public static class ListenerBinding {

		@StreamListener(Sink.INPUT)
		public void receive(TaskExecution execution) {
			assertTrue(String.format("Task name should be '%s'", TASK_NAME), execution.getTaskName().equals(TASK_NAME));
			latch.countDown();
		}
	}
}
