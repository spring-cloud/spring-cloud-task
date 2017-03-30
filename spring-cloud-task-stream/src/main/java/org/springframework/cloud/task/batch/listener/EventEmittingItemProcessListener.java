/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.task.batch.listener;

import org.springframework.batch.core.ItemProcessListener;
import org.springframework.cloud.task.batch.listener.support.BatchJobHeaders;
import org.springframework.cloud.task.batch.listener.support.MessagePublisher;
import org.springframework.core.Ordered;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * Provides informational messages around the {@link ItemProcessListener} of a batch job.
 *
 * The {@link ItemProcessListener#beforeProcess(Object)} of this listener is a no-op.
 * {@link ItemProcessListener#afterProcess(Object, Object)} returns a message if an item
 * was filtered ({@link ItemProcessor} returned null), if the result of the processor was
 * equal to the input (via <code>.equals</code>), or if they were not equal.
 * {@link ItemProcessListener#onProcessError(Object, Exception)} provides the exception
 * via the {@link BatchJobHeaders.BATCH_EXCEPTION} message header.
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Ali Shahbour
 */
public class EventEmittingItemProcessListener implements ItemProcessListener, Ordered {

	private MessagePublisher<String> messagePublisher;
	private int order = Ordered.LOWEST_PRECEDENCE;

	public EventEmittingItemProcessListener(MessageChannel output) {
		Assert.notNull(output, "An output channel is required");
		this.messagePublisher = new MessagePublisher<>(output);
	}

	public EventEmittingItemProcessListener(MessageChannel output, int order) {
		this(output);
		this.order = order;
	}

	@Override
	public void beforeProcess(Object item) {
	}

	@Override
	public void afterProcess(Object item, Object result) {
		if (result == null) {
			messagePublisher.publish("1 item was filtered");
		}
		else if (item.equals(result)) {
			messagePublisher.publish("item equaled result after processing");
		}
		else {
			messagePublisher.publish("item did not equal result after processing");
		}
	}

	@Override
	public void onProcessError(Object item, Exception e) {
		messagePublisher.publishWithThrowableHeader("Exception while item was being processed", e.getMessage());
	}

	@Override
	public int getOrder() {
		return this.order;
	}
}
