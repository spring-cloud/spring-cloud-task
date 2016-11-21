/*
 *  Copyright 2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.launcher;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.spi.local.LocalDeployerProperties;
import org.springframework.cloud.deployer.spi.local.LocalTaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Tests the TaskLauncherConfiguration in a case where a TaskLauncher is already
 * present.
 *
 * @author  Glenn Renfro
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes =
		{TaskLaunchConfigurationExistingTests.TestTaskDeployerConfiguration.class})
public class TaskLaunchConfigurationExistingTests {

	@Autowired
	private ApplicationContext context;

	private static TaskLauncher testTaskLauncher;

	@Test
	public void testTaskLauncher() {
		LocalTaskLauncher taskLauncher =
				context.getBean(LocalTaskLauncher.class);
		assertNotNull(testTaskLauncher);
		assertNotNull(taskLauncher);
		assertEquals(testTaskLauncher, taskLauncher);
	}

	@Configuration
	protected static class TestTaskDeployerConfiguration {
		@Bean
		public TaskLauncher taskLauncher() {
			testTaskLauncher =
					new LocalTaskLauncher(new LocalDeployerProperties());
			return testTaskLauncher;
		}
	}
}
