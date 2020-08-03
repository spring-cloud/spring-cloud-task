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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties to configure a {@code KafkaItemWriter}.
 *
 * @author Glenn Renfro
 * @since 2.3
 */
@ConfigurationProperties(prefix = "spring.batch.job.kafkaitemwriter")
public class KafkaItemWriterProperties {

	private String name;

	private String topic;

	private long pollTimeOutInSeconds = 30L;

	/**
	 * Returns the configured value of the name used to calculate {@code ExecutionContext}
	 * keys.
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}.
	 * @param name name of the writer instance
	 * @see org.springframework.batch.item.ItemStreamSupport#setName(String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the name of the topic from which messages will be read.
	 * @return the name of the topic.
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * The topic name from which the messages will be read.
	 * @param topic name of the topic
	 */
	public void setTopic(String topic) {
		this.topic = topic;
	}

}
