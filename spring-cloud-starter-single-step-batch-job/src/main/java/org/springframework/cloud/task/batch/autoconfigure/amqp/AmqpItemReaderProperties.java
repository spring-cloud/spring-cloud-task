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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.batch.job.amqpitemreader")
public class AmqpItemReaderProperties {

	/**
	 * The default millis for receiving a message.
	 */
	public static long DEFAULT_TIMEOUT = 0;

	private String name;

	private String defaultReceiveQueue;

	private String host = "localhost";

	private int port = 5672;

	private long receiveTimeout = DEFAULT_TIMEOUT;

	private String encoding;

	/**
	 * Returns the configured value of the name used to calculate
	 * {@code ExecutionContext}. keys.
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
	 * Returns the queue name that is being used by the
	 * {@link org.springframework.batch.item.amqp.AmqpItemReader}.
	 * @return name of the queue to be used by the reader.
	 */
	public String getDefaultReceiveQueue() {
		return defaultReceiveQueue;
	}

	/**
	 * Establish the name of the queue to be used by
	 * {@link org.springframework.batch.item.amqp.AmqpItemReader}.
	 * @param defaultReceiveQueue the name of the queue to read.
	 */
	public void setDefaultReceiveQueue(String defaultReceiveQueue) {
		this.defaultReceiveQueue = defaultReceiveQueue;
	}

	/**
	 * Returns host of the RabbitMQ Server.
	 * @return the host of the RabbitMQ server to be used.
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Establishes the host of the RabbitMQ Server.
	 * @param host the host to be used by the item reader.
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Returns port of the RabbitMQ Server.
	 * @return the port of the RabbitMQ server to be used.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Establishes the port of the RabbitMQ Server.
	 * @param port the port to be used by the item reader.
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Returns receive timeout to be used by the item reader.
	 * @return the receive timeout to be used by the item reader.
	 */
	public long getReceiveTimeout() {
		return receiveTimeout;
	}

	/**
	 * Establishes the receive timeout to be used by the item reader. Default is zero. Set
	 * to less than zero to wait indefinitely.
	 * @param receiveTimeout the routingKey to be used by the item reader.
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * The encoding used when converting between byte arrays and Strings in message
	 * properties.
	 * @return the encoding to be used by the item reader.
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * Establishes the encoding to be used by the item reader.
	 * @param encoding the encoding to be used by the item reader.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

}
