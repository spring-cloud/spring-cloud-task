/*
 * Copyright 2022-2022 the original author or authors.
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
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.task.batch.configuration.TaskBatchTest;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Henning Pöttker
 */
class PrimaryKeyTests {

	private ConfigurableApplicationContext applicationContext;

	@AfterEach
	void tearDown() {
		if (this.applicationContext != null && this.applicationContext.isActive()) {
			this.applicationContext.close();
		}
	}

	@Test
	void testSchemaWithPrimaryKeys() {
		this.applicationContext = SpringApplication.run(JobConfiguration.class);

		TaskExplorer taskExplorer = this.applicationContext.getBean(TaskExplorer.class);

		Set<Long> jobIds = taskExplorer.getJobExecutionIdsByTaskExecutionId(1);
		assertThat(jobIds).containsExactly(1L);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableBatchProcessing
	@TaskBatchTest
	@EnableTask
	static class JobConfiguration {

		@Bean
		Job job(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
			return jobBuilderFactory.get("job")
					.start(stepBuilderFactory.get("step1").tasklet((contribution, chunkContext) -> {
						System.out.println("Executed");
						return RepeatStatus.FINISHED;
					}).build()).build();
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().addScript("classpath:schema-with-primary-keys-h2.sql")
					.setType(EmbeddedDatabaseType.H2).build();
		}

	}

}
