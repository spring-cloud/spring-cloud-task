/*
 *  Copyright 2018 the original author or authors.
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

package org.springframework.cloud.task.batch.configuration;

import org.junit.Test;

import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.JobLauncherCommandLineRunner;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.batch.handler.TaskJobLauncherCommandLineRunner;
import org.springframework.cloud.task.batch.listener.TaskBatchExecutionListenerTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
public class TaskJobLauncherAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().
			withUserConfiguration(TaskBatchExecutionListenerTests.JobConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class,
					EmbeddedDataSourceConfiguration.class,
					BatchAutoConfiguration.class,
					TaskJobLauncherAutoConfiguration.class);

	@Test
	public void testAutoBuiltDataSourceWithTaskJobLauncherCLR() {
		this.contextRunner.
				withPropertyValues("spring.cloud.task.batch.failOnJobFailure=true").
				run(context -> {
			assertThat(context).hasSingleBean(TaskJobLauncherCommandLineRunner.class);
			assertThat(context).doesNotHaveBean(JobLauncherCommandLineRunner.class);
					assertThat(context.getBean(TaskJobLauncherCommandLineRunner.class)
							.getOrder())
							.isEqualTo(0);
		});
	}

	@Test
	public void testAutoBuiltDataSourceWithTaskJobLauncherCLROrder() {
		this.contextRunner.
				withPropertyValues("spring.cloud.task.batch.failOnJobFailure=true",
						"spring.cloud.task.batch.commandLineRunnerOrder=100").
				run(context -> {
					assertThat(context.getBean(TaskJobLauncherCommandLineRunner.class)
							.getOrder())
							.isEqualTo(100);
		});
	}

	@Test
	public void testAutoBuiltDataSourceWithTaskJobLauncherCLRDisabled() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(JobLauncherCommandLineRunner.class);
			assertThat(context).doesNotHaveBean(TaskJobLauncherCommandLineRunner.class);
		});
	}
}

