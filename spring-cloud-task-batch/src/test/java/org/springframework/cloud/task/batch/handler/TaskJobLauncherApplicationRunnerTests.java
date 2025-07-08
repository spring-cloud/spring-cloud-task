/*
 * Copyright 2018-2025 the original author or authors.
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

package org.springframework.cloud.task.batch.handler;

import java.util.Arrays;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration;
import org.springframework.boot.batch.autoconfigure.BatchProperties;
import org.springframework.boot.batch.autoconfigure.JobExecutionEvent;
import org.springframework.boot.batch.autoconfigure.JobLauncherApplicationRunner;
import org.springframework.boot.jdbc.autoconfigure.EmbeddedDataSourceConfiguration;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.cloud.task.batch.configuration.TaskBatchAutoConfiguration;
import org.springframework.cloud.task.batch.configuration.TaskJobLauncherAutoConfiguration;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.cloud.task.configuration.SingleTaskConfiguration;
import org.springframework.cloud.task.listener.TaskException;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Glenn Renfro
 */
public class TaskJobLauncherApplicationRunnerTests {

	private static final String DEFAULT_ERROR_MESSAGE = "Job jobA failed during execution for job instance id 1 with jobExecutionId of 1";

	private ConfigurableApplicationContext applicationContext;

	@AfterEach
	public void tearDown() {
		if (this.applicationContext != null && this.applicationContext.isActive()) {
			this.applicationContext.close();
		}
	}

	@Disabled
	@Test
	public void testTaskJobLauncherCLRSuccessFail() {
		String[] enabledArgs = new String[] { "--spring.cloud.task.batch.failOnJobFailure=true" };
		validateForFail(DEFAULT_ERROR_MESSAGE, TaskJobLauncherApplicationRunnerTests.JobWithFailureConfiguration.class,
				enabledArgs);
	}

	/**
	 * Verifies that the task will return an exit code other than zero if the job fails
	 * with the EnableTask annotation.
	 */
	@Disabled("The task repository is not getting populated.")
	@Test
	public void testTaskJobLauncherCLRSuccessFailWithAnnotation() {
		String[] enabledArgs = new String[] { "--spring.cloud.task.batch.failOnJobFailure=true" };
		validateForFail(DEFAULT_ERROR_MESSAGE,
				TaskJobLauncherApplicationRunnerTests.JobWithFailureAnnotatedConfiguration.class, enabledArgs);
	}

	@Disabled
	@Test
	public void testTaskJobLauncherCLRSuccessFailWithTaskExecutor() {
		String[] enabledArgs = new String[] { "--spring.cloud.task.batch.failOnJobFailure=true",
				"--spring.cloud.task.batch.failOnJobFailurePollInterval=500" };
		validateForFail(DEFAULT_ERROR_MESSAGE,
				TaskJobLauncherApplicationRunnerTests.JobWithFailureTaskExecutorConfiguration.class, enabledArgs);
	}

	@Disabled
	@Test
	public void testNoTaskJobLauncher() {
		String[] enabledArgs = new String[] { "--spring.cloud.task.batch.failOnJobFailure=true",
				"--spring.cloud.task.batch.failOnJobFailurePollInterval=500", "--spring.batch.job.enabled=false" };
		this.applicationContext = SpringApplication
			.run(new Class[] { TaskJobLauncherApplicationRunnerTests.JobWithFailureConfiguration.class }, enabledArgs);
		JobRepository jobRepository = this.applicationContext.getBean(JobRepository.class);
		assertThat(jobRepository.getJobNames().size()).isEqualTo(0);
	}

	@Disabled
	@Test
	public void testTaskJobLauncherPickOneJob() {
		String[] enabledArgs = new String[] { "--spring.cloud.task.batch.fail-on-job-failure=true",
				"--spring.cloud.task.batch.jobNames=jobSucceed" };
		boolean isExceptionThrown = false;
		try {
			this.applicationContext = SpringApplication.run(
					new Class[] { TaskJobLauncherApplicationRunnerTests.JobWithFailureConfiguration.class },
					enabledArgs);
		}
		catch (IllegalStateException exception) {
			isExceptionThrown = true;
		}
		assertThat(isExceptionThrown).isFalse();
		validateContext();
	}

	@Disabled
	@Test
	public void testApplicationRunnerSetToFalse() {
		String[] enabledArgs = new String[] {};
		this.applicationContext = SpringApplication
			.run(new Class[] { TaskJobLauncherApplicationRunnerTests.JobConfiguration.class }, enabledArgs);
		validateContext();
		assertThat(this.applicationContext.getBean(JobLauncherApplicationRunner.class)).isNotNull();

		Executable executable = () -> this.applicationContext.getBean(TaskJobLauncherApplicationRunner.class);

		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(executable::execute)
			.withMessage("No qualifying bean of type "
					+ "'org.springframework.cloud.task.batch.handler.TaskJobLauncherApplicationRunner' available");
		validateContext();
	}

