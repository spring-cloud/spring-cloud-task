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

package org.springframework.cloud.task.batch.autoconfigure.rabbit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
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
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.batch.autoconfigure.SingleStepJobAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.RowMapper;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("DockerRequired")
public class AmqpItemWriterAutoConfigurationTests {

	private final static String QUEUE_NAME = "foo";

	private final static String EXCHANGE_NAME = "fooexchange";

	private static int amqpPort;

	private static String host;

	private static List<Map<String, Object>> sampleData;

	private RabbitTemplate template;

	private ConnectionFactory connectionFactory;

	private String[] configurations;

	static {
		GenericContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.8.9")
				.withExposedPorts(5672);
		rabbitmq.start();
		final Integer mappedPort = rabbitmq.getMappedPort(5672);
		host = rabbitmq.getContainerIpAddress();
		amqpPort = mappedPort;
		sampleData = new ArrayList<>(5);
		addNameToReaderList(sampleData, "Jane");
		addNameToReaderList(sampleData, "John");
		addNameToReaderList(sampleData, "Liz");
		addNameToReaderList(sampleData, "Cameron");
		addNameToReaderList(sampleData, "Judy");
	}

	private static void addNameToReaderList(List<Map<String, Object>> itemReaderList,
			String value) {
		Map<String, Object> prepMap = new HashMap<>();
		prepMap.put("first_name", value);
		itemReaderList.add(prepMap);
	}

	@BeforeEach
	void setupTest() {
		this.connectionFactory = new CachingConnectionFactory(host, amqpPort);
		this.template = new RabbitTemplate(this.connectionFactory);
		this.template.setMessageConverter(new Jackson2JsonMessageConverter());
		AmqpAdmin admin = new RabbitAdmin(this.connectionFactory);
		admin.declareQueue(new Queue(QUEUE_NAME));
		admin.declareExchange(new TopicExchange(EXCHANGE_NAME));
		admin.declareBinding(new Binding(QUEUE_NAME, Binding.DestinationType.QUEUE,
				EXCHANGE_NAME, "#", null));
		this.configurations = new String[] { "spring.batch.job.jobName=integrationJob",
				"spring.batch.job.stepName=step1", "spring.batch.job.chunkSize=5",
				"spring.rabbitmq.template.exchange=" + EXCHANGE_NAME,
				"spring.rabbitmq.host=" + host,
				"spring.batch.job.amqpitemwriter.enabled=true",
				"spring.rabbitmq.port=" + amqpPort };
	}

	@AfterEach
	void teardownTest() {
		AmqpAdmin admin = new RabbitAdmin(this.connectionFactory);
		admin.deleteQueue(QUEUE_NAME);
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
								AmqpItemWriterAutoConfiguration.class,
								RabbitAutoConfiguration.class))
				.withPropertyValues(this.configurations);

		applicationContextRunner.run((context) -> {
			JobExecution jobExecution = runJob(context);
			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			for (Map<String, Object> sampleEntry : sampleData) {
				Map<String, Object> map = (Map<String, Object>) template
						.receiveAndConvert(QUEUE_NAME);
				assertThat(map.get("first_name"))
						.isEqualTo(sampleEntry.get("first_name"));
			}
		});
	}

	@Test
	void useAmqpTemplateTest() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(MockConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
								BatchAutoConfiguration.class,
								SingleStepJobAutoConfiguration.class,
								AmqpItemWriterAutoConfiguration.class))
				.withPropertyValues(this.configurations);

		applicationContextRunner.run((context) -> {
			runJob(context);
			AmqpTemplate amqpTemplate = context.getBean(AmqpTemplate.class);
			Mockito.verify(amqpTemplate, Mockito.times(5)).convertAndSend(Mockito.any());
		});
	}

	private JobExecution runJob(AssertableApplicationContext context) throws Exception {
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);

		Job job = context.getBean(Job.class);

		return jobLauncher.run(job, new JobParameters());
	}

	@EnableBatchProcessing
	@Configuration
	public static class BaseConfiguration extends ItemWriterConfiguration {

	}

	@EnableBatchProcessing
	@Configuration
	public static class MockConfiguration extends ItemWriterConfiguration {

		@Bean
		AmqpTemplate amqpTemplateBean() {
			return Mockito.mock(AmqpTemplate.class);
		}

	}

	public static class ItemWriterConfiguration {

		@Bean
		public RowMapper<Map<String, Object>> rowMapper() {
			return (rs, rowNum) -> {
				Map<String, Object> item = new HashMap<>();

				item.put("item", rs.getString("item_name"));

				return item;
			};
		}

		@Bean
		public ItemReader<Map<String, Object>> itemWriter() {

			return new ListItemReader<>(sampleData);
		}

	}

}
