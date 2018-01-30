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

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.JobLauncherCommandLineRunner;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.batch.handler.TaskJobLauncherCommandLineRunner;
import org.springframework.cloud.task.batch.listener.TaskBatchExecutionListenerTests;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
public class TaskJobLauncherAutoConfigurationTests {

	private ConfigurableApplicationContext applicationContext;

	@After
	public void tearDown() {
		if(this.applicationContext != null) {
			this.applicationContext.close();
		}
	}

	@Test
	public void testAutobuiltDataSourceWithTaskJobLauncherCLR() {
		String [] enabledArgs = new String[] {
				"--spring.cloud.task.batch.commandLineRunnerEnabled=true"};
		this.applicationContext = SpringApplication.run(new Class[]
				{TaskBatchExecutionListenerTests.JobConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				BatchAutoConfiguration.class,
				TaskJobLauncherAutoConfiguration.class}, enabledArgs);
		assertThat(this.applicationContext.getBean(TaskJobLauncherCommandLineRunner.class)).isNotNull();
		boolean exceptionThrown = false;
		try {
			this.applicationContext.getBean(JobLauncherCommandLineRunner.class);
		}
		catch(NoSuchBeanDefinitionException exception) {
			exceptionThrown = true;
		}
		assertThat(exceptionThrown).isTrue();
	}

	@Test
	public void testAutobuiltDataSourceWithTaskJobLauncherCLRDisabled() {
		String [] enabledArgs = {};
		this.applicationContext = SpringApplication.run(new Class[]
				{TaskBatchExecutionListenerTests.JobConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				BatchAutoConfiguration.class,
				TaskJobLauncherAutoConfiguration.class}, enabledArgs);
		assertThat(this.applicationContext.getBean(JobLauncherCommandLineRunner.class)).isNotNull();
		boolean exceptionThrown = false;
		try {
			this.applicationContext.getBean(TaskJobLauncherCommandLineRunner.class);
		}
		catch(NoSuchBeanDefinitionException exception) {
			exceptionThrown = true;
		}
		assertThat(exceptionThrown).isTrue();
	}
}
