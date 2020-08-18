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

import java.util.Map;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.batch.item.amqp.AmqpItemReader;
import org.springframework.batch.item.amqp.builder.AmqpItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Autconfiguration for a {@code AmqpItemReader}.
 *
 * @author Glenn Renfro
 * @since 2.3
 */
@Configuration
@EnableConfigurationProperties(AmqpItemReaderProperties.class)
@AutoConfigureAfter(BatchAutoConfiguration.class)
@ConditionalOnProperty(name = "spring.batch.job.amqpitemreader.enabled",
		havingValue = "true", matchIfMissing = false)
public class AmqpItemReaderAutoConfiguration {

	@Autowired(required = false)
	private RabbitProperties rabbitProperties;

	@Bean
	public AmqpItemReaderProperties amqpItemReaderProperties() {
		return new AmqpItemReaderProperties();
	}

	@ConditionalOnBean(RabbitProperties.class)
	@Bean
	public Queue myQueue() {
		if (!StringUtils
				.hasText(this.rabbitProperties.getTemplate().getDefaultReceiveQueue())) {
			throw new IllegalArgumentException(
					"DefaultReceiveQueue must not be empty nor null");
		}
		return new Queue(this.rabbitProperties.getTemplate().getDefaultReceiveQueue(),
				true);
	}

	@Bean
	public AmqpItemReader<Map<Object, Object>> amqpItemReader(AmqpTemplate amqpTemplate,
			@Autowired(required = false) Class itemType) {
		AmqpItemReaderBuilder<Map<Object, Object>> builder = new AmqpItemReaderBuilder<Map<Object, Object>>()
				.amqpTemplate(amqpTemplate);
		if (itemType != null) {
			builder.itemType(itemType);
		}
		return builder.build();
	}

	@ConditionalOnProperty(name = "spring.batch.job.amqpitemreader.jsonConverterEnabled",
			havingValue = "true", matchIfMissing = true)
	@Bean
	public MessageConverter messageConverter() {
		return new Jackson2JsonMessageConverter();
	}

}
