/*
 * Copyright 2018-2022 the original author or authors.
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

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.repository.dao.DefaultExecutionContextSerializer;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.batch.configuration.TaskBatchProperties;
import org.springframework.cloud.task.listener.TaskException;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Glenn Renfro
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
		classes = { TaskJobLauncherApplicationRunnerCoreTests.BatchConfiguration.class })
public class TaskJobLauncherApplicationRunnerCoreTests {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private JobExplorer jobExplorer;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private TaskJobLauncherApplicationRunner runner;

	private JobBuilderFactory jobs;

	private StepBuilderFactory steps;

	private Job job;

	private Step step;

	@BeforeEach
	public void init() {
		this.jobs = new JobBuilderFactory(this.jobRepository);
		this.steps = new StepBuilderFactory(this.jobRepository, this.transactionManager);
		Tasklet tasklet = (contribution, chunkContext) -> RepeatStatus.FINISHED;
		this.step = this.steps.get("step").tasklet(tasklet).build();
		this.job = this.jobs.get("job").start(this.step).build();
		this.runner = new TaskJobLauncherApplicationRunner(this.jobLauncher,
				this.jobExplorer, this.jobRepository, new TaskBatchProperties());

	}

	@DirtiesContext
	@Test
	public void basicExecution() throws Exception {
		this.runner.execute(this.job, new JobParameters());
		assertThat(this.jobExplorer.getJobInstances("job", 0, 100)).hasSize(1);
		this.runner.execute(this.job,
				new JobParametersBuilder().addLong("id", 1L).toJobParameters());
		assertThat(this.jobExplorer.getJobInstances("job", 0, 100)).hasSize(2);
	}

	@DirtiesContext
	@Test
	public void incrementExistingExecution() throws Exception {
		this.job = this.jobs.get("job").start(this.step)
				.incrementer(new RunIdIncrementer()).build();
		this.runner.execute(this.job, new JobParameters());
		this.runner.execute(this.job, new JobParameters());
		assertThat(this.jobExplorer.getJobInstances("job", 0, 100)).hasSize(2);
	}

	@DirtiesContext
	@Test
	public void retryFailedExecution() throws Exception {
		this.job = this.jobs.get("job")
				.start(this.steps.get("step").tasklet(throwingTasklet()).build())
				.incrementer(new RunIdIncrementer()).build();
		runFailedJob(new JobParameters());
		runFailedJob(new JobParametersBuilder().addLong("run.id", 1L).toJobParameters());
		assertThat(this.jobExplorer.getJobInstances("job", 0, 100)).hasSize(1);
	}

	@DirtiesContext
	@Test
	public void runDifferentInstances() throws Exception {
		this.job = this.jobs.get("job")
				.start(this.steps.get("step").tasklet(throwingTasklet()).build()).build();
		// start a job instance
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo")
				.toJobParameters();
		runFailedJob(jobParameters);
		assertThat(this.jobExplorer.getJobInstances("job", 0, 100)).hasSize(1);
		// start a different job instance
		JobParameters otherJobParameters = new JobParametersBuilder()
				.addString("name", "bar").toJobParameters();
		runFailedJob(otherJobParameters);
		assertThat(this.jobExplorer.getJobInstances("job", 0, 100)).hasSize(2);
	}

	@DirtiesContext
	@Test
	public void retryFailedExecutionOnNonRestartableJob() throws Exception {
		this.job = this.jobs.get("job").preventRestart()
				.start(this.steps.get("step").tasklet(throwingTasklet()).build())
				.incrementer(new RunIdIncrementer()).build();
		runFailedJob(new JobParameters());
		runFailedJob(new JobParameters());
		// A failed job that is not restartable does not re-use the job params of
		// the last execution, but creates a new job instance when running it again.
		assertThat(this.jobExplorer.getJobInstances("job", 0, 100)).hasSize(2);

		// try to re-run a failed execution
		Executable executable = () -> this.runner.execute(this.job,
				new JobParametersBuilder().addLong("run.id", 1L).toJobParameters());
		assertThatExceptionOfType(JobRestartException.class)
				.isThrownBy(executable::execute)
				.withMessage("JobInstance already exists and is not restartable");
	}

	@DirtiesContext
	@Test
	public void retryFailedExecutionWithNonIdentifyingParameters() throws Exception {
		this.job = this.jobs.get("job")
				.start(this.steps.get("step").tasklet(throwingTasklet()).build())
				.incrementer(new RunIdIncrementer()).build();
		JobParameters jobParameters = new JobParametersBuilder().addLong("id", 1L, false)
				.addLong("foo", 2L, false).toJobParameters();
		runFailedJob(jobParameters);
		assertThat(this.jobExplorer.getJobInstances("job", 0, 100)).hasSize(1);
		runFailedJob(new JobParametersBuilder(jobParameters).addLong("run.id", 1L)
				.toJobParameters());
		assertThat(this.jobExplorer.getJobInstances("job", 0, 100)).hasSize(1);
	}

	@DirtiesContext
	@Test
	public void retryFailedExecutionWithDifferentNonIdentifyingParametersFromPreviousExecution()
			throws Exception {
		this.job = this.jobs.get("job")
				.start(this.steps.get("step").tasklet(throwingTasklet()).build())
				.incrementer(new RunIdIncrementer()).build();
		JobParameters jobParameters = new JobParametersBuilder().addLong("id", 1L, false)
				.addLong("foo", 2L, false).toJobParameters();
		runFailedJob(jobParameters);
		assertThat(this.jobExplorer.getJobInstances("job", 0, 100)).hasSize(1);
		// try to re-run a failed execution with non identifying parameters
		runFailedJob(new JobParametersBuilder().addLong("run.id", 1L)
				.addLong("id", 2L, false).addLong("foo", 3L, false).toJobParameters());
		assertThat(this.jobExplorer.getJobInstances("job", 0, 100)).hasSize(1);
		JobInstance jobInstance = jobExplorer.getLastJobInstance("job");

		List<JobExecution> executions = this.jobExplorer.getJobExecutions(jobInstance);

		assertThat(executions).hasSize(2);

		JobExecution firstJobExecution  = executions.get(0);
		JobExecution secondJobExecution = executions.get(1);
		if ((executions.get(0).getId() > executions.get(1).getId())) {
			firstJobExecution = executions.get(1);
			secondJobExecution = executions.get(0);
		}

		// first execution
		JobParameters parameters = firstJobExecution.getJobParameters();
		assertThat(parameters.getLong("run.id")).isEqualTo(1L);
		assertThat(parameters.getLong("id")).isEqualTo(2L);
		assertThat(parameters.getLong("foo")).isEqualTo(3L);
		// second execution
		parameters = secondJobExecution.getJobParameters();
		// identifying parameters should be the same as previous execution
		assertThat(parameters.getLong("run.id")).isEqualTo(1L);
		// non-identifying parameters should be the newly specified ones
		assertThat(parameters.getLong("id")).isEqualTo(2L);
		assertThat(parameters.getLong("foo")).isEqualTo(3L);
	}

	private Tasklet throwingTasklet() {
		return (contribution, chunkContext) -> {
			throw new RuntimeException("Planned");
		};
	}

	private void runFailedJob(JobParameters jobParameters) throws Exception {
		boolean isExceptionThrown = false;
		try {
			this.runner.execute(this.job, jobParameters);
		}
		catch (TaskException taskException) {
			isExceptionThrown = true;
		}
		assertThat(isExceptionThrown).isTrue();
	}

	@Configuration
	@EnableBatchProcessing
	@Import(EmbeddedDataSourceConfiguration.class)

	protected static class BatchConfiguration implements BatchConfigurer {

		private ResourcelessTransactionManager transactionManager = new ResourcelessTransactionManager();

		private JobRepository jobRepository;

		@Autowired
		private  DataSource dataSource;

		public BatchConfiguration() throws Exception {
		}

		private JobRepository jobRepositoryNew() throws Exception {
			JobRepositoryFactoryBean jobRepositoryFactoryBean = new JobRepositoryFactoryBean();
			jobRepositoryFactoryBean.setDataSource(dataSource);
			jobRepositoryFactoryBean.setTransactionManager(transactionManager);
			jobRepositoryFactoryBean.setSerializer(new DefaultExecutionContextSerializer());
			this.jobRepository = jobRepositoryFactoryBean.getObject();
			return this.jobRepository;
		}

		@Override
		public JobRepository getJobRepository() throws Exception {
			if (this.jobRepository == null) {
				this.jobRepository = jobRepositoryNew();
			}
			return this.jobRepository;
		}

		@Override
		public PlatformTransactionManager getTransactionManager() {
			return this.transactionManager;
		}

		@Override
		public JobLauncher getJobLauncher() throws Exception {
			SimpleJobLauncher launcher = new SimpleJobLauncher();

			launcher.setJobRepository(getJobRepository());
			launcher.setTaskExecutor(new SyncTaskExecutor());
			return launcher;
		}

		@Override
		public JobExplorer getJobExplorer() throws Exception {
			dataSourceInitializer().afterPropertiesSet();
			JobExplorerFactoryBean factoryBean = new JobExplorerFactoryBean();
			factoryBean.setDataSource(dataSource);
			factoryBean.setJdbcOperations(new JdbcTemplate(dataSource));
			factoryBean.setSerializer(new DefaultExecutionContextSerializer());
			return factoryBean.getObject();
		}

		public DataSourceInitializer dataSourceInitializer() {
			DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
			dataSourceInitializer.setDataSource(dataSource);
			ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
			databasePopulator.addScript(new ClassPathResource("org/springframework/batch/core/schema-h2.sql"));
			dataSourceInitializer.setDatabasePopulator(databasePopulator);
			databasePopulator = new ResourceDatabasePopulator();
			databasePopulator.addScript(new ClassPathResource("org/springframework/batch/core/schema-drop-h2.sql"));
			dataSourceInitializer.setDatabaseCleaner(databasePopulator);
			return dataSourceInitializer;
	}

	}

}
