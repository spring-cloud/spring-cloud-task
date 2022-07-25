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

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties to configure a {@code KafkaItemReader}.
 *
 * @author Glenn Renfro
 * @since 2.3
 */
@ConfigurationProperties(prefix = "spring.batch.job.kafkaitemreader")
public class KafkaItemReaderProperties {

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}.
	 */
	private String name;

	/**
	 * The topic name from which the messages is read.
	 */
	private String topic;

	/**
	 * A list of partitions to manually assign to the consumer. Defaults to a single entry
	 * value of 1.
	 */
	private List<Integer> partitions = new ArrayList<>();

	/**
	 * Establish the {@code pollTimeout} for the {@code poll()} operations. Defaults to 30
	 * seconds.
	 */
	private long pollTimeOutInSeconds = 30L;

	/**
	 * Configure whether the state of the
	 * {@link org.springframework.batch.item.ItemStreamSupport} should be persisted within
	 * the {@link org.springframework.batch.item.ExecutionContext} for restart purposes.
	 * Defaults to {@code true}.
	 */
	private boolean saveState = true;

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

	/**
	 * A list of partitions to manually assign to the consumer. Defaults to a single entry
	 * value of 1.
	 * @return the list of partitions.
	 */
	public List<Integer> getPartitions() {
		return partitions;
	}

	/**
	 * A list of partitions to manually assign to the consumer. Defaults to a single entry
	 * value of 1.
	 * @param partitions list of partitions
	 */
	public void setPartitions(List<Integer> partitions) {
		this.partitions = partitions;
	}

	/**
	 * Get the pollTimeout for the poll() operations. Defaults to 30 seconds.
	 * @return long containing the poll timeout.
	 */
	public long getPollTimeOutInSeconds() {
		return pollTimeOutInSeconds;
	}

	/**
	 * Set the pollTimeout for the poll() operations. Defaults to 30 seconds.
	 * @param pollTimeOutInSeconds the number of seconds to wait before timing out.
	 */
	public void setPollTimeOutInSeconds(long pollTimeOutInSeconds) {
		this.pollTimeOutInSeconds = pollTimeOutInSeconds;
	}

	/**
	 * Configure if the state of the
	 * {@link org.springframework.batch.item.ItemStreamSupport} should be persisted within
	 * the {@link org.springframework.batch.item.ExecutionContext} for restart purposes.
	 * Defaults to true.
	 * @return current status of the saveState flag.
	 */
	public boolean isSaveState() {
		return saveState;
	}

	/**
	 * Configure if the state of the
	 * {@link org.springframework.batch.item.ItemStreamSupport} should be persisted within
	 * the {@link org.springframework.batch.item.ExecutionContext} for restart purposes.
	 * @param saveState true if state should be persisted. Defaults to true.
	 */
	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}

}
