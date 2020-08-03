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

	private String topic;

	private boolean delete;

	/**
	 * Returns the name of the topic from which messages will be written.
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

	/**
	 * Indicate if the items being passed to the writer are all to be sent as delete
	 * events to the topic. A delete event is made of a key with a null value. If set to
	 * false (default), the items will be sent with provided value and key converter by
	 * the itemKeyMapper. If set to true, the items will be sent with the key converter
	 * from the value by the itemKeyMapper and a null value.
	 * @return removal indicator.
	 */
	public boolean isDelete() {
		return delete;
	}

	/**
	 * Indicate if the items being passed to the writer are all to be sent as delete
	 * events to the topic. A delete event is made of a key with a null value. If set to
	 * false (default), the items will be sent with provided value and key converter by
	 * the itemKeyMapper. If set to true, the items will be sent with the key converter
	 * from the value by the itemKeyMapper and a null value.
	 * @param delete removal indicator.
	 */
	public void setDelete(boolean delete) {
		this.delete = delete;
	}

}
