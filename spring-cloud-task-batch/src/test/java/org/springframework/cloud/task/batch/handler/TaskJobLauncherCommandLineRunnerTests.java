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

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.function.Executable;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.JobLauncherCommandLineRunner;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.batch.configuration.TaskBatchAutoConfiguration;
import org.springframework.cloud.task.batch.configuration.TaskBatchTest;
import org.springframework.cloud.task.batch.configuration.TaskJobLauncherAutoConfiguration;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.cloud.task.configuration.SingleTaskConfiguration;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Glenn Renfro
 */
public class TaskJobLauncherCommandLineRunnerTests {

	private ConfigurableApplicationContext applicationContext;

	private static final String DEFAULT_ERROR_MESSAGE = "The following Jobs have failed: \n" +
			"Job jobA failed during execution for job instance id 1 with jobExecutionId of 1 \n";

	@After
	public void tearDown() {
		if (this.applicationContext != null && this.applicationContext.isActive()) {
			this.applicationContext.close();
		}
	}

	@Test
	public void testTaskJobLauncherCLRSuccessFail() {
		String[] enabledArgs = new String[] {
				"--spring.cloud.task.batch.failOnJobFailure=true"};
		validateForFail(DEFAULT_ERROR_MESSAGE, TaskJobLauncherCommandLineRunnerTests.JobWithFailureConfiguration.class,
				enabledArgs);
	}

	/**
	 * Verifies that the task will return an exit code other than zero if the
	 * job fails with the deprecated EnableTask annotation.
	 */
	@Test
	public void testTaskJobLauncherCLRSuccessFailWithAnnotation() {
		String[] enabledArgs = new String[] {
				"--spring.cloud.task.batch.failOnJobFailure=true"};
		validateForFail(DEFAULT_ERROR_MESSAGE, TaskJobLauncherCommandLineRunnerTests.JobWithFailureAnnotatedConfiguration.class,
				enabledArgs);
	}

	@Test
	public void testTaskJobLauncherCLRSuccessFailWithTaskExecutor() {
		String[] enabledArgs = new String[] {
				"--spring.cloud.task.batch.failOnJobFailure=true",
				"--spring.cloud.task.batch.failOnJobFailurePollInterval=500"};
		validateForFail(DEFAULT_ERROR_MESSAGE, TaskJobLauncherCommandLineRunnerTests.JobWithFailureTaskExecutorConfiguration.class,
				enabledArgs);
	}

	@Test
	public void testTaskJobLauncherPickOneJob() {
		String[] enabledArgs = new String[] {
				"--spring.cloud.task.batch.fail-on-job-failure=true",
				"--spring.cloud.task.batch.jobNames=jobSucceed"};
		boolean isExceptionThrown = false;
		try {
			this.applicationContext = SpringApplication
					.run(new Class[] { TaskJobLauncherCommandLineRunnerTests.JobWithFailureConfiguration.class }, enabledArgs);
		}
		catch (IllegalStateException exception) {
			isExceptionThrown = true;
		}
		assertThat(isExceptionThrown).isFalse();
		validateContext();
	}

	@Test
	public void testCommandLineRunnerSetToFalse() {
		String[] enabledArgs = new String[] {};
		this.applicationContext = SpringApplication
				.run(new Class[] { TaskJobLauncherCommandLineRunnerTests.JobConfiguration.class }, enabledArgs);
		validateContext();
		assertThat(applicationContext.getBean(JobLauncherCommandLineRunner.class)).isNotNull();

		Executable executable = () -> applicationContext.getBean(TaskJobLauncherCommandLineRunner.class);

		Throwable exception = assertThrows(NoSuchBeanDefinitionException.class, executable);
		assertThat(exception.getMessage()).isEqualTo("No qualifying bean of type " +
				"'org.springframework.cloud.task.batch.handler.TaskJobLauncherCommandLineRunner' available");
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

	private void validateForFail(String errorMessage, Class clazz, String [] enabledArgs) {
		Executable executable = () -> this.applicationContext = SpringApplication
				.run(new Class[] { clazz,PropertyPlaceholderAutoConfiguration.class}, enabledArgs);

		Throwable exception = assertThrows(IllegalStateException.class, executable);
		assertThat(exception.getCause().getMessage()).isEqualTo(errorMessage);
	}


	@EnableBatchProcessing
	@TaskBatchTest
	@Import(EmbeddedDataSourceConfiguration.class)
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
						public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
							System.out.println("Executed");
							return RepeatStatus.FINISHED;
						}
					}).build())
					.build();
		}
	}

	@EnableBatchProcessing
	@ImportAutoConfiguration({
			PropertyPlaceholderAutoConfiguration.class,
			BatchAutoConfiguration.class,
			TaskBatchAutoConfiguration.class,
			TaskJobLauncherAutoConfiguration.class,
			SingleTaskConfiguration.class,
			SimpleTaskAutoConfiguration.class })
	@Import(EmbeddedDataSourceConfiguration.class)
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
								ChunkContext chunkContext) {
							System.out.println("Executed");
							return RepeatStatus.FINISHED;
						}
					}).build())
					.build();
		}
	}

	@EnableTask
	public static class JobWithFailureAnnotatedConfiguration extends JobWithFailureConfiguration{

	}

	public static class JobWithFailureTaskExecutorConfiguration extends JobWithFailureConfiguration{
		@Bean
		public BatchConfigurer batchConfigurer(DataSource dataSource) {
			return new TestBatchConfigurer(dataSource);
		}
	}

	private static class TestBatchConfigurer extends DefaultBatchConfigurer{
		public TestBatchConfigurer(DataSource dataSource) {
			super(dataSource);
		}
		protected JobLauncher createJobLauncher() throws Exception {
			SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
			jobLauncher.setJobRepository(getJobRepository());
			jobLauncher.setTaskExecutor(new ConcurrentTaskExecutor());
			jobLauncher.afterPropertiesSet();
			return jobLauncher;
		}
	}
}
