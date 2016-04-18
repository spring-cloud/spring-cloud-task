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
package org.springframework.cloud.task.listener;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.redis.config.RedisServiceAutoConfiguration;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.stream.test.junit.redis.RedisTestSupport;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Michael Minella
 * @author Ilayaperumal Gopinathan
 */
public class TaskEventTests {

	@Rule
	public RedisTestSupport redisTestSupport = new RedisTestSupport();

	private static final String TASK_NAME = "taskEventTest";

	@Test
	public void testDefaultConfiguration() {
		ConfigurableApplicationContext applicationContext =
				SpringApplication.run(new Object[]{ TaskEventsConfiguration.class,
								TaskEventAutoConfiguration.class,
								PropertyPlaceholderAutoConfiguration.class },
						new String[]{ "--spring.cloud.task.closecontext.enable=false",
								"--spring.main.web-environment=false",
								"--spring.cloud.stream.defaultBinder=test" });

		assertNotNull(applicationContext.getBean("taskEventListener"));
		assertNotNull(applicationContext.getBean(TaskEventAutoConfiguration.TaskEventChannels.class));
	}

	@Configuration
	@EnableTask
	public static class TaskEventsConfiguration {
	}

}
