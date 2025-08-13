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

package org.springframework.cloud.task.batch.autoconfigure.rabbit;

import java.util.Map;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.batch.item.amqp.AmqpItemWriter;
import org.springframework.batch.item.amqp.builder.AmqpItemWriterBuilder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autconfiguration for a {@code AmqpItemWriter}.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 * @since 2.3
 */
@AutoConfiguration
@EnableConfigurationProperties(AmqpItemWriterProperties.class)
@AutoConfigureAfter(BatchAutoConfiguration.class)
@ConditionalOnProperty(name = "spring.batch.job.amqpitemwriter.enabled", havingValue = "true", matchIfMissing = false)
public class AmqpItemWriterAutoConfiguration {

	@Bean
	public AmqpItemWriter<Map<String, Object>> amqpItemWriter(AmqpTemplate amqpTemplate) {
		return new AmqpItemWriterBuilder<Map<String, Object>>().amqpTemplate(amqpTemplate).build();
	}

	@Bean
	public AmqpItemWriterProperties amqpItemWriterProperties() {
		return new AmqpItemWriterProperties();
	}

	@ConditionalOnProperty(name = "spring.batch.job.amqpitemwriter.jsonConverterEnabled", havingValue = "true",
			matchIfMissing = true)
	@Bean
	public MessageConverter messageConverter() {
		return new Jackson2JsonMessageConverter();
	}

}
