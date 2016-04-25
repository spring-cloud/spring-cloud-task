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

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.cloud.task.batch.listener.support.MessagePublisher;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 *  Setups up the ItemWriteEventsListener to emit events to the spring cloud stream output channel.
 *
 * @author Glenn Renfro
 */
public class EventEmittingItemWriteEventsListener implements ItemWriteListener{

	private static final Logger logger = LoggerFactory.getLogger(EventEmittingItemWriteEventsListener.class);

	private MessageChannel output;
	private MessagePublisher<Object> messagePublisher;

	public EventEmittingItemWriteEventsListener(MessageChannel output) {
		Assert.notNull(output, "An output channel is required");
		this.output = output;
		this.messagePublisher = new MessagePublisher(output);
	}

	@Override
	public void beforeWrite(List items) {
		messagePublisher.publish(items.size() + " items to be written.");
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
}
