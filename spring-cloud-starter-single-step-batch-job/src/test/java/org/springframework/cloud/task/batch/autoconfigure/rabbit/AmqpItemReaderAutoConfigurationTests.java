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
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.batch.autoconfigure.SingleStepJobAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("DockerRequired")
public class AmqpItemReaderAutoConfigurationTests {

	private static int amqpPort;

	private static String host;

	private RabbitTemplate template;

	private ConnectionFactory connectionFactory;

	static {
		GenericContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.8.9")
				.withExposedPorts(5672);
		rabbitmq.start();
		final Integer mappedPort = rabbitmq.getMappedPort(5672);
		host = rabbitmq.getContainerIpAddress();
		amqpPort = mappedPort;
	}

	@BeforeEach
	void setupTest() {
		this.connectionFactory = new CachingConnectionFactory(host, amqpPort);
		this.template = new RabbitTemplate(this.connectionFactory);
		this.template.setMessageConverter(new Jackson2JsonMessageConverter());
		AmqpAdmin admin = new RabbitAdmin(this.connectionFactory);
		admin.declareQueue(new Queue("foo"));

		Map<String, Object> testMap = new HashMap<>();
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
								AmqpItemReaderAutoConfiguration.class,
								RabbitAutoConfiguration.class))
				.withPropertyValues("spring.batch.job.jobName=integrationJob",
						"spring.batch.job.stepName=step1", "spring.batch.job.chunkSize=5",
						"spring.batch.job.amqpitemreader.enabled=true",
						"spring.rabbitmq.template.default-receive-queue=foo",
						"spring.rabbitmq.host=" + host,
						"spring.rabbitmq.port=" + amqpPort);

		applicationContextRunner.run((context) -> {
			JobExecution jobExecution = runJob(context);

			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			validateBasicTest(context.getBean(ListItemWriter.class).getWrittenItems());
		});
	}

	@Test
	void basicTestWithItemType() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(ItemTypeConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
								BatchAutoConfiguration.class,
								SingleStepJobAutoConfiguration.class,
								AmqpItemReaderAutoConfiguration.class,
								RabbitAutoConfiguration.class))
				.withPropertyValues("spring.batch.job.jobName=integrationJob",
						"spring.batch.job.stepName=step1", "spring.batch.job.chunkSize=5",
						"spring.batch.job.amqpitemreader.enabled=true",
						"spring.rabbitmq.template.default-receive-queue=foo",
						"spring.rabbitmq.host=" + host,
						"spring.rabbitmq.port=" + amqpPort);

		applicationContextRunner.run((context) -> {
			JobExecution jobExecution = runJob(context);

			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}
			validateBasicTest(context.getBean(ListItemWriter.class).getWrittenItems());
		});
	}

	@Test
	void useAmqpTemplateTest() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(MockTemplateConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
								BatchAutoConfiguration.class,
								SingleStepJobAutoConfiguration.class,
								AmqpItemReaderAutoConfiguration.class))
				.withPropertyValues("spring.batch.job.jobName=integrationJob",
						"spring.batch.job.stepName=step1", "spring.batch.job.chunkSize=5",
						"spring.batch.job.amqpitemreader.enabled=true",
						"spring.rabbitmq.host=" + host,
						"spring.rabbitmq.port=" + amqpPort);

		applicationContextRunner.run((context) -> {
			runJob(context);
			AmqpTemplate amqpTemplate = context.getBean(AmqpTemplate.class);
			Mockito.verify(amqpTemplate, Mockito.times(1)).receiveAndConvert();
		});
	}

	private JobExecution runJob(AssertableApplicationContext context) throws Exception {
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);
		return jobLauncher.run(job, new JobParameters());
	}

	private void validateBasicTest(List<Map<String, Object>> items) {
		assertThat(items.size()).isEqualTo(3);
		assertThat(items.get(0).get("ITEM_NAME")).isEqualTo("foo");
		assertThat(items.get(1).get("ITEM_NAME")).isEqualTo("bar");
		assertThat(items.get(2).get("ITEM_NAME")).isEqualTo("baz");
	}

	public static class MockTemplateConfiguration extends BaseConfiguration {

		@Bean
		AmqpTemplate amqpTemplateBean() {
			return Mockito.mock(AmqpTemplate.class);
		};

	}

	public static class ItemTypeConfiguration extends BaseConfiguration {

		@Bean
		Class<?> itemTypeClass() {
			return Map.class;
		}

	}

	@EnableBatchProcessing
	@Configuration
	public static class BaseConfiguration {

		@Bean
		public ListItemWriter<Map<String, Object>> itemWriter() {
			return new ListItemWriter<>();
		}

	}

}
