/*
 * Copyright 2016-present the original author or authors.
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

package org.springframework.cloud.task.batch.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.cloud.task.batch.listener.support.BatchJobHeaders;
import org.springframework.cloud.task.batch.listener.support.MessagePublisher;
import org.springframework.cloud.task.batch.listener.support.TaskEventProperties;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * Setups up the ItemWriteEventsListener to emit events to the spring cloud stream output
 * channel.
 *
 * Each method provides an informational message.
 * {@link ItemWriteListener#onWriteError(Exception, Chunk)} provides a message as well as
 * the exception's message via the {@link BatchJobHeaders#BATCH_EXCEPTION} message header.
 *
 * @author Glenn Renfro
 * @author Ali Shahbour
 */
public class EventEmittingItemWriteListener implements ItemWriteListener, Ordered {

	private static final Log logger = LogFactory.getLog(EventEmittingItemWriteListener.class);

	private int order = Ordered.LOWEST_PRECEDENCE;

	private final MessagePublisher<String> messagePublisher;

	private TaskEventProperties properties;

	public EventEmittingItemWriteListener(MessagePublisher messagePublisher, TaskEventProperties properties) {
		Assert.notNull(messagePublisher, "messagePublisher is required");
		Assert.notNull(properties, "properties is required");

		this.messagePublisher = messagePublisher;
		this.properties = properties;
	}

	public EventEmittingItemWriteListener(MessagePublisher messagePublisher, int order,
			TaskEventProperties properties) {
		this(messagePublisher, properties);
		this.order = order;
	}

	@Override
	public void beforeWrite(Chunk items) {
		this.messagePublisher.publish(this.properties.getItemWriteEventBindingName(),
				items.size() + " items to be written.");
	}

	@Override
	public void afterWrite(Chunk items) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing afterWrite: " + items);
		}
		this.messagePublisher.publish(this.properties.getItemWriteEventBindingName(),
				items.size() + " items have been written.");
	}

	@Override
	public void onWriteError(Exception exception, Chunk items) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onWriteError: " + exception.getMessage(), exception);
		}
		String payload = "Exception while " + items.size() + " items are attempted to be written.";
		String message = exception.getMessage();
		this.messagePublisher.publishWithThrowableHeader(this.properties.getItemWriteEventBindingName(), payload,
				message != null ? message : exception.getClass().getName());
	}

	@Override
	public int getOrder() {
		return this.order;
	}

}
