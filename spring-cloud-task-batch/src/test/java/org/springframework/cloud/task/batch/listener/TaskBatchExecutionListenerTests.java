/*
 * Copyright 2016-present the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.batch.configuration.TaskBatchAutoConfiguration;
import org.springframework.cloud.task.batch.configuration.TaskBatchExecutionListenerBeanPostProcessor;
import org.springframework.cloud.task.batch.configuration.TaskBatchTest;
import org.springframework.cloud.task.configuration.DefaultTaskConfigurer;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.cloud.task.configuration.SingleTaskConfiguration;
import org.springframework.cloud.task.configuration.TaskConfigurer;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Michael Minella
 * @author Glenn Renfro
 */
public class TaskBatchExecutionListenerTests {

	private static final String[] ARGS = new String[] {};

	private ConfigurableApplicationContext applicationContext;

	@AfterEach
	public void tearDown() {
		if (this.applicationContext != null && this.applicationContext.isActive()) {
			this.applicationContext.close();
		}
	}

	@Test
	public void testAutobuiltDataSource() {
		this.applicationContext = SpringApplication.run(JobConfiguration.class, ARGS);
		validateContext();
	}

	@Test
	public void testNoAutoConfigurationEnabled() {
		this.applicationContext = SpringApplication.run(JobConfiguration.class,
				"--spring.cloud.task.batch.listener.enabled=false");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> {
			validateContext();
		});
	}

	@Test
	public void testNoAutoConfigurationEnable() {
		this.applicationContext = SpringApplication.run(JobConfiguration.class,
				"--spring.cloud.task.batch.listener.enable=false");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> {
			validateContext();
		});
	}

	@Test
	public void testNoAutoConfigurationBothDisabled() {
		this.applicationContext = SpringApplication.run(JobConfiguration.class,
				"--spring.cloud.task.batch.listener.enable=false --spring.cloud.task.batch.listener.enabled=false");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> {
			validateContext();
		});
	}

	@Test
	public void testAutoConfigurationEnable() {
		this.applicationContext = SpringApplication.run(JobConfiguration.class,
				"--spring.cloud.task.batch.listener.enable=true");
		validateContext();
	}

	@Test
	public void testAutoConfigurationEnabled() {
		this.applicationContext = SpringApplication.run(JobConfiguration.class,
				"--spring.cloud.task.batch.listener.enabled=true");
		validateContext();
	}

	@Test
	public void testFactoryBean() {
		this.applicationContext = SpringApplication.run(JobFactoryBeanConfiguration.class, ARGS);
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

	@Test
	public void testNoListenerIfTaskNotEnabled() {
		this.applicationContext = SpringApplication.run(TaskNotEnabledConfiguration.class, ARGS);
		assertThat(applicationContext.getBean(Job.class)).isNotNull();
		assertThatThrownBy(() -> applicationContext.getBean(TaskBatchExecutionListenerBeanPostProcessor.class))
			.isInstanceOf(NoSuchBeanDefinitionException.class);
		assertThatThrownBy(() -> applicationContext.getBean(TaskBatchExecutionListener.class))
			.isInstanceOf(NoSuchBeanDefinitionException.class);
	}

	@Test
	public void testMultipleDataSources() {
		this.applicationContext = SpringApplication.run(JobConfigurationMultipleDataSources.class, ARGS);

		TaskExplorer taskExplorer = this.applicationContext.getBean(TaskExplorer.class);

		Page<TaskExecution> page = taskExplorer.findTaskExecutionsByName("application", PageRequest.of(0, 1));

		Set<Long> jobExecutionIds = taskExplorer
			.getJobExecutionIdsByTaskExecutionId(page.iterator().next().getExecutionId());

		assertThat(jobExecutionIds.size()).isEqualTo(1);
		assertThat(taskExplorer.getTaskExecution(jobExecutionIds.iterator().next()).getExecutionId()).isEqualTo(1);
	}

	@Test
	public void testAutobuiltDataSourceNoJob() {
		this.applicationContext = SpringApplication.run(NoJobConfiguration.class, ARGS);

		TaskExplorer taskExplorer = this.applicationContext.getBean(TaskExplorer.class);

		Page<TaskExecution> page = taskExplorer.findTaskExecutionsByName("application", PageRequest.of(0, 1));

		Set<Long> jobExecutionIds = taskExplorer
			.getJobExecutionIdsByTaskExecutionId(page.iterator().next().getExecutionId());

		assertThat(jobExecutionIds.size()).isEqualTo(0);
	}

	@Test
	public void testMapBased() {
		this.applicationContext = SpringApplication.run(JobConfiguration.class, ARGS);

		TaskExplorer taskExplorer = this.applicationContext.getBean(TaskExplorer.class);

		Page<TaskExecution> page = taskExplorer.findTaskExecutionsByName("application", PageRequest.of(0, 1));

		Set<Long> jobExecutionIds = taskExplorer
			.getJobExecutionIdsByTaskExecutionId(page.iterator().next().getExecutionId());

		assertThat(jobExecutionIds.size()).isEqualTo(1);
		assertThat((long) taskExplorer.getTaskExecutionIdByJobExecutionId(jobExecutionIds.iterator().next()))
			.isEqualTo(1);
	}

	@Test
	public void testMultipleJobs() {
		this.applicationContext = SpringApplication.run(MultipleJobConfiguration.class, "--spring.batch.job.name=job1");

		TaskExplorer taskExplorer = this.applicationContext.getBean(TaskExplorer.class);

		Page<TaskExecution> page = taskExplorer.findTaskExecutionsByName("application", PageRequest.of(0, 1));

		Set<Long> jobExecutionIds = taskExplorer
			.getJobExecutionIdsByTaskExecutionId(page.iterator().next().getExecutionId());

		assertThat(jobExecutionIds.size()).isEqualTo(1);
		Iterator<Long> jobExecutionIdsIterator = jobExecutionIds.iterator();
		assertThat((long) taskExplorer.getTaskExecutionIdByJobExecutionId(jobExecutionIdsIterator.next())).isEqualTo(1);

	}

	@Test
	public void testBatchExecutionListenerBeanPostProcessorWithJobNames() {
		List<String> jobNames = new ArrayList<>(3);
		jobNames.add("job1");
		jobNames.add("job2");
		jobNames.add("TESTOBJECT");

		TaskBatchExecutionListenerBeanPostProcessor beanPostProcessor = beanPostProcessor(jobNames);

		SimpleJob testObject = new SimpleJob();
		SimpleJob bean = (SimpleJob) beanPostProcessor.postProcessBeforeInitialization(testObject, "TESTOBJECT");
		assertThat(bean).isEqualTo(testObject);
	}

	@Test
	public void testBatchExecutionListenerBeanPostProcessorWithEmptyJobNames() {
		TaskBatchExecutionListenerBeanPostProcessor beanPostProcessor = beanPostProcessor(Collections.emptyList());

		SimpleJob testObject = new SimpleJob();
		SimpleJob bean = (SimpleJob) beanPostProcessor.postProcessBeforeInitialization(testObject, "TESTOBJECT");
		assertThat(bean).isEqualTo(testObject);
	}

	@Test
	public void testBatchExecutionListenerBeanPostProcessorNullJobNames() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			beanPostProcessor(null);
		});
	}

	private TaskBatchExecutionListenerBeanPostProcessor beanPostProcessor(List<String> jobNames) {
		this.applicationContext = SpringApplication.run(new Class[] { JobConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, EmbeddedDataSourceConfiguration.class,
				BatchAutoConfiguration.class, TaskBatchAutoConfiguration.class, SimpleTaskAutoConfiguration.class,
				SingleTaskConfiguration.class }, ARGS);

		TaskBatchExecutionListenerBeanPostProcessor beanPostProcessor = this.applicationContext
			.getBean(TaskBatchExecutionListenerBeanPostProcessor.class);

		beanPostProcessor.setJobNames(jobNames);
		return beanPostProcessor;
	}

	@EnableBatchProcessing
	@TaskBatchTest
	@Import(EmbeddedDataSourceConfiguration.class)
	@EnableTask
	public static class NoJobConfiguration {

		@Bean
		PlatformTransactionManager transactionManager() {
			return new ResourcelessTransactionManager();
		}

	}

	@TaskBatchTest
	@EnableTask
	@Import(EmbeddedDataSourceConfiguration.class)
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
		PlatformTransactionManager transactionManager() {
			return new ResourcelessTransactionManager();
		}

	}

	@EnableBatchProcessing
	@TaskBatchTest
	@Import(EmbeddedDataSourceConfiguration.class)
	public static class TaskNotEnabledConfiguration {

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
		PlatformTransactionManager transactionManager() {
			return new ResourcelessTransactionManager();
		}

	}

	@TaskBatchTest
	@EnableTask
	@Import(EmbeddedDataSourceConfiguration.class)
	public static class JobFactoryBeanConfiguration {

		@Bean
		public FactoryBean<Job> job(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new FactoryBean<Job>() {
				@Override
				public Job getObject() {
					return new JobBuilder("job", jobRepository)
						.start(new StepBuilder("step1", jobRepository).tasklet((contribution, chunkContext) -> {
							System.out.println("Executed");
							return RepeatStatus.FINISHED;
						}, transactionManager).build())
						.build();
				}

				@Override
				public Class<?> getObjectType() {
					return Job.class;
				}

				@Override
				public boolean isSingleton() {
					return true;
				}
			};
		}

		@Bean
		PlatformTransactionManager transactionManager() {
			return new ResourcelessTransactionManager();
		}

	}

	@TaskBatchTest
	@EnableTask
	@Import(EmbeddedDataSourceConfiguration.class)
	public static class JobConfigurationMultipleDataSources {

		@Bean
		public Job job(JobRepository jobRepository) {
			return new JobBuilder("job", jobRepository)
				.start(new StepBuilder("step1", jobRepository).tasklet(new Tasklet() {
					@Override
					public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
							throws Exception {
						System.out.println("Executed");
						return RepeatStatus.FINISHED;
					}
				}, new ResourcelessTransactionManager()).build())
				.build();
		}

		@Bean
		@Primary
		public DataSource myDataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
				.setName("myDataSource");
			return builder.build();
		}

		@Bean
		public DataSource incorrectDataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
				.setName("incorrectDataSource");
			return builder.build();
		}

		@Bean
		public TaskConfigurer taskConfigurer() {
			return new DefaultTaskConfigurer(myDataSource());
		}

		@Bean
		PlatformTransactionManager transactionManager() {
			return new ResourcelessTransactionManager();
		}

	}

	@TaskBatchTest
	@EnableTask
	@Import(EmbeddedDataSourceConfiguration.class)
	public static class MultipleJobConfiguration {

		@Bean
		public Job job1(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new JobBuilder("job1", jobRepository)
				.start(new StepBuilder("job1step1", jobRepository).tasklet((contribution, chunkContext) -> {
					System.out.println("Executed job1");
					return RepeatStatus.FINISHED;
				}, transactionManager).build())
				.build();
		}

		@Bean
		public Job job2(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new JobBuilder("job2", jobRepository)
				.start(new StepBuilder("job2step1", jobRepository).tasklet((contribution, chunkContext) -> {
					System.out.println("Executed job2");
					return RepeatStatus.FINISHED;
				}, transactionManager).build())
				.build();
		}

		@Bean
		PlatformTransactionManager transactionManager() {
			return new ResourcelessTransactionManager();
		}

	}

}
