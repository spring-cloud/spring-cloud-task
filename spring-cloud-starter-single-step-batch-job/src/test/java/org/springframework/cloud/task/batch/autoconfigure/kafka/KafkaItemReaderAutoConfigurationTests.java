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

package org.springframework.cloud.task.batch.autoconfigure.kafka;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedKafka(partitions = 1, topics = { "test" })
public class KafkaItemReaderAutoConfigurationTests {

	private static EmbeddedKafkaBroker embeddedKafkaBroker;

	@BeforeAll
	public static void setupTest(EmbeddedKafkaBroker embeddedKafka) {
		embeddedKafkaBroker = embeddedKafka;
		embeddedKafka.addTopics(new NewTopic("topic1", 1, (short) 1),
				new NewTopic("topic2", 2, (short) 1),
				new NewTopic("topic3", 1, (short) 1));
	}

	@Test
	public void testBaseKafkaItemReader() {
		final String topicName = "topic1";
		populateSingleTopic(topicName);
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(CustomMappingConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
								BatchAutoConfiguration.class,
								SingleStepJobAutoConfiguration.class,
								KafkaItemReaderAutoConfiguration.class))
				.withPropertyValues("spring.batch.job.jobName=job",
						"spring.batch.job.stepName=step1", "spring.batch.job.chunkSize=5",
						"spring.kafka.consumer.bootstrap-servers="
								+ embeddedKafkaBroker.getBrokersAsString(),
						"spring.kafka.consumer.group-id=1",
						"spring.batch.job.kafkaitemreader.name=kafkaItemReader",
						"spring.batch.job.kafkaitemreader.poll-time-out-in-seconds=2",
						"spring.batch.job.kafkaitemreader.topic=" + topicName,
						"spring.kafka.consumer.value-deserializer="
								+ JsonDeserializer.class.getName());

		applicationContextRunner.run((context) -> {
			JobLauncher jobLauncher = context.getBean(JobLauncher.class);

			Job job = context.getBean(Job.class);

			ListItemWriter itemWriter = context.getBean(ListItemWriter.class);

			JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			List<Map<Object, Object>> writtenItems = itemWriter.getWrittenItems();

			assertThat(writtenItems.size()).isEqualTo(4);
			assertThat(writtenItems.get(0).get("first_name")).isEqualTo("jane");
			assertThat(writtenItems.get(1).get("first_name")).isEqualTo("john");
			assertThat(writtenItems.get(2).get("first_name")).isEqualTo("susan");
			assertThat(writtenItems.get(3).get("first_name")).isEqualTo("jim");
		});
	}

	@Test
	public void testBaseKafkaItemReaderMultiplePartitions() {
		final String topicName = "topic2";
		populateSingleTopic(topicName);
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(CustomMappingConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
								BatchAutoConfiguration.class,
								SingleStepJobAutoConfiguration.class,
								KafkaItemReaderAutoConfiguration.class))
				.withPropertyValues("spring.batch.job.jobName=job",
						"spring.batch.job.stepName=step1", "spring.batch.job.chunkSize=5",
						"spring.kafka.consumer.bootstrap-servers="
								+ embeddedKafkaBroker.getBrokersAsString(),
						"spring.kafka.consumer.group-id=1",
						"spring.batch.job.kafkaitemreader.name=kafkaItemReader",
						"spring.batch.job.kafkaitemreader.partitions=0,1",
						"spring.batch.job.kafkaitemreader.poll-time-out-in-seconds=2",
						"spring.batch.job.kafkaitemreader.topic=" + topicName,
						"spring.kafka.consumer.value-deserializer="
								+ JsonDeserializer.class.getName());

		applicationContextRunner.run((context) -> {
			JobLauncher jobLauncher = context.getBean(JobLauncher.class);

			Job job = context.getBean(Job.class);

			ListItemWriter itemWriter = context.getBean(ListItemWriter.class);

			JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			basicValidation(itemWriter);
		});
	}

	@Test
	public void testBaseKafkaItemReaderPollTimeoutDefault() {
		final String topicName = "topic3";
		populateSingleTopic(topicName);
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(CustomMappingConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
								BatchAutoConfiguration.class,
								SingleStepJobAutoConfiguration.class,
								KafkaItemReaderAutoConfiguration.class))
				.withPropertyValues("spring.batch.job.jobName=job",
						"spring.batch.job.stepName=step1", "spring.batch.job.chunkSize=5",
						"spring.kafka.consumer.bootstrap-servers="
								+ embeddedKafkaBroker.getBrokersAsString(),
						"spring.kafka.consumer.group-id=1",
						"spring.batch.job.kafkaitemreader.name=kafkaItemReader",
						"spring.batch.job.kafkaitemreader.topic=" + topicName,
						"spring.kafka.consumer.value-deserializer="
								+ JsonDeserializer.class.getName());
		Date startTime = new Date();
		applicationContextRunner.run((context) -> {
			JobLauncher jobLauncher = context.getBean(JobLauncher.class);

			Job job = context.getBean(Job.class);

			ListItemWriter itemWriter = context.getBean(ListItemWriter.class);

			JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}
			Date endTime = new Date();
			long seconds = (endTime.getTime() - startTime.getTime()) / 1000;
			assertThat(seconds).isGreaterThanOrEqualTo(30);
			basicValidation(itemWriter);
		});
	}

	private void basicValidation(ListItemWriter itemWriter) {
		List<Map<Object, Object>> writtenItems = itemWriter.getWrittenItems();
		assertThat(writtenItems.size()).isEqualTo(4);
		List<Object> results = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			results.add(writtenItems.get(i).get("first_name"));
		}

		assertThat(results).contains("jane", "john", "susan", "jim");
	}

	private void populateSingleTopic(String topic) {
		Map<String, Object> configps = new HashMap<>(
				KafkaTestUtils.producerProps(embeddedKafkaBroker));
		Producer<String, Object> producer = new DefaultKafkaProducerFactory<>(configps,
				new StringSerializer(), new JsonSerializer<>()).createProducer();
		Map<Object, Object> testMap = new HashMap<>();
		testMap.put("first_name", "jane");
		producer.send(new ProducerRecord<>(topic, "my-aggregate-id", testMap));
		testMap = new HashMap<>();
		testMap.put("first_name", "john");
		producer.send(new ProducerRecord<>(topic, "my-aggregate-id", testMap));
		testMap = new HashMap<>();
		testMap.put("first_name", "susan");
		producer.send(new ProducerRecord<>(topic, "my-aggregate-id", testMap));
		testMap = new HashMap<>();
		testMap.put("first_name", "jim");
		producer.send(new ProducerRecord<>(topic, "my-aggregate-id", testMap));
		producer.flush();
		producer.close();
	}

	@EnableBatchProcessing
	@Configuration
	public static class CustomMappingConfiguration {
		@Bean
		public ListItemWriter<Map> itemWriter() {
			return new ListItemWriter<>();
		}
	}

}
