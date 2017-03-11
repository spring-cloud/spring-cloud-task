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

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.cloud.task.batch.listener.support.StepExecutionEvent;
import org.springframework.cloud.task.batch.listener.support.MessagePublisher;
import org.springframework.core.Ordered;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * Provides a {@link StepExecutionEvent} at the start and end of each step indicating the
 * step's status.  The {@link StepExecutionListener#afterStep(StepExecution)} returns the
 * {@link ExitStatus} of the inputted {@link StepExecution}.
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Ali Shahbour
 */
public class EventEmittingStepExecutionListener implements StepExecutionListener, Ordered {

	private MessagePublisher<StepExecutionEvent> messagePublisher;
	private int order = Ordered.LOWEST_PRECEDENCE;

	public EventEmittingStepExecutionListener(MessageChannel output) {
		Assert.notNull(output, "An output channel is required");
		this.messagePublisher = new MessagePublisher<>(output);
	}

	public EventEmittingStepExecutionListener(MessageChannel output, int order) {
		this(output);
		this.order = order;
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		this.messagePublisher.publish(new StepExecutionEvent(stepExecution));
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		this.messagePublisher.publish(new StepExecutionEvent(stepExecution));

		return stepExecution.getExitStatus();
	}

	@Override
	public int getOrder() {
		return this.order;
	}
}
