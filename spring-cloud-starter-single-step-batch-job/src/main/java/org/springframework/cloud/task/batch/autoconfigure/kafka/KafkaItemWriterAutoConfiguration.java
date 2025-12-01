/*
 * Copyright 2020-present the original author or authors.
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

import org.springframework.batch.infrastructure.item.kafka.KafkaItemWriter;
import org.springframework.batch.infrastructure.item.kafka.builder.KafkaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.Assert;

/**
 *
 * Autconfiguration for a {@code KafkaItemReader}.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 * @since 2.3
 */
@AutoConfiguration
@EnableConfigurationProperties({ KafkaProperties.class, KafkaItemWriterProperties.class })
@AutoConfigureAfter(BatchAutoConfiguration.class)
public class KafkaItemWriterAutoConfiguration {

	@Autowired
	private KafkaProperties kafkaProperties;

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.batch.job.kafkaitemwriter", name = "topic")
	@SuppressWarnings("NullAway")
	public KafkaItemWriter<Object, Map<String, Object>> kafkaItemWriter(
			KafkaItemWriterProperties kafkaItemWriterProperties,
			ProducerFactory<Object, Map<String, Object>> producerFactory,
			@Qualifier("batchItemKeyMapper") Converter<Map<String, Object>, Object> itemKeyMapper) {

		validateProperties(kafkaItemWriterProperties);
		KafkaTemplate template = new KafkaTemplate(producerFactory);
		template.setDefaultTopic(kafkaItemWriterProperties.getTopic());
		return new KafkaItemWriterBuilder<Object, Map<String, Object>>().delete(kafkaItemWriterProperties.isDelete())
			.kafkaTemplate(template)
			.itemKeyMapper(itemKeyMapper)
			.build();
	}

	@Bean
	@ConditionalOnMissingBean(name = "batchItemKeyMapper")
	public Converter<Map<String, Object>, Object> batchItemKeyMapper() {
		return new Converter<Map<String, Object>, Object>() {
			@Override
			public Object convert(Map<String, Object> source) {
				return source;
			}
		};
	}

	@Bean
	@ConditionalOnMissingBean
	ProducerFactory<Object, Map<String, Object>> producerFactory() {
		Map<String, Object> configs = new HashMap<>();
		configs.putAll(this.kafkaProperties.getProducer().buildProperties());
		return new DefaultKafkaProducerFactory<>(configs, null, new JacksonJsonSerializer());
	}

	private void validateProperties(KafkaItemWriterProperties kafkaItemWriterProperties) {
		Assert.hasText(kafkaItemWriterProperties.getTopic(), "topic must not be empty or null");
	}

}
