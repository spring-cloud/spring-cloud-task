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

import java.util.Map;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.batch.item.amqp.AmqpItemReader;
import org.springframework.batch.item.amqp.builder.AmqpItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(AmqpItemReaderProperties.class)
@AutoConfigureAfter(BatchAutoConfiguration.class)
@ConditionalOnProperty(prefix = "spring.batch.job.amqpitemreader", name = "name")
public class AmqpItemReaderAutoConfiguration {

	@Autowired
	private AmqpItemReaderProperties amqpItemReaderProperties;

	@Bean
	public ConnectionFactory connectionFactory() {
		return new CachingConnectionFactory(this.amqpItemReaderProperties.getHost(),
				this.amqpItemReaderProperties.getPort());
	}

	@Bean
	public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate result = new RabbitTemplate(connectionFactory);
		result.setMessageConverter(new Jackson2JsonMessageConverter());

		if (StringUtils.hasText(this.amqpItemReaderProperties.getDefaultReceiveQueue())) {
			result.setDefaultReceiveQueue(
					this.amqpItemReaderProperties.getDefaultReceiveQueue());
		}
		else {
			throw new IllegalArgumentException(
					"DefaultReceiveQueue must not be empty nor null");
		}

		if (StringUtils.hasText(this.amqpItemReaderProperties.getEncoding())) {
			result.setEncoding(this.amqpItemReaderProperties.getEncoding());
		}

		if (this.amqpItemReaderProperties
				.getReceiveTimeout() != AmqpItemReaderProperties.DEFAULT_TIMEOUT) {
			result.setReceiveTimeout(this.amqpItemReaderProperties.getReceiveTimeout());
		}

		return result;
	}

	@Bean
	public AmqpItemReaderProperties amqpItemReaderProperties() {
		return new AmqpItemReaderProperties();
	}

	@Bean
	public Queue myQueue() {
		return new Queue(amqpItemReaderProperties.getDefaultReceiveQueue(), true);
	}

	@Bean
	public AmqpItemReader<Map<Object, Object>> amqpItemReader(AmqpTemplate amqpTemplate) {
		return new AmqpItemReaderBuilder<Map<Object, Object>>().amqpTemplate(amqpTemplate)
				.build();
	}

}
