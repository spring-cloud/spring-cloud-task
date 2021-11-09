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

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.cloud.task.batch.listener.support.JobExecutionEvent;
import org.springframework.cloud.task.batch.listener.support.MessagePublisher;
import org.springframework.cloud.task.batch.listener.support.TaskEventProperties;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * Provides {@link JobExecutionEvent} at both the start and end of the job's execution.
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Ali Shahbour
 */
public class EventEmittingJobExecutionListener implements JobExecutionListener, Ordered {

	private int order = Ordered.LOWEST_PRECEDENCE;

	private final MessagePublisher<JobExecutionEvent> messagePublisher;

	private TaskEventProperties properties;

	public EventEmittingJobExecutionListener(MessagePublisher messagePublisher, TaskEventProperties properties) {
		Assert.notNull(messagePublisher, "messagePublisher is required");
		Assert.notNull(properties, "properties is required");

		this.messagePublisher = messagePublisher;
		this.properties = properties;
	}

	public EventEmittingJobExecutionListener(MessagePublisher messagePublisher, int order, TaskEventProperties properties) {
		this(messagePublisher, properties);
		this.order = order;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		this.messagePublisher.publish(properties.getJobExecutionEventBindingName(), new JobExecutionEvent(jobExecution));
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		this.messagePublisher.publish(properties.getJobExecutionEventBindingName(), new JobExecutionEvent(jobExecution));
	}

	@Override
	public int getOrder() {
		return this.order;
	}

}
