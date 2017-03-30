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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.SkipListener;
import org.springframework.cloud.task.batch.listener.support.BatchJobHeaders;
import org.springframework.cloud.task.batch.listener.support.MessagePublisher;
import org.springframework.core.Ordered;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * Setups up the SkipProcessListener to emit events to the spring cloud stream output channel.
 *
 * This listener emits the exception's message via the
 * {@link BatchJobHeaders.BATCH_EXCEPTION} message header for each method.  For
 * {@link SkipListener#onSkipInProcess(Object, Throwable)} and
 * {@link SkipListener#onSkipInWrite(Object, Throwable)} the body of the message consists
 * of the item that caused the error.
 *
 * @author Glenn Renfro
 * @author Ali Shahbour
 */
public class EventEmittingSkipListener implements SkipListener, Ordered {

	private static final Log logger = LogFactory.getLog(EventEmittingSkipListener.class);

	private MessagePublisher<Object> messagePublisher;
	private int order = Ordered.LOWEST_PRECEDENCE;

	public EventEmittingSkipListener(MessageChannel output) {
		Assert.notNull(output, "An output channel is required");
		this.messagePublisher = new MessagePublisher<>(output);
	}

	public EventEmittingSkipListener(MessageChannel output, int order) {
		this(output);
		this.order = order;
	}

	@Override
	public void onSkipInRead(Throwable t) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onSkipInRead: " + t.getMessage(), t);
		}
		messagePublisher.publishWithThrowableHeader("Skipped when reading.", t.getMessage());
	}

	@Override
	public void onSkipInWrite(Object item, Throwable t) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onSkipInWrite: " + t.getMessage(), t);
		}
		messagePublisher.publishWithThrowableHeader(item, t.getMessage());
	}

	@Override
	public void onSkipInProcess(Object item, Throwable t) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onSkipInProcess: " + t.getMessage(), t);
		}
		messagePublisher.publishWithThrowableHeader(item, t.getMessage());
	}

	@Override
	public int getOrder() {
		return this.order;
	}
}