	private void validateContext() {
		TaskExplorer taskExplorer = this.applicationContext.getBean(TaskExplorer.class);

		Page<TaskExecution> page = taskExplorer.findTaskExecutionsByName("application", PageRequest.of(0, 1));

		Set<Long> jobExecutionIds = taskExplorer
			.getJobExecutionIdsByTaskExecutionId(page.iterator().next().getExecutionId());

		assertThat(jobExecutionIds.size()).isEqualTo(1);
		assertThat(taskExplorer.getTaskExecution(jobExecutionIds.iterator().next()).getExecutionId()).isEqualTo(1);

		JobExecutionEventListener listener = this.applicationContext.getBean(JobExecutionEventListener.class);
		assertThat(listener.getEventCounter()).isEqualTo(1);
	}

	private void validateForFail(String errorMessage, Class<?> clazz, String[] enabledArgs) {
		Executable executable = () -> this.applicationContext = SpringApplication
			.run(new Class[] { clazz, PropertyPlaceholderAutoConfiguration.class }, enabledArgs);

		assertThatExceptionOfType(TaskException.class).isThrownBy(executable::execute)
			.withMessageContaining(errorMessage);
	}

	@Component
	private static class JobExecutionEventListener implements ApplicationListener<JobExecutionEvent> {

		private int eventCounter = 0;

		@Override
		public void onApplicationEvent(JobExecutionEvent event) {
			eventCounter++;
		}

		public int getEventCounter() {
			return eventCounter;
		}

	}

	@EnableTask
	@Import({ EmbeddedDataSourceConfiguration.class, JobExecutionEventListener.class })
	@ImportAutoConfiguration({ SimpleTaskAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	public static class JobConfiguration {

		@Bean
		public Job job(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new JobBuilder("job", jobRepository)
				.start(new StepBuilder("step1", jobRepository).tasklet((contribution, chunkContext) -> {
					System.out.println("Executed");
					return RepeatStatus.FINISHED;
				}, transactionManager).build())
				.build();
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new ResourcelessTransactionManager();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(JobExecutionEventListener.class)
	public static class TransactionManagerTestConfiguration {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new ResourcelessTransactionManager();
		}

		@Bean
		public BatchProperties batchProperties() {
			return new BatchProperties();
		}

		@Bean
		DataSourceScriptDatabaseInitializer batchDataSourceInitializer(DataSource dataSource) {
			DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
			settings.setSchemaLocations(Arrays.asList("classpath:org/springframework/batch/core/schema-h2.sql"));
			return new DataSourceScriptDatabaseInitializer(dataSource, settings);
		}

	}

	@EnableBatchProcessing
	@EnableJdbcJobRepository
	@ImportAutoConfiguration({ PropertyPlaceholderAutoConfiguration.class, BatchAutoConfiguration.class,
			TaskBatchAutoConfiguration.class, TaskJobLauncherAutoConfiguration.class, SingleTaskConfiguration.class,
			SimpleTaskAutoConfiguration.class, TransactionManagerTestConfiguration.class })
	@Import(EmbeddedDataSourceConfiguration.class)
	@EnableTask
	public static class JobWithFailureConfiguration {

		@Autowired
		private JobRepository jobRepository;

		@Autowired
		private PlatformTransactionManager transactionManager;

		@Bean
		public Job jobFail() {
			return new JobBuilder("jobA", this.jobRepository)
				.start(new StepBuilder("step1", this.jobRepository).tasklet((contribution, chunkContext) -> {
					System.out.println("Executed");
					throw new IllegalStateException("WHOOPS");
				}, transactionManager).build())
				.build();
		}

		@Bean
		public Job jobFun() {
			return new JobBuilder("jobSucceed", this.jobRepository)
				.start(new StepBuilder("step1Succeed", this.jobRepository).tasklet((contribution, chunkContext) -> {
					System.out.println("Executed");
					return RepeatStatus.FINISHED;
				}, transactionManager).build())
				.build();
		}

	}

	@EnableTask
	public static class JobWithFailureAnnotatedConfiguration extends JobWithFailureConfiguration {

	}

	@Import(JobWithFailureConfiguration.class)
	@Configuration
	@EnableTask
	public static class JobWithFailureTaskExecutorConfiguration {

	}

}
