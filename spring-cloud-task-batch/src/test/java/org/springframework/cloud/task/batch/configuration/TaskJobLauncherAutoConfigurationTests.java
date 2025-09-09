/*
 * Copyright 2018-present the original author or authors.
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

package org.springframework.cloud.task.batch.configuration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration;
import org.springframework.boot.batch.autoconfigure.JobLauncherApplicationRunner;
import org.springframework.boot.jdbc.autoconfigure.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.batch.autoconfigure.BatchProperties;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.task.batch.handler.TaskJobLauncherApplicationRunner;
import org.springframework.cloud.task.batch.listener.TaskBatchExecutionListenerTests;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
public class TaskJobLauncherAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(BatchAutoConfiguration.class, TaskJobLauncherAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, EmbeddedDataSourceConfiguration.class,
				SimpleTaskAutoConfiguration.class))
		.withUserConfiguration(TestJobConfiguration.class);

	@Test
	public void testAutoBuiltDataSourceWithTaskJobLauncherCLR() {
		this.contextRunner.withPropertyValues("spring.cloud.task.batch.fail-on-job-failure=true").run(context -> {
			assertThat(context).hasSingleBean(TaskJobLauncherApplicationRunner.class);
			assertThat(context.getBean(TaskJobLauncherApplicationRunner.class).getOrder()).isEqualTo(0);
		});
	}

	@Test
	public void testAutoBuiltDataSourceWithTaskJobLauncherCLROrder() {
		this.contextRunner
			.withPropertyValues("spring.cloud.task.batch.fail-on-job-failure=true",
					"spring.cloud.task.batch.applicationRunnerOrder=100")
			.run(context -> {
				assertThat(context.getBean(TaskJobLauncherApplicationRunner.class).getOrder()).isEqualTo(100);
			});
	}

	@Test
	public void testAutoBuiltDataSourceWithBatchJobNames() {
		this.contextRunner
			.withPropertyValues("spring.cloud.task.batch.fail-on-job-failure=true", "spring.batch.job.name=job1",
					"spring.cloud.task.batch.jobName=foobar")
			.run(context -> {
				validateJobNames(context, "job1");
			});
	}

	@Test
	public void testAutoBuiltDataSourceWithTaskBatchJobNames() {
		this.contextRunner
			.withPropertyValues("spring.cloud.task.batch.fail-on-job-failure=true",
					"spring.cloud.task.batch.jobNames=job1,job2")
			.run(context -> {
				validateJobNames(context, "job1,job2");
			});
	}

	private void validateJobNames(AssertableApplicationContext context, String jobNames) throws Exception {
		JobLauncherApplicationRunner jobLauncherApplicationRunner = context
			.getBean(TaskJobLauncherApplicationRunner.class);

		Object names = ReflectionTestUtils.getField(jobLauncherApplicationRunner, "jobName");
		assertThat(names).isEqualTo(jobNames);
	}

	@Test
	public void testAutoBuiltDataSourceWithTaskJobLauncherCLRDisabled() {
		this.contextRunner.run(context -> {
			// assertThat(context).hasSingleBean(JobLauncherApplicationRunner.class);
			assertThat(context).doesNotHaveBean(TaskJobLauncherApplicationRunner.class);
		});
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableBatchProcessing
	static class TestJobConfiguration {

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

		@Bean
		JobRegistry jobRegistry() {
			return new MapJobRegistry();
		}

		@Bean
		BatchProperties batchProperties() {
			return new BatchProperties();
		}

	}

}
