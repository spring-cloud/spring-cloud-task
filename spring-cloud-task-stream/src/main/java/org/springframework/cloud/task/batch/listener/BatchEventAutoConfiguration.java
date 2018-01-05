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

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.task.batch.listener.support.TaskBatchEventListenerBeanPostProcessor;
import org.springframework.cloud.task.batch.listener.support.TaskEventProperties;
import org.springframework.cloud.task.listener.TaskLifecycleListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.MessageChannel;

/**
 * Autoconfigures Spring Batch listeners designed to emit events on the following channels:
 *
 * <ul>
 *     <li>{@link EventEmittingJobExecutionListener} - job-execution-events</li>
 *     <li>{@link EventEmittingStepExecutionListener} - step-execution-events</li>
 *     <li>{@link ChunkListener} - chunk-events</li>
 *     <li>{@link EventEmittingItemReadListener} - item-read-events</li>
 *     <li>{@link EventEmittingItemProcessListener} - item-process-events</li>
 *     <li>{@link EventEmittingItemWriteListener} - item-write-events</li>
 *     <li>{@link EventEmittingSkipListener} - skip-events</li>
 * </ul>
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Ali Shahbour
 */
@Configuration
@ConditionalOnClass(Job.class)
@ConditionalOnBean(value = { Job.class, TaskLifecycleListener.class })
@ConditionalOnProperty(prefix = "spring.cloud.task.batch.events", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BatchEventAutoConfiguration {

	public final static String JOB_EXECUTION_EVENTS_LISTENER = "jobExecutionEventsListener";
	public final static String CHUNK_EVENTS_LISTENER = "chunkEventsListener";
	public final static String STEP_EXECUTION_EVENTS_LISTENER = "stepExecutionEventsListener";
	public final static String ITEM_READ_EVENTS_LISTENER = "itemReadEventsListener";
	public final static String ITEM_WRITE_EVENTS_LISTENER = "itemWriteEventsListener";
	public final static String ITEM_PROCESS_EVENTS_LISTENER = "itemProcessEventsListener";
	public final static String SKIP_EVENTS_LISTENER = "skipEventsListener";

	@Bean
	@ConditionalOnMissingBean
	public TaskBatchEventListenerBeanPostProcessor batchTaskEventListenerBeanPostProcessor() {
		return new TaskBatchEventListenerBeanPostProcessor();
	}

	@Configuration
	@ConditionalOnClass(EnableBinding.class)
	@EnableBinding(BatchEventsChannels.class)
	@EnableConfigurationProperties(TaskEventProperties.class)
	@ConditionalOnMissingBean(name = JOB_EXECUTION_EVENTS_LISTENER)
	public static class JobExecutionListenerConfiguration {

		@Autowired
		private BatchEventsChannels listenerChannels;

		@Autowired
		private TaskEventProperties taskEventProperties;

		@Bean
		@Lazy
		@ConditionalOnProperty(prefix = "spring.cloud.task.batch.events.job-execution", name = "enabled", havingValue = "true", matchIfMissing = true)
		public JobExecutionListener jobExecutionEventsListener() {
			return new EventEmittingJobExecutionListener(listenerChannels.jobExecutionEvents(),taskEventProperties.getJobExecutionOrder());
		}

		@Bean
		@ConditionalOnProperty(prefix = "spring.cloud.task.batch.events.step-execution", name = "enabled", havingValue = "true", matchIfMissing = true)
		public StepExecutionListener stepExecutionEventsListener() {
			return new EventEmittingStepExecutionListener(listenerChannels.stepExecutionEvents(),taskEventProperties.getStepExecutionOrder());
		}

		@Bean
		@Lazy
		@ConditionalOnProperty(prefix = "spring.cloud.task.batch.events.chunk", name = "enabled", havingValue = "true", matchIfMissing = true)
		public EventEmittingChunkListener chunkEventsListener() {
			return new EventEmittingChunkListener(listenerChannels.chunkEvents(),taskEventProperties.getChunkOrder());
		}

		@Bean
		@ConditionalOnProperty(prefix = "spring.cloud.task.batch.events.item-read", name = "enabled", havingValue = "true", matchIfMissing = true)
		public ItemReadListener itemReadEventsListener() {
			return new EventEmittingItemReadListener(listenerChannels.itemReadEvents(),taskEventProperties.getItemReadOrder());
		}

		@Bean
		@ConditionalOnProperty(prefix = "spring.cloud.task.batch.events.item-write", name = "enabled", havingValue = "true", matchIfMissing = true)
		public ItemWriteListener itemWriteEventsListener() {
			return new EventEmittingItemWriteListener(listenerChannels.itemWriteEvents(),taskEventProperties.getItemWriteOrder());
		}

		@Bean
		@ConditionalOnProperty(prefix = "spring.cloud.task.batch.events.item-process", name = "enabled", havingValue = "true", matchIfMissing = true)
		public ItemProcessListener itemProcessEventsListener() {
			return new EventEmittingItemProcessListener(listenerChannels.itemProcessEvents(),taskEventProperties.getItemProcessOrder());
		}

		@Bean
		@ConditionalOnProperty(prefix = "spring.cloud.task.batch.events.skip", name = "enabled", havingValue = "true", matchIfMissing = true)
		public SkipListener skipEventsListener() {
			return new EventEmittingSkipListener(listenerChannels.skipEvents(),taskEventProperties.getItemProcessOrder());
		}
	}

	public interface BatchEventsChannels {

		String JOB_EXECUTION_EVENTS = "job-execution-events";
		String STEP_EXECUTION_EVENTS = "step-execution-events";
		String CHUNK_EXECUTION_EVENTS = "chunk-events";
		String ITEM_READ_EVENTS = "item-read-events";
		String ITEM_PROCESS_EVENTS = "item-process-events";
		String ITEM_WRITE_EVENTS = "item-write-events";
		String SKIP_EVENTS = "skip-events";

		@Output(JOB_EXECUTION_EVENTS)
		MessageChannel jobExecutionEvents();

		@Output(STEP_EXECUTION_EVENTS)
		MessageChannel stepExecutionEvents();

		@Output(CHUNK_EXECUTION_EVENTS)
		MessageChannel chunkEvents();

		@Output(ITEM_READ_EVENTS)
		MessageChannel itemReadEvents();

		@Output(ITEM_WRITE_EVENTS)
		MessageChannel itemWriteEvents();

		@Output(ITEM_PROCESS_EVENTS)
		MessageChannel itemProcessEvents();

		@Output(SKIP_EVENTS)
		MessageChannel skipEvents();

	}
}
