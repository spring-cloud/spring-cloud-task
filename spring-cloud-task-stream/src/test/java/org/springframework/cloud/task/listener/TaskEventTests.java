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

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.stream.config.BindingServiceConfiguration;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.cloud.task.configuration.SingleTaskConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.BridgeFrom;
import org.springframework.integration.channel.NullChannel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael Minella
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 */
public class TaskEventTests {

	@Test
	public void testDefaultConfiguration() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						EmbeddedDataSourceConfiguration.class,
						TaskEventAutoConfiguration.class,
						PropertyPlaceholderAutoConfiguration.class,
						TestSupportBinderAutoConfiguration.class,
						SimpleTaskAutoConfiguration.class, SingleTaskConfiguration.class,
						BindingServiceConfiguration.class))
				.withUserConfiguration(TaskEventsConfiguration.class)
				.withPropertyValues("spring.cloud.task.closecontext_enabled=false",
						"spring.main.web-environment=false");
		applicationContextRunner.run((context) -> {
			assertThat(context.getBean("taskEventListener")).isNotNull();
			assertThat(
					context.getBean(TaskEventAutoConfiguration.TaskEventChannels.class))
							.isNotNull();
		});
	}

	@EnableTask
	@Configuration
	public static class TaskEventsConfiguration {

		@Bean
		@BridgeFrom(TaskEventAutoConfiguration.TaskEventChannels.TASK_EVENTS)
		public NullChannel testEmptyChannel() {
			return new NullChannel();
		}

	}

}
