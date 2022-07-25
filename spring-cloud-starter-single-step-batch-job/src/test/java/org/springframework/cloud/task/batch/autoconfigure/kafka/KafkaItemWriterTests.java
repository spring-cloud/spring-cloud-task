/*
 * Copyright 2020-2022 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.batch.autoconfigure.SingleStepJobAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedKafka(partitions = 1, topics = { "topic1" })
public class KafkaItemWriterTests {

	private static EmbeddedKafkaBroker embeddedKafkaBroker;

	@BeforeAll
	public static void setupTest(EmbeddedKafkaBroker embeddedKafka) {
		embeddedKafkaBroker = embeddedKafka;
		embeddedKafka.addTopics("topic2");
	}

	@Test
	public void testBaseKafkaItemWriter() {
		final String topicName = "topic1";
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(CustomMappingConfiguration.class)
				.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
						BatchAutoConfiguration.class, SingleStepJobAutoConfiguration.class,
						KafkaItemWriterAutoConfiguration.class, DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.batch.job.jobName=job", "spring.batch.job.stepName=step1",
						"spring.batch.job.chunkSize=5",
						"spring.kafka.producer.bootstrap-servers=" + embeddedKafkaBroker.getBrokersAsString(),
						"spring.kafka.producer.keySerializer=org.springframework.kafka.support.serializer.JsonSerializer",
						"spring.batch.job.kafkaitemwriter.topic=" + topicName);

		applicationContextRunner.run((context) -> {
			waitForTopicPopulation(context);
			validateResults(topicName);
		});
	}

	private void validateResults(String topicName) {
		Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("1", "false", embeddedKafkaBroker));
		Consumer<String, Object> consumer = new DefaultKafkaConsumerFactory<>(configs, new StringDeserializer(),
				new JsonDeserializer<>()).createConsumer();
		consumer.subscribe(singleton(topicName));

		ConsumerRecords<String, Object> consumerRecords = KafkaTestUtils.getRecords(consumer);
		assertThat(consumerRecords.count()).isEqualTo(5);
		List<Map<String, Object>> result = new ArrayList<>();
		consumerRecords.forEach(cs -> {
			result.add((Map<String, Object>) cs.value());
		});
		List<String> firstNames = new ArrayList<>();
		result.forEach(s -> firstNames.add((String) s.get("first_name")));
		assertThat(firstNames.size()).isEqualTo(5);
		assertThat(firstNames).contains("Jane");
		assertThat(firstNames).contains("John");
		assertThat(firstNames).contains("Liz");
		assertThat(firstNames).contains("Cameron");
		assertThat(firstNames).contains("Judy");
	}

	private void waitForTopicPopulation(ApplicationContext context) throws Exception {
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);
		JobExecution jobExecution = jobLauncher.run(job, new JobParameters());
		JobExplorer jobExplorer = context.getBean(JobExplorer.class);

		while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
			Thread.sleep(1000);
		}
	}

	@EnableBatchProcessing
	@Configuration
	public static class CustomMappingConfiguration {

		@Bean
		public ListItemReader<Map<String, Object>> itemReader() {
			List<Map<String, Object>> list = new ArrayList<>(5);
			addNameToReaderList(list, "Jane");
			addNameToReaderList(list, "John");
			addNameToReaderList(list, "Liz");
			addNameToReaderList(list, "Cameron");
			addNameToReaderList(list, "Judy");
			return new ListItemReader<>(list);
		}

		private void addNameToReaderList(List<Map<String, Object>> itemReaderList, String value) {
			Map<String, Object> prepMap = new HashMap<>();
			prepMap.put("first_name", value);
			itemReaderList.add(prepMap);
		}

	}

}
