/*
 * Copyright 2016-2022 the original author or authors.
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

package io.spring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.batch.partition.DeployerPartitionHandler;
import org.springframework.cloud.task.batch.partition.DeployerStepExecutionHandler;
import org.springframework.cloud.task.batch.partition.PassThroughCommandLineArgsProvider;
import org.springframework.cloud.task.batch.partition.SimpleEnvironmentVariablesProvider;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Michael Minella
 */
@Configuration(proxyBeanMethods = false)
public class JobConfiguration {

	private static final int GRID_SIZE = 4;

	// @checkstyle:off
	@Autowired
	public JobRepository jobRepository;

	@Autowired
	public DataSource dataSource;

	// @checkstyle:on
	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired
	private DelegatingResourceLoader resourceLoader;

	@Autowired
	private Environment environment;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Bean
	public PartitionHandler partitionHandler(TaskLauncher taskLauncher, JobExplorer jobExplorer,
			TaskRepository taskRepository, @Autowired(required = false) ThreadPoolTaskExecutor executor)
			throws Exception {
		Resource resource = this.resourceLoader
			.getResource("maven://io.spring.cloud:partitioned-batch-job:3.0.0-SNAPSHOT");

		DeployerPartitionHandler partitionHandler = new DeployerPartitionHandler(taskLauncher, jobExplorer, resource,
				"workerStep", taskRepository, executor);

		List<String> commandLineArgs = new ArrayList<>(3);
		commandLineArgs.add("--spring.profiles.active=worker");
		commandLineArgs.add("--spring.cloud.task.initialize-enabled=false");
		commandLineArgs.add("--spring.batch.initializer.enabled=false");
		partitionHandler.setCommandLineArgsProvider(new PassThroughCommandLineArgsProvider(commandLineArgs));
		partitionHandler.setEnvironmentVariablesProvider(new SimpleEnvironmentVariablesProvider(this.environment));
		partitionHandler.setMaxWorkers(2);
		partitionHandler.setApplicationName("PartitionedBatchJobTask");

		return partitionHandler;
	}

	@ConditionalOnProperty(value = "io.spring.asynchronous", havingValue = "true", matchIfMissing = false)
	@Bean
	public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setThreadNamePrefix("default_task_executor_thread");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.initialize();
		return executor;
	}

	@Bean
	public Partitioner partitioner() {
		return new Partitioner() {
			@Override
			public Map<String, ExecutionContext> partition(int gridSize) {

				Map<String, ExecutionContext> partitions = new HashMap<>(gridSize);

				for (int i = 0; i < GRID_SIZE; i++) {
					ExecutionContext context1 = new ExecutionContext();
					context1.put("partitionNumber", i);

					partitions.put("partition" + i, context1);
				}

				return partitions;
			}
		};
	}

	@Bean
	@Profile("worker")
	public DeployerStepExecutionHandler stepExecutionHandler(JobExplorer jobExplorer) {
		return new DeployerStepExecutionHandler(this.context, jobExplorer, this.jobRepository);
	}

	@Bean
	@StepScope
	public Tasklet workerTasklet(final @Value("#{stepExecutionContext['partitionNumber']}") Integer partitionNumber) {

		return new Tasklet() {
			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				System.out.println("This tasklet ran partition: " + partitionNumber);

				return RepeatStatus.FINISHED;
			}
		};
	}

	@Bean
	public Step step1(PartitionHandler partitionHandler) throws Exception {
		return new StepBuilder("step1").repository(this.jobRepository)
			.partitioner(workerStep().getName(), partitioner())
			.step(workerStep())
			.partitionHandler(partitionHandler)
			.build();
	}

	@Bean
	public Step workerStep() {
		return new StepBuilder("workerStep").repository(this.jobRepository)
			.tasklet(workerTasklet(null))
			.transactionManager(this.transactionManager)
			.build();
	}

	@Bean
	@Profile("!worker")
	public Job partitionedJob(PartitionHandler partitionHandler) throws Exception {
		Random random = new Random();
		return new JobBuilder("partitionedJob" + random.nextInt()).repository(this.jobRepository)
			.start(step1(partitionHandler))
			.build();
	}

}
