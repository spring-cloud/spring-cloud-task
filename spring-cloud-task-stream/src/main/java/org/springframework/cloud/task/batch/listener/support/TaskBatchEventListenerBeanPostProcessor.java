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
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.core.step.item.SimpleChunkProvider;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

/**
 * @author Michael Minella
 */
public class TaskBatchEventListenerBeanPostProcessor implements BeanPostProcessor {

	@Autowired
	private ApplicationContext applicationContext;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

		if(bean instanceof AbstractJob) {
			JobExecutionListener jobExecutionEventsListener = (JobExecutionListener) this.applicationContext.getBean("jobExecutionEventsListener");

			AbstractJob job = (AbstractJob) bean;

			job.registerJobExecutionListener(
					jobExecutionEventsListener);
		}

		if(bean instanceof AbstractStep) {
			StepExecutionListener stepExecutionListener = (StepExecutionListener) this.applicationContext.getBean("stepExecutionEventsListener");

			AbstractStep step = (AbstractStep) bean;

			step.registerStepExecutionListener(stepExecutionListener);

			if(bean instanceof TaskletStep) {

				TaskletStep taskletStep = (TaskletStep) bean;

				taskletStep.registerChunkListener((ChunkListener) this.applicationContext.getBean("chunkEventsListener"));

				Tasklet tasklet = taskletStep.getTasklet();

				if(tasklet instanceof ChunkOrientedTasklet) {

					Field chunkProviderField = ReflectionUtils.findField(ChunkOrientedTasklet.class, "chunkProvider");

					ReflectionUtils.makeAccessible(chunkProviderField);
					SimpleChunkProvider chunkProvider = (SimpleChunkProvider) ReflectionUtils.getField(chunkProviderField, tasklet);

					chunkProvider.registerListener((ItemReadListener) this.applicationContext.getBean("itemReadEventsListener"));
				}
			}
		}

		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
}
