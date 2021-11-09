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

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael Minella
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 */
public class TaskEventTests {

	@Test
	public void testDefaultConfiguration() {
		ConfigurableApplicationContext applicationContext = new SpringApplicationBuilder()
			.sources(TestChannelBinderConfiguration
				.getCompleteConfiguration(TaskEventsApplication.class)).web(WebApplicationType.NONE).build()
			.run();
		assertThat(applicationContext.getBean("taskEventEmitter")).isNotNull();
	}

	@EnableTask
	@SpringBootApplication
	public static class TaskEventsApplication {
	}

}
