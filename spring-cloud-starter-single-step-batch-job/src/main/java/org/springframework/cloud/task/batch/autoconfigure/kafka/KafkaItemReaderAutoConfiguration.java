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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import org.springframework.batch.item.kafka.KafkaItemReader;
import org.springframework.batch.item.kafka.builder.KafkaItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 *
 * AutoConfiguration for a {@code KafkaItemReader}.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 * @since 2.3
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ KafkaProperties.class, KafkaItemReaderProperties.class })
@AutoConfigureAfter(BatchAutoConfiguration.class)
public class KafkaItemReaderAutoConfiguration {

	@Autowired
	private KafkaProperties kafkaProperties;

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.batch.job.kafkaitemreader", name = "name")
	public KafkaItemReader<Object, Map<String, Object>> kafkaItemReader(
			KafkaItemReaderProperties kafkaItemReaderProperties) {
		Properties consumerProperties = new Properties();
		consumerProperties.putAll(this.kafkaProperties.getConsumer().buildProperties());
		validateProperties(kafkaItemReaderProperties);
		if (kafkaItemReaderProperties.getPartitions() == null
				|| kafkaItemReaderProperties.getPartitions().size() == 0) {
			kafkaItemReaderProperties.setPartitions(new ArrayList<>(1));
			kafkaItemReaderProperties.getPartitions().add(0);
		}
		return new KafkaItemReaderBuilder<Object, Map<String, Object>>()
				.partitions(kafkaItemReaderProperties.getPartitions())
				.consumerProperties(consumerProperties)
				.name(kafkaItemReaderProperties.getName())
				.pollTimeout(Duration
						.ofSeconds(kafkaItemReaderProperties.getPollTimeOutInSeconds()))
				.saveState(kafkaItemReaderProperties.isSaveState()).topic(kafkaItemReaderProperties.getTopic()).build();
	}

	private void validateProperties(KafkaItemReaderProperties kafkaItemReaderProperties) {
		if (!StringUtils.hasText(kafkaItemReaderProperties.getName())) {
			throw new IllegalArgumentException("Name must not be empty or null");
		}
		if (!StringUtils.hasText(kafkaItemReaderProperties.getTopic())) {
			throw new IllegalArgumentException("Topic must not be empty or null");
		}
		if (!StringUtils.hasText(this.kafkaProperties.getConsumer().getGroupId())) {
			throw new IllegalArgumentException("GroupId must not be empty or null");
		}
		if (this.kafkaProperties.getBootstrapServers() == null
				|| this.kafkaProperties.getBootstrapServers().size() == 0) {
			throw new IllegalArgumentException("Bootstrap Servers must be configured");
		}
	}

}
