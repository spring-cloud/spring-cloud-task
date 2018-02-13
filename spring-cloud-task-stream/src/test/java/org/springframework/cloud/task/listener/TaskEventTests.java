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

import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertNotNull;

/**
 * @author Michael Minella
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 */
public class TaskEventTests {

	private static final String TASK_NAME = "taskEventTest";

	@Test
	public void testDefaultConfiguration() {
		ConfigurableApplicationContext applicationContext =
				SpringApplication.run(new Class[]{PropertyPlaceholderAutoConfiguration.class,EmbeddedDataSourceConfiguration.class,TaskEventsConfiguration.class,
								TaskEventAutoConfiguration.class,
								TestSupportBinderAutoConfiguration.class},
						new String[]{ "--spring.cloud.task.closecontext_enabled=false",
								"--spring.main.web-environment=false"});

		assertNotNull(applicationContext.getBean("taskEventListener"));
		assertNotNull(applicationContext.getBean(TaskEventAutoConfiguration.TaskEventChannels.class));
	}

	@Configuration
	@EnableTask
	public static class TaskEventsConfiguration {
	}

}
