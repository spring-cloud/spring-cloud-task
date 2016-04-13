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
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * @author Michael Minella
 */
public class EventEmittingItemProcessListener implements ItemProcessListener {

	private MessageChannel output;

	public EventEmittingItemProcessListener(MessageChannel output) {
		Assert.notNull(output, "An output channel is required");

		this.output = output;
	}

	@Override
	public void beforeProcess(Object item) {
		this.output.send(MessageBuilder.withPayload(item).build());
	}

	@Override
	public void afterProcess(Object item, Object result) {

	}

	@Override
	public void onProcessError(Object item, Exception e) {

	}
}
