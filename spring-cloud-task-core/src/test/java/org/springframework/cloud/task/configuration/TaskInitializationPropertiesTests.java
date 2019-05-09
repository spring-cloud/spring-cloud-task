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

package org.springframework.cloud.task.configuration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Suite.class)
@SuiteClasses({ TaskInitializationPropertiesTests.TableInitializationEnabledTest.class })

@DirtiesContext
public class TaskInitializationPropertiesTests {

	@Autowired
	TaskInitializationProperties taskProperties;

	@Test
	public void test() {
		assertThat(this.taskProperties.isEnable()).isFalse();
	}

	@RunWith(SpringRunner.class)
	@SpringBootTest(
			classes = { TaskInitializationPropertiesTests.Config.class,
					SimpleTaskAutoConfiguration.class, SingleTaskConfiguration.class },
			properties = { "spring.cloud.task.initialize.enable=false" })

	@DirtiesContext
	public static class TableInitializationEnabledTest
			extends TaskInitializationPropertiesTests {

	}

	@Configuration
	public static class Config {

	}

}
