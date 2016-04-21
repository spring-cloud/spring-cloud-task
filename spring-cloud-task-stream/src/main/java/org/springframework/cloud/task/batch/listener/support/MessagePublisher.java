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

package org.springframework.cloud.task.batch.listener.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Utility class that sends batch job listener payloads to the  notification channel.
 * @author Glenn Renfro
 */
public class MessagePublisher<P>{
	private final MessageChannel listenerEventsChannel;

	public MessagePublisher(MessageChannel listenerEventsChannel) {
		Assert.notNull(listenerEventsChannel, "listenerEventsChannel must not be null");
		this.listenerEventsChannel = listenerEventsChannel;
	}

	public final void publish(P payload) {
		if (payload instanceof Message) {
			this.publishMessage((Message<?>) payload);
		}
		else {
			Message<P> message = MessageBuilder.withPayload(payload).build();
			this.listenerEventsChannel.send(message);
		}
	}

	private final void publishMessage(Message<?> message) {
		this.listenerEventsChannel.send(message);
	}

	public void publishWithThrowableHeader(P payload, String header) {
		Message<P> message = MessageBuilder.withPayload(payload).setHeader(BatchJobHeaders.BATCH_EXCEPTION,
				header).build();
		publishMessage(message);
	}
}
