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

import org.springframework.batch.core.ItemReadListener;
import org.springframework.cloud.task.batch.listener.support.BatchJobHeaders;
import org.springframework.cloud.task.batch.listener.support.MessagePublisher;
import org.springframework.core.Ordered;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 *  Provides informational messages around the {@link ItemReader} of a batch job.
 *
 *  The {@link ItemReadListener#beforeRead()} and
 *  {@link ItemReadListener#afterRead(Object)} are both no-ops in this implementation.
 *  {@link ItemReadListener#onReadError(Exception)} provides the exception
 * via the {@link BatchJobHeaders.BATCH_EXCEPTION} message header.
 *
 * @author Glenn Renfro
 * @author Ali Shahbour
 */
public class EventEmittingItemReadListener implements ItemReadListener, Ordered {

	private static final Log logger = LogFactory.getLog(EventEmittingItemReadListener.class);

	private MessagePublisher<String> messagePublisher;
	private int order = Ordered.LOWEST_PRECEDENCE;

	public EventEmittingItemReadListener(MessageChannel output) {
		Assert.notNull(output, "An output channel is required");
		this.messagePublisher = new MessagePublisher(output);
	}

	public EventEmittingItemReadListener(MessageChannel output, int order) {
		this(output);
		this.order = order;
	}

	@Override
	public void beforeRead() {

	}

	@Override
	public void afterRead(Object item) {

	}

	@Override
	public void onReadError(Exception ex) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onReadError: " + ex.getMessage(), ex);
		}

		messagePublisher.publishWithThrowableHeader("Exception while item was being read", ex.getMessage());
	}

	@Override
	public int getOrder() {
		return this.order;
	}
}
