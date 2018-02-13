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

package org.springframework.cloud.task.batch.listener;

import java.util.Set;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.cloud.task.batch.configuration.TaskBatchAutoConfiguration;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
public class PrefixTests {

	private ConfigurableApplicationContext applicationContext;

	@After
	public void tearDown() {
		if (this.applicationContext != null) {
			this.applicationContext.close();
		}
	}

	@Test
	public void testPrefix() {
		this.applicationContext = SpringApplication.run(new Class[] {
				JobConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				BatchAutoConfiguration.class,
				TaskBatchAutoConfiguration.class }, new String[] { "--spring.cloud.task.tablePrefix=FOO_" });

		TaskExplorer taskExplorer = this.applicationContext.getBean(TaskExplorer.class);

		Set<Long> jobIds = taskExplorer.getJobExecutionIdsByTaskExecutionId(1);
		assertThat(jobIds.size()).isEqualTo(1);
		assertThat(jobIds.contains(1L));
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
					.start(stepBuilderFactory.get("step1")
							.tasklet((contribution, chunkContext) -> {
						System.out.println("Executed");
						return RepeatStatus.FINISHED;
					}).build())
					.build();
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()
					.addScript("classpath:schema-h2.sql")
					.setType(EmbeddedDatabaseType.H2)
					.build();
		}
	}

}
