/*
 * Copyright 2017-present the original author or authors.
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

import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.cloud.task.batch.listener.support.MessagePublisher;
import org.springframework.cloud.task.batch.listener.support.TaskEventProperties;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * Provides informational messages around the {@link Chunk} of a batch job.
 *
 * The {@link ChunkListener#beforeChunk(Chunk)} and
 * {@link ChunkListener#afterChunk(Chunk)} are both no-ops in this implementation.
 * {@link ChunkListener#onChunkError(Exception,Chunk)}.
 *
 * @author Ali Shahbour
 */
public class EventEmittingChunkListener implements ChunkListener, Ordered {

	private int order = Ordered.LOWEST_PRECEDENCE;

	private MessagePublisher messagePublisher;

	private TaskEventProperties properties;

	public EventEmittingChunkListener(MessagePublisher messagePublisher, TaskEventProperties properties) {
		Assert.notNull(messagePublisher, "messagePublisher is required");
		Assert.notNull(properties, "properties is required");
		this.messagePublisher = messagePublisher;
		this.properties = properties;
	}

	public EventEmittingChunkListener(MessagePublisher messagePublisher, int order, TaskEventProperties properties) {
		this(messagePublisher, properties);
		this.order = order;
	}

	@Override
	public void beforeChunk(Chunk chunk) {
		this.messagePublisher.publish(this.properties.getChunkEventBindingName(), "Before Chunk Processing");
	}

	@Override
	public void afterChunk(Chunk chunk) {
		this.messagePublisher.publish(this.properties.getChunkEventBindingName(), "After Chunk Processing");
	}

	@Override
	public void onChunkError(Exception exception, Chunk chunk) {

	}

	@Override
	public int getOrder() {
		return this.order;
	}

}
