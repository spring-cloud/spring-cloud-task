/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.task.batch.listener.support;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Utility class that sends batch job listener payloads to the notification channel.
 *
 * @param <P> payload type
 * @author Glenn Renfro
 */
public class MessagePublisher<P> {


	private final StreamBridge streamBridge;

	public MessagePublisher(StreamBridge streamBridge) {
		Assert.notNull(streamBridge, "streamBridge must not be null");
		this.streamBridge = streamBridge;
	}

	public final void publish(String bindingName, P payload) {
		if (payload instanceof Message) {
			this.publishMessage(bindingName, (Message<?>) payload);
		}
		else {
			Message<P> message = MessageBuilder.withPayload(payload).build();
			this.streamBridge.send(bindingName, message);
		}
	}

	private void publishMessage(String bindingName, Message<?> message) {
		this.streamBridge.send(bindingName, message);
	}

	public void publishWithThrowableHeader(String bindingName, P payload, String header) {
		Message<P> message = MessageBuilder.withPayload(payload)
				.setHeader(BatchJobHeaders.BATCH_EXCEPTION, header).build();
		publishMessage(bindingName, message);
	}

}
