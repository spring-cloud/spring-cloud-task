/*
 *  Copyright 2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.batch.listener;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.ItemWriteListener;
import org.springframework.cloud.task.batch.listener.support.BatchJobHeaders;
import org.springframework.cloud.task.batch.listener.support.MessagePublisher;
import org.springframework.core.Ordered;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 *  Setups up the ItemWriteEventsListener to emit events to the spring cloud stream output channel.
 *
 *  Each method provides an informational message.
 *  {@link ItemWriteListener#onWriteError(Exception, List)} provides a message as well as
 *  the exception's message via the {@link BatchJobHeaders.BATCH_EXCEPTION} message header.
 *
 * @author Glenn Renfro
 * @author Ali Shahbour
 */
public class EventEmittingItemWriteListener implements ItemWriteListener, Ordered {

	private static final Log logger = LogFactory.getLog(EventEmittingItemWriteListener.class);

	private MessagePublisher<String> messagePublisher;
	private int order = Ordered.LOWEST_PRECEDENCE;

	public EventEmittingItemWriteListener(MessageChannel output) {
		Assert.notNull(output, "An output channel is required");
		this.messagePublisher = new MessagePublisher<>(output);
	}

	public EventEmittingItemWriteListener(MessageChannel output, int order) {
		this(output);
		this.order = order;
	}

	@Override
	public void beforeWrite(List items) {
		this.messagePublisher.publish(items.size() + " items to be written.");
	}

	@Override
	public void afterWrite(List items) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing afterWrite: " + items);
		}
		this.messagePublisher.publish(items.size() + " items have been written.");
	}

	@Override
	public void onWriteError(Exception exception, List items) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onWriteError: " + exception.getMessage(), exception);
		}
		String payload = "Exception while " + items.size() + " items are attempted to be written.";
		this.messagePublisher.publishWithThrowableHeader(payload, exception.getMessage());
	}

	@Override
	public int getOrder() {
		return this.order;
	}
}
