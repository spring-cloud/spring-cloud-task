/*
 * Copyright 2016-2021 the original author or authors.
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

import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.cloud.task.batch.listener.support.BatchJobHeaders;
import org.springframework.cloud.task.batch.listener.support.MessagePublisher;
import org.springframework.cloud.task.batch.listener.support.TaskEventProperties;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * Provides informational messages around the {@link ItemProcessListener} of a batch job.
 *
 * The {@link ItemProcessListener#beforeProcess(Object)} of this listener is a no-op.
 * {@link ItemProcessListener#afterProcess(Object, Object)} returns a message if an item
 * was filtered ({@link ItemProcessor} returned null), if the result of the processor was
 * equal to the input (via <code>.equals</code>), or if they were not equal.
 * {@link ItemProcessListener#onProcessError(Object, Exception)} provides the exception
 * via the {@link BatchJobHeaders#BATCH_EXCEPTION} message header.
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Ali Shahbour
 */
public class EventEmittingItemProcessListener implements ItemProcessListener, Ordered {

	private static final Log logger = LogFactory
		.getLog(EventEmittingItemProcessListener.class);

	private MessagePublisher messagePublisher;

	private int order = Ordered.LOWEST_PRECEDENCE;

	private TaskEventProperties properties;

	public EventEmittingItemProcessListener(MessagePublisher messagePublisher, TaskEventProperties properties) {
		Assert.notNull(messagePublisher, "messagePublisher is required");
		Assert.notNull(properties, "properties is required");
		this.messagePublisher = messagePublisher;
		this.properties = properties;
	}

	public EventEmittingItemProcessListener(MessagePublisher messagePublisher, int order, TaskEventProperties properties) {
		this(messagePublisher, properties);
		this.order = order;
	}

	@Override
	public void beforeProcess(Object item) {
	}

	@Override
	public void afterProcess(Object item, Object result) {
		if (result == null) {
			this.messagePublisher.publish(this.properties.getItemProcessEventBindingName(), "1 item was filtered");
		}
		else if (item.equals(result)) {
			this.messagePublisher.publish(this.properties.getItemProcessEventBindingName(), "item equaled result after processing");
		}
		else {
			this.messagePublisher.publish(this.properties.getItemProcessEventBindingName(), "item did not equal result after processing");
		}
	}

	@Override
	public void onProcessError(Object item, Exception e) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onProcessError: " + e.getMessage(), e);
		}
		this.messagePublisher.publishWithThrowableHeader(
			this.properties.getItemProcessEventBindingName(),
			"Exception while item was being processed", e.getMessage());
	}

	@Override
	public int getOrder() {
		return this.order;
	}

}
