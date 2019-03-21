/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.cloud.task.batch.listener;

import java.util.Iterator;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.batch.configuration.TaskBatchAutoConfiguration;
import org.springframework.cloud.task.batch.configuration.TaskBatchExecutionListenerFactoryBean;
import org.springframework.cloud.task.configuration.DefaultTaskConfigurer;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.configuration.TaskConfigurer;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.support.TaskRepositoryInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.junit.Assert.assertEquals;

/**
 * @author Michael Minella
 */
public class TaskBatchExecutionListenerTests {

	public static final String[] ARGS = new String[] {"--spring.cloud.task.closecontext.enable=false"};

	private ConfigurableApplicationContext applicationContext;

	@After
	public void tearDown() {
		if(this.applicationContext != null) {
			this.applicationContext.close();
		}
	}

	@Test
	public void testAutobuiltDataSource() {
		this.applicationContext = SpringApplication.run(new Object[] {JobConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				BatchAutoConfiguration.class,
				TaskBatchAutoConfiguration.class}, ARGS);

		TaskExplorer taskExplorer = this.applicationContext.getBean(TaskExplorer.class);

		Page<TaskExecution> page = taskExplorer.findTaskExecutionsByName("application", new PageRequest(0, 1));

		Set<Long> jobExecutionIds = taskExplorer.getJobExecutionIdsByTaskExecutionId(page.iterator().next().getExecutionId());

		assertEquals(1, jobExecutionIds.size());
		assertEquals(1, taskExplorer.getTaskExecution(jobExecutionIds.iterator().next()).getExecutionId());
	}

	@Test
	public void testMultipleDataSources() {
		this.applicationContext = SpringApplication.run(new Object[] {JobConfigurationMultipleDataSources.class,
				PropertyPlaceholderAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				BatchAutoConfiguration.class,
				TaskBatchAutoConfiguration.class}, ARGS);

		TaskExplorer taskExplorer = this.applicationContext.getBean(TaskExplorer.class);

		Page<TaskExecution> page = taskExplorer.findTaskExecutionsByName("application", new PageRequest(0, 1));

		Set<Long> jobExecutionIds = taskExplorer.getJobExecutionIdsByTaskExecutionId(page.iterator().next().getExecutionId());

		assertEquals(1, jobExecutionIds.size());
		assertEquals(1, taskExplorer.getTaskExecution(jobExecutionIds.iterator().next()).getExecutionId());
	}

	@Test
	public void testAutobuiltDataSourceNoJob() {
		this.applicationContext = SpringApplication.run(new Object[] {NoJobConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				BatchAutoConfiguration.class,
				TaskBatchAutoConfiguration.class}, ARGS);

		TaskExplorer taskExplorer = this.applicationContext.getBean(TaskExplorer.class);

		Page<TaskExecution> page = taskExplorer.findTaskExecutionsByName("application", new PageRequest(0, 1));

		Set<Long> jobExecutionIds = taskExplorer.getJobExecutionIdsByTaskExecutionId(page.iterator().next().getExecutionId());

		assertEquals(0, jobExecutionIds.size());
	}

	@Test
	public void testMapBased() {
		this.applicationContext = SpringApplication.run(new Object[] {JobConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				BatchAutoConfiguration.class,
				TaskBatchAutoConfiguration.class}, ARGS);

		TaskExplorer taskExplorer = this.applicationContext.getBean(TaskExplorer.class);

		Page<TaskExecution> page = taskExplorer.findTaskExecutionsByName("application", new PageRequest(0, 1));

		Set<Long> jobExecutionIds = taskExplorer.getJobExecutionIdsByTaskExecutionId(page.iterator().next().getExecutionId());

		assertEquals(1, jobExecutionIds.size());
		assertEquals(0, (long) taskExplorer.getTaskExecutionIdByJobExecutionId(jobExecutionIds.iterator().next()));
	}

	@Test
	public void testMultipleJobs() {
		this.applicationContext = SpringApplication.run(new Object[] {MultipleJobConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				BatchAutoConfiguration.class,
				TaskBatchAutoConfiguration.class}, ARGS);

		TaskExplorer taskExplorer = this.applicationContext.getBean(TaskExplorer.class);

		Page<TaskExecution> page = taskExplorer.findTaskExecutionsByName("application", new PageRequest(0, 1));

		Set<Long> jobExecutionIds = taskExplorer.getJobExecutionIdsByTaskExecutionId(page.iterator().next().getExecutionId());

		assertEquals(2, jobExecutionIds.size());
		Iterator<Long> jobExecutionIdsIterator = jobExecutionIds.iterator();
		assertEquals(0, (long) taskExplorer.getTaskExecutionIdByJobExecutionId(jobExecutionIdsIterator.next()));
		assertEquals(0, (long) taskExplorer.getTaskExecutionIdByJobExecutionId(jobExecutionIdsIterator.next()));
	}

	@Configuration
	@EnableBatchProcessing
	@EnableTask
	public static class NoJobConfiguration {

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
						public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
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
	public static class JobConfigurationMultipleDataSources {

		@Autowired
		private JobBuilderFactory jobBuilderFactory;

		@Autowired
		private StepBuilderFactory stepBuilderFactory;

		@Bean
		public Job job() {
			return jobBuilderFactory.get("job")
					.start(stepBuilderFactory.get("step1").tasklet(new Tasklet() {
						@Override
						public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
							System.out.println("Executed");
							return RepeatStatus.FINISHED;
						}
					}).build())
					.build();
		}

		@Bean
		@Primary
		public DataSource myDataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.H2)
					.setName("myDataSource");
			return builder.build();
		}

		@Bean
		public DataSource incorrectDataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.H2)
					.setName("incorrectDataSource");
			return builder.build();
		}

		@Bean
		public TaskBatchExecutionListenerFactoryBean taskBatchExecutionListener(TaskExplorer taskExplorer) {
			return new TaskBatchExecutionListenerFactoryBean(myDataSource(), taskExplorer);
		}

		@Bean
		public TaskConfigurer taskConfigurer() {
			return new DefaultTaskConfigurer(myDataSource());
		}

		@Bean
		public TaskRepositoryInitializer taskRepositoryInitializer() {
			TaskRepositoryInitializer taskRepositoryInitializer = new TaskRepositoryInitializer();

			taskRepositoryInitializer.setDataSource(myDataSource());

			return taskRepositoryInitializer;
		}

		@Bean
		public DefaultBatchConfigurer batchConfigurer() {
			return new DefaultBatchConfigurer(myDataSource());
		}
	}

	@Configuration
	@EnableBatchProcessing
	@EnableTask
	public static class MultipleJobConfiguration {

		@Autowired
		private JobBuilderFactory jobBuilderFactory;

		@Autowired
		private StepBuilderFactory stepBuilderFactory;

		@Bean
		public Job job1() {
			return jobBuilderFactory.get("job1")
					.start(stepBuilderFactory.get("job1step1").tasklet(new Tasklet() {
						@Override
						public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
							System.out.println("Executed job1");
							return RepeatStatus.FINISHED;
						}
					}).build())
					.build();
		}

		@Bean
		public Job job2() {
			return jobBuilderFactory.get("job2")
					.start(stepBuilderFactory.get("job2step1").tasklet(new Tasklet() {
						@Override
						public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
							System.out.println("Executed job2");
							return RepeatStatus.FINISHED;
						}
					}).build())
					.build();
		}
	}
}
