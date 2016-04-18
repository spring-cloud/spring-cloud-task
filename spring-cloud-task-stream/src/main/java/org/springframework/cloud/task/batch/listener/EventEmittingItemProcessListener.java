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
import org.springframework.cloud.task.batch.listener.support.MessagePublisher;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * Setups up the ItemProcessListener to emit events to the spring cloud stream output channel.
 *
 * @author Michael Minella
 * @author Glenn Renfro
 */
public class EventEmittingItemProcessListener implements ItemProcessListener {

	MessageChannel output;
	private MessagePublisher<Object> messagePublisher;

	public EventEmittingItemProcessListener(MessageChannel output) {
		Assert.notNull(output, "An output channel is required");
		this.output = output;
		this.messagePublisher = new MessagePublisher(output);
	}

	@Override
	public void beforeProcess(Object item) {
	}

	@Override
	public void afterProcess(Object item, Object result) {
		String message = "";
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
}
