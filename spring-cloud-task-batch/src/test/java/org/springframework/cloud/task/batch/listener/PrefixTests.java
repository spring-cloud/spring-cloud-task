/*
 * Copyright 2018-2021 the original author or authors.
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

import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.task.batch.configuration.TaskBatchTest;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Glenn Renfro
 */
public class PrefixTests {

	private ConfigurableApplicationContext applicationContext;

	@AfterEach
	public void tearDown() {
		if (this.applicationContext != null && this.applicationContext.isActive()) {
			this.applicationContext.close();
		}
	}

	@Test
	public void testPrefix() {
		this.applicationContext = SpringApplication.run(JobConfiguration.class, "--spring.cloud.task.tablePrefix=FOO_");

		TaskExplorer taskExplorer = this.applicationContext.getBean(TaskExplorer.class);

		Set<Long> jobIds = taskExplorer.getJobExecutionIdsByTaskExecutionId(1);
		assertThat(jobIds.size()).isEqualTo(1);
		assertThat(jobIds.contains(1L));
	}

	@Configuration(proxyBeanMethods = false)
	@TaskBatchTest
	@EnableTask
	public static class JobConfiguration {

		@Bean
		public Job job(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new JobBuilder("job", jobRepository)
					.start(new StepBuilder("step1", jobRepository).tasklet((contribution, chunkContext) -> {
						System.out.println("Executed");
						return RepeatStatus.FINISHED;
					}, transactionManager).build()).build();
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().addScript("classpath:schema-h2.sql").setType(EmbeddedDatabaseType.H2)
					.build();
		}

		@Bean
		PlatformTransactionManager transactionManager() {
			return new ResourcelessTransactionManager();
		}

	}

}
