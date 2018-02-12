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

package org.springframework.cloud.task.batch.handler;

import java.util.Set;

import org.junit.After;
import org.junit.Test;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.JobLauncherCommandLineRunner;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.batch.configuration.TaskBatchAutoConfiguration;
import org.springframework.cloud.task.batch.configuration.TaskJobLauncherAutoConfiguration;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * @author Glenn Renfro
 */
public class TaskJobLauncherCommandLineRunnerTests {

	private ConfigurableApplicationContext applicationContext;

	@After
	public void tearDown() {
		if (this.applicationContext != null) {
			this.applicationContext.close();
		}
	}

	@Test
	public void testTaskJobLauncherCLRSuccessFail() {
		String[] enabledArgs = new String[] { "--spring.cloud.task.batch.failOnJobFailure=true" };
		boolean isExceptionThrown = false;
		try {
			this.applicationContext = SpringApplication
					.run(new Class[] { TaskJobLauncherCommandLineRunnerTests.JobWithFailureConfiguration.class,
							PropertyPlaceholderAutoConfiguration.class,
							EmbeddedDataSourceConfiguration.class,
							BatchAutoConfiguration.class,
							TaskBatchAutoConfiguration.class,
							TaskJobLauncherAutoConfiguration.class }, enabledArgs);
		}
		catch (IllegalStateException exception) {
			isExceptionThrown = true;
		}
		assertThat(isExceptionThrown).isTrue();
	}

	@Test
	public void testTaskJobLauncherPickOneJob() {
		String[] enabledArgs = new String[] {
				"--spring.cloud.task.batch.failOnJobFailure=true",
				"--spring.cloud.task.batch.jobNames=jobSucceed" };
		boolean isExceptionThrown = false;
		try {
			this.applicationContext = SpringApplication
					.run(new Class[] { TaskJobLauncherCommandLineRunnerTests.JobWithFailureConfiguration.class,
							PropertyPlaceholderAutoConfiguration.class,
							EmbeddedDataSourceConfiguration.class,
							BatchAutoConfiguration.class,
							TaskBatchAutoConfiguration.class,
							TaskJobLauncherAutoConfiguration.class }, enabledArgs);
		}
		catch (IllegalStateException exception) {
			isExceptionThrown = true;
		}
		assertThat(isExceptionThrown).isFalse();
		validateContext();
	}

	@Test
	public void testCommandLineRunnerSetToFalse() {
		String[] enabledArgs = new String[] { };
		this.applicationContext = SpringApplication
				.run(new Class[] { TaskJobLauncherCommandLineRunnerTests.JobConfiguration.class,
						PropertyPlaceholderAutoConfiguration.class,
						EmbeddedDataSourceConfiguration.class,
						BatchAutoConfiguration.class,
						TaskBatchAutoConfiguration.class,
						TaskJobLauncherAutoConfiguration.class }, enabledArgs);
		validateContext();
		assertThat(applicationContext.getBean(JobLauncherCommandLineRunner.class)).isNotNull();
		boolean exceptionThrown = false;
		try {
			applicationContext.getBean(TaskJobLauncherCommandLineRunner.class);
		}
		catch (NoSuchBeanDefinitionException exception) {
			exceptionThrown = true;
		}
		assertThat(exceptionThrown).isTrue();
		validateContext();
	}

	private void validateContext() {
		TaskExplorer taskExplorer = this.applicationContext.getBean(TaskExplorer.class);

		Page<TaskExecution> page = taskExplorer.findTaskExecutionsByName("application", PageRequest.of(0, 1));

		Set<Long> jobExecutionIds = taskExplorer
				.getJobExecutionIdsByTaskExecutionId(page.iterator().next().getExecutionId());

		assertThat(jobExecutionIds.size()).isEqualTo(1);
		assertThat(taskExplorer.getTaskExecution(jobExecutionIds.iterator().next()).getExecutionId()).isEqualTo(1);

	}

	@Configuration
	@EnableBatchProcessing
	@EnableTask
	public static class JobConfiguration {

		@Autowired
		private JobBuilderFactory jobBuilderFactory;

		@Autowired
		private StepBuilderFactory stepBuilderFactory;

		@Bean
		public Job job() {
			return jobBuilderFactory.get("job")
					.start(stepBuilderFactory.get("step1").tasklet(new Tasklet() {
						@Override
						public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
								throws Exception {
							System.out.println("Executed");
							return RepeatStatus.FINISHED;
						}
					}).build())
					.build();
		}
	}

	@Configuration
	@EnableBatchProcessing
	@EnableTask
	public static class JobWithFailureConfiguration {

		@Autowired
		private JobBuilderFactory jobBuilderFactory;

		@Autowired
		private StepBuilderFactory stepBuilderFactory;

		@Bean
		public Job jobFail() {
			return jobBuilderFactory.get("jobA")
					.start(stepBuilderFactory.get("step1").tasklet(new Tasklet() {
						@Override
						public RepeatStatus execute(StepContribution contribution,
								ChunkContext chunkContext)
								throws Exception {
							System.out.println("Executed");
								throw new IllegalStateException("WHOOPS");
						}
					}).build())
					.build();
		}

		@Bean
		public Job jobFun() {
			return jobBuilderFactory.get("jobSucceed")
					.start(stepBuilderFactory.get("step1Succeed").tasklet(new Tasklet() {
						@Override
						public RepeatStatus execute(StepContribution contribution,
								ChunkContext chunkContext)
								throws Exception {
							System.out.println("Executed");
							return RepeatStatus.FINISHED;
						}
					}).build())
					.build();
		}
	}
}
