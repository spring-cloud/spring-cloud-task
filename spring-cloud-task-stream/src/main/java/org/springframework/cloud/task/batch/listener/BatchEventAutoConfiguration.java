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
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.task.batch.listener.support.TaskBatchEventListenerBeanPostProcessor;
import org.springframework.cloud.task.listener.TaskLifecycleListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.messaging.MessageChannel;

/**
 * @author Michael Minella
 */
@Configuration
@ConditionalOnBean(value = {Job.class, TaskLifecycleListener.class})
@ConditionalOnProperty(prefix = "spring.cloud.task.batch.events", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BatchEventAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TaskBatchEventListenerBeanPostProcessor batchTaskExecutionListenerBeanPostProcessor() {
		return new TaskBatchEventListenerBeanPostProcessor();
	}

	@Configuration
	@EnableBinding(BatchEventsChannels.class)
	@ConditionalOnMissingBean(name = "jobExecutionEventsListener")
	public static class JobExecutionListenerConfiguration {

		@Autowired
		private BatchEventsChannels listenerChannels;

		@Bean
		@Lazy
		public GatewayProxyFactoryBean jobExecutionEventsListener() {
			GatewayProxyFactoryBean factoryBean =
					new GatewayProxyFactoryBean(JobExecutionListener.class);

			factoryBean.setDefaultRequestChannel(listenerChannels.jobExecutionEvents());

			return factoryBean;
		}

		@Bean
		public StepExecutionListener stepExecutionEventsListener() {
			EventEmittingStepExecutionListener listener =
					new EventEmittingStepExecutionListener(listenerChannels.stepExecutionEvents());

			return listener;
		}

		@Bean
		@Lazy
		public GatewayProxyFactoryBean chunkEventsListener() {
			GatewayProxyFactoryBean factoryBean =
					new GatewayProxyFactoryBean(ChunkListener.class);

			factoryBean.setDefaultRequestChannel(listenerChannels.chunkEvents());

			return factoryBean;
		}

		@Bean
		@Lazy
		public GatewayProxyFactoryBean itemReadEventsListener() {
			GatewayProxyFactoryBean factoryBean =
					new GatewayProxyFactoryBean(ItemReadListener.class);

			factoryBean.setDefaultRequestChannel(listenerChannels.itemReadEvents());

			return factoryBean;
		}

		@Bean
		@Lazy
		public GatewayProxyFactoryBean itemWriteEventsListener() {
			GatewayProxyFactoryBean factoryBean =
					new GatewayProxyFactoryBean(ItemWriteListener.class);

			factoryBean.setDefaultRequestChannel(listenerChannels.itemWriteEvents());

			return factoryBean;
		}

		@Bean
		@Lazy
		public GatewayProxyFactoryBean itemProcessEventsListener() {
			GatewayProxyFactoryBean factoryBean =
					new GatewayProxyFactoryBean(ItemProcessListener.class);

			factoryBean.setDefaultRequestChannel(listenerChannels.itemProcessEvents());

			return factoryBean;
		}
	}

	public interface BatchEventsChannels {

		String JOB_EXECUTION_EVENTS = "job-execution-events";
		String STEP_EXECUTION_EVENTS = "step-execution-events";
		String CHUNK_EXECUTION_EVENTS = "chunk-events";
		String ITEM_READ_EVENTS = "item-read-events";
		String ITEM_PROCESS_EVENTS = "item-process-events";
		String ITEM_WRITE_EVENTS = "item-write-events";

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
	}
}
