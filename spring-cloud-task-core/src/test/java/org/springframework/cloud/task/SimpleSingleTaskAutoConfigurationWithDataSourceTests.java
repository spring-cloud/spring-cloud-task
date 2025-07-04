/*
 * Copyright 2017-2025 the original author or authors.
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

package org.springframework.cloud.task;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.cloud.task.configuration.SingleInstanceTaskListener;
import org.springframework.cloud.task.configuration.SingleTaskConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the beans created by the SimpleSingleTaskAutoConfigurationConfiguration
 * specifically that the JdbcLockRegistry was selected.
 *
 * @author Glenn Renfro
 * @since 2.0.0
 */
public class SimpleSingleTaskAutoConfigurationWithDataSourceTests {

	@Test
	public void testConfiguration() {

		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class, SimpleTaskAutoConfiguration.class,
							SingleTaskConfiguration.class, EmbeddedDataSourceConfiguration.class))
			.withPropertyValues("spring.cloud.task.singleInstanceEnabled=true");
		applicationContextRunner.run((context) -> {
			SingleInstanceTaskListener singleInstanceTaskListener = context.getBean(SingleInstanceTaskListener.class);

			assertThat(singleInstanceTaskListener).as("singleInstanceTaskListener should not be null").isNotNull();

			assertThat(SingleInstanceTaskListener.class).isEqualTo(singleInstanceTaskListener.getClass());
		});
	}

}
