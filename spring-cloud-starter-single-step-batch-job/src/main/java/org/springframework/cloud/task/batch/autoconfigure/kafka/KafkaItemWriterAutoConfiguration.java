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

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.item.kafka.KafkaItemWriter;
import org.springframework.batch.item.kafka.builder.KafkaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.Assert;

/**
 *
 * Autconfiguration for a {@code KafkaItemReader}.
 *
 * @author Glenn Renfro
 * @since 2.3
 */
@Configuration
@EnableConfigurationProperties({ KafkaProperties.class, KafkaItemWriterProperties.class })
@AutoConfigureAfter(BatchAutoConfiguration.class)
public class KafkaItemWriterAutoConfiguration {

	@Autowired
	private KafkaProperties kafkaProperties;

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.batch.job.kafkaitemwriter", name = "name")
	public KafkaItemWriter<Object, Map<Object, Object>> kafkaItemWriter(
			KafkaItemWriterProperties kafkaItemWriterProperties,
			ProducerFactory<Object, Map<Object, Object>> producerFactory) {

		validateProperties(kafkaItemWriterProperties);
		KafkaTemplate template = new KafkaTemplate(producerFactory);
		template.setDefaultTopic(kafkaItemWriterProperties.getTopic());
		return new KafkaItemWriterBuilder<Object, Map<Object, Object>>().delete(false)
				.kafkaTemplate(template).itemKeyMapper(new Converter() {
					@Override
					public Object convert(Object source) {
						return "";
					}
				}).build();
	}

	@Bean
	@ConditionalOnMissingBean
	ProducerFactory<Object, Map<Object, Object>> producerFactory() {
		Map<String, Object> configs = new HashMap<>();
		configs.putAll(this.kafkaProperties.getProducer().buildProperties());
		return new DefaultKafkaProducerFactory<Object, Map<Object, Object>>(configs, null,
				new JsonSerializer<>());
	}

	private void validateProperties(KafkaItemWriterProperties kafkaItemWriterProperties) {
		Assert.hasText(kafkaItemWriterProperties.getTopic(),
				"topic must not be empty or null");
		Assert.hasText(kafkaItemWriterProperties.getName(),
				"name must not be empty or null");
	}

}
