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
package org.springframework.cloud.task.batch.listener.support;

import java.lang.reflect.Field;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.core.step.item.SimpleChunkProcessor;
import org.springframework.batch.core.step.item.SimpleChunkProvider;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.task.batch.listener.BatchEventAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

/**
 * Attaches the listeners to the job and its steps.
 * Based on the type of bean that is being processed will determine what listener is attached.
 * <p>
 *     <ul>
 *         <li>If the bean is of type AbstactJob then the JobExecutionListener is registered with this bean.</li>
 *         <li>If the bean is of type AbstactStep then the StepExecutionListener is registered with this bean.</li>
 *         <li>If the bean is of type TaskletStep then the ChunkEventListener is registered with this bean.</li>
 *         <li>If the tasklet for the TaskletStep is of type ChunkOrientedTasklet the following listeners will be registered. </li>
 *         <ul>
 *             <li>ItemReadListener with the ChunkProvider.</li>
 *             <li>ItemProcessListener with the ChunkProcessor.</li>
 *             <li>ItemWriteEventsListener with the ChunkProcessor.</li>
 *             <li>SkipEventsListener with the ChunkProcessor.</li>
 *         </ul>
 *     </ul>
 * </p>
 * @author Michael Minella
 * @author Glenn Renfro
 */
public class TaskBatchEventListenerBeanPostProcessor implements BeanPostProcessor {

	@Autowired
	private ApplicationContext applicationContext;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

		registerJobExecutionEventListener(bean);

		if (bean instanceof AbstractStep) {
			registerStepExecutionEventListener(bean);
			if (bean instanceof TaskletStep) {
				TaskletStep taskletStep = (TaskletStep) bean;
				Tasklet tasklet = taskletStep.getTasklet();
				registerChunkEventsListener(bean);

				if (tasklet instanceof ChunkOrientedTasklet) {
					Field chunkProviderField = ReflectionUtils.findField(ChunkOrientedTasklet.class, "chunkProvider");
					ReflectionUtils.makeAccessible(chunkProviderField);
					SimpleChunkProvider chunkProvider = (SimpleChunkProvider) ReflectionUtils.getField(chunkProviderField, tasklet);
					Field chunkProcessorField = ReflectionUtils.findField(ChunkOrientedTasklet.class, "chunkProcessor");
					ReflectionUtils.makeAccessible(chunkProcessorField);
					SimpleChunkProcessor chunkProcessor = (SimpleChunkProcessor) ReflectionUtils.getField(chunkProcessorField, tasklet);
					registerItemReadEvents(chunkProvider);
					registerSkipEvents(chunkProvider);
					registerItemProcessEvents(chunkProcessor);
					registerItemWriteEvents(chunkProcessor);
					registerSkipEvents(chunkProcessor);
				}
			}
		}

		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	private void registerItemProcessEvents(SimpleChunkProcessor chunkProcessor) {
		if(this.applicationContext.containsBean(BatchEventAutoConfiguration.ITEM_PROCESS_EVENTS_LISTENER)) {
			chunkProcessor.registerListener((ItemProcessListener) this.applicationContext.getBean(BatchEventAutoConfiguration.ITEM_PROCESS_EVENTS_LISTENER));
		}
	}

	private void registerItemReadEvents(SimpleChunkProvider chunkProvider) {
		if(this.applicationContext.containsBean(BatchEventAutoConfiguration.ITEM_READ_EVENTS_LISTENER)) {
			chunkProvider.registerListener((ItemReadListener) this.applicationContext.getBean(BatchEventAutoConfiguration.ITEM_READ_EVENTS_LISTENER));
		}
	}

	private void registerItemWriteEvents(SimpleChunkProcessor chunkProcessor) {
		if(this.applicationContext.containsBean(BatchEventAutoConfiguration.ITEM_WRITE_EVENTS_LISTENER)) {
			chunkProcessor.registerListener((ItemWriteListener) this.applicationContext.getBean(BatchEventAutoConfiguration.ITEM_WRITE_EVENTS_LISTENER));
		}
	}

	private void registerSkipEvents(SimpleChunkProvider chunkProvider) {
		if (this.applicationContext.containsBean(BatchEventAutoConfiguration.SKIP_EVENTS_LISTENER)) {
			chunkProvider.registerListener((SkipListener) this.applicationContext.getBean(BatchEventAutoConfiguration.SKIP_EVENTS_LISTENER));
		}
	}

	private void registerSkipEvents(SimpleChunkProcessor chunkProcessor) {
		if(this.applicationContext.containsBean(BatchEventAutoConfiguration.SKIP_EVENTS_LISTENER)) {
			chunkProcessor.registerListener((SkipListener) this.applicationContext.getBean(BatchEventAutoConfiguration.SKIP_EVENTS_LISTENER));
		}
	}

	private void registerChunkEventsListener(Object bean) {
		if(this.applicationContext.containsBean(BatchEventAutoConfiguration.CHUNK_EVENTS_LISTENER))
		{
			((TaskletStep)bean).registerChunkListener((ChunkListener)
					this.applicationContext.getBean(BatchEventAutoConfiguration.CHUNK_EVENTS_LISTENER));
		}
	}

	private void registerJobExecutionEventListener(Object bean) {
		if (bean instanceof AbstractJob &&
				this.applicationContext.containsBean(BatchEventAutoConfiguration.JOB_EXECUTION_EVENTS_LISTENER)) {
			JobExecutionListener jobExecutionEventsListener =
					(JobExecutionListener) this.applicationContext.getBean(
							BatchEventAutoConfiguration.JOB_EXECUTION_EVENTS_LISTENER);

			AbstractJob job = (AbstractJob) bean;
			job.registerJobExecutionListener(
					jobExecutionEventsListener);
		}
	}

	private void registerStepExecutionEventListener(Object bean) {
		if (this.applicationContext.containsBean(BatchEventAutoConfiguration.STEP_EXECUTION_EVENTS_LISTENER)) {
			StepExecutionListener stepExecutionListener =
					(StepExecutionListener) this.applicationContext.getBean(BatchEventAutoConfiguration.STEP_EXECUTION_EVENTS_LISTENER);
			AbstractStep step = (AbstractStep) bean;
			step.registerStepExecutionListener(stepExecutionListener);
		}
	}
}
