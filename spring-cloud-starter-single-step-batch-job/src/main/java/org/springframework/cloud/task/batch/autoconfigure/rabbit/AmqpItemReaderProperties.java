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

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties to configure a {@code AmqpItemReader}.
 *
 * @author Glenn Renfro
 * @since 2.3
 */
@ConfigurationProperties(prefix = "spring.batch.job.amqpitemreader")
public class AmqpItemReaderProperties {

	/**
	 * Enables or disables the AmqpItemReader.  Defaults to false.
	 */
	private boolean enabled;

	/**
	 * Establishes whether the {@link Jackson2JsonMessageConverter} is to be used as a
	 * message converter.   Defaults to true.
	 */
	private boolean jsonConverterEnabled = true;

	/**
	 * The state of the enabled flag.
	 * @return true if AmqpItemReader is enabled. Otherwise false.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Enables or disables the AmqpItemReader.
	 * @param enabled if true then AmqpItemReader will be enabled. Defaults to false.
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * States whether the {@link Jackson2JsonMessageConverter} is used as a message
	 * converter.
	 * @return true if enabled else false.
	 */
	public boolean isJsonConverterEnabled() {
		return jsonConverterEnabled;
	}

	/**
	 * Establishes whether the {@link Jackson2JsonMessageConverter} is to be used as a
	 * message converter.
	 * @param jsonConverterEnabled true if it is to be enabled else false. Defaults to
	 * true.
	 */
	public void setJsonConverterEnabled(boolean jsonConverterEnabled) {
		this.jsonConverterEnabled = jsonConverterEnabled;
	}

}
