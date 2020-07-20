/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.task.batch.autoconfigure.amqp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.batch.autoconfigure.SingleStepJobAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.RowMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RabbitItemReaderAutoConfigurationTests {

	private static int amqpPort;

	private RabbitTemplate template;

	private ConnectionFactory connectionFactory;

	static {
		GenericContainer rabbitmq = new GenericContainer("rabbitmq:3.5.3")
				.withExposedPorts(5672);
		rabbitmq.start();
		final Integer mappedPort = rabbitmq.getMappedPort(5672);
		amqpPort = mappedPort;
	}

	@BeforeEach
	void setupTest() {
		this.connectionFactory = new CachingConnectionFactory("localhost", amqpPort);
		this.template = new RabbitTemplate(this.connectionFactory);
		this.template.setMessageConverter(new Jackson2JsonMessageConverter());
		AmqpAdmin admin = new RabbitAdmin(this.connectionFactory);
		admin.declareQueue(new Queue("foo"));

		Map<Object, Object> testMap = new HashMap<>();
		testMap.put("ITEM_NAME", "foo");
		this.template.convertAndSend("foo", testMap);
		testMap = new HashMap<>();
		testMap.put("ITEM_NAME", "bar");
		this.template.convertAndSend("foo", testMap);
		testMap = new HashMap<>();
		testMap.put("ITEM_NAME", "baz");
		this.template.convertAndSend("foo", testMap);
	}

	@AfterEach
	void teardownTest() {
		AmqpAdmin admin = new RabbitAdmin(this.connectionFactory);
		admin.deleteQueue("foo");
		this.template.destroy();
	}

	@Test
	void basicTest() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(BaseConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
								BatchAutoConfiguration.class,
								SingleStepJobAutoConfiguration.class,
								AmqpItemReaderAutoConfiguration.class))
				.withPropertyValues("spring.batch.job.jobName=integrationJob",
						"spring.batch.job.stepName=step1", "spring.batch.job.chunkSize=5",
						"spring.batch.job.amqpitemreader.name=fooReader",
						"spring.batch.job.amqpitemreader.defaultReceiveQueue=foo",
						"spring.batch.job.amqpitemreader.port=" + amqpPort);

		applicationContextRunner.run((context) -> {
			JobLauncher jobLauncher = context.getBean(JobLauncher.class);

			Job job = context.getBean(Job.class);

			JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			List<Map<Object, Object>> items = context.getBean(ListItemWriter.class)
					.getWrittenItems();

			assertThat(items.size()).isEqualTo(3);
			assertThat(items.get(0).get("ITEM_NAME")).isEqualTo("foo");
			assertThat(items.get(1).get("ITEM_NAME")).isEqualTo("bar");
			assertThat(items.get(2).get("ITEM_NAME")).isEqualTo("baz");
		});
	}

	@Test
	void missingDefaultQueueTest() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(BaseConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
								BatchAutoConfiguration.class,
								SingleStepJobAutoConfiguration.class,
								AmqpItemReaderAutoConfiguration.class))
				.withPropertyValues("spring.batch.job.jobName=integrationJob",
						"spring.batch.job.stepName=step1", "spring.batch.job.chunkSize=5",
						"spring.batch.job.amqpitemreader.name=fooReader",
						"spring.batch.job.amqpitemreader.port=" + amqpPort);

		assertThatThrownBy(() -> {
			applicationContextRunner.run((context) -> {
				JobLauncher jobLauncher = context.getBean(JobLauncher.class);
			});
		}).isInstanceOf(IllegalStateException.class).getRootCause()
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("DefaultReceiveQueue must not be empty nor null");
	}

	@EnableBatchProcessing
	@Configuration
	public static class RowMapperConfiguration {

		@Bean
		public RowMapper<Map<Object, Object>> rowMapper() {
			return (rs, rowNum) -> {
				Map<Object, Object> item = new HashMap<>();

				item.put("item", rs.getString("item_name"));

				return item;
			};
		}

		@Bean
		public ListItemWriter<Map<Object, Object>> itemWriter() {
			return new ListItemWriter<>();
		}

	}

	@EnableBatchProcessing
	@Configuration
	public static class BaseConfiguration {

		@Bean
		public ListItemWriter<Map<Object, Object>> itemWriter() {
			return new ListItemWriter<>();
		}

	}

}
