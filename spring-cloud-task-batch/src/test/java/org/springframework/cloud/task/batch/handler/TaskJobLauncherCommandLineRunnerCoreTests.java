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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.MapJobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.task.listener.TaskException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TaskJobLauncherCommandLineRunnerCoreTests.BatchConfiguration.class})
public class TaskJobLauncherCommandLineRunnerCoreTests {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private JobExplorer jobExplorer;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private TaskJobLauncherCommandLineRunner runner;

	private JobBuilderFactory jobs;

	private StepBuilderFactory steps;

	private Job job;

	private Step step;

	@Before
	public void init() {
		this.jobs = new JobBuilderFactory(this.jobRepository);
		this.steps = new StepBuilderFactory(this.jobRepository, this.transactionManager);
		Tasklet tasklet = (contribution, chunkContext) -> null;
		this.step = this.steps.get("step").tasklet(tasklet).build();
		this.job = this.jobs.get("job").start(this.step).build();
		this.runner = new TaskJobLauncherCommandLineRunner(this.jobLauncher, this.jobExplorer);
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
		runFailedJob(new JobParameters());
		assertThat(this.jobExplorer.getJobInstances("job", 0, 100)).hasSize(1);
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
	}

	@DirtiesContext
	@Test
	public void retryFailedExecutionWithNonIdentifyingParameters() throws Exception {
		this.job = this.jobs.get("job")
				.start(this.steps.get("step").tasklet(throwingTasklet()).build())
				.incrementer(new RunIdIncrementer()).build();
		JobParameters jobParameters = new JobParametersBuilder().addLong("id", 1L, false)
				.addLong("foo", 2L, false).toJobParameters();
		runFailedJob(new JobParameters());
		runFailedJob(new JobParameters());
		assertThat(this.jobExplorer.getJobInstances("job", 0, 100)).hasSize(1);
	}

	private Tasklet throwingTasklet() {
		return (contribution, chunkContext) -> {
			throw new RuntimeException("Planned");
		};
	}

	private void runFailedJob(JobParameters jobParameters) throws Exception {
		boolean isExceptionThrown = false;
		try {
			this.runner.execute(this.job, new JobParameters());
		}
		catch (TaskException taskException) {
			isExceptionThrown = true;
		}
		assertThat(isExceptionThrown).isTrue();
	}

	@Configuration
	@EnableBatchProcessing
	protected static class BatchConfiguration implements BatchConfigurer {

		private ResourcelessTransactionManager transactionManager =
				new ResourcelessTransactionManager();

		private JobRepository jobRepository;

		private MapJobRepositoryFactoryBean jobRepositoryFactory =
				new MapJobRepositoryFactoryBean(
				this.transactionManager);

		public BatchConfiguration() throws Exception {
			this.jobRepository = this.jobRepositoryFactory.getObject();
		}

		public void clear() {
			this.jobRepositoryFactory.clear();
		}

		@Override
		public JobRepository getJobRepository() {
			return this.jobRepository;
		}

		@Override
		public PlatformTransactionManager getTransactionManager() {
			return this.transactionManager;
		}

		@Override
		public JobLauncher getJobLauncher() {
			SimpleJobLauncher launcher = new SimpleJobLauncher();
			launcher.setJobRepository(this.jobRepository);
			launcher.setTaskExecutor(new SyncTaskExecutor());
			return launcher;
		}

		@Override
		public JobExplorer getJobExplorer() throws Exception {
			return new MapJobExplorerFactoryBean(this.jobRepositoryFactory).getObject();
		}

	}
}
