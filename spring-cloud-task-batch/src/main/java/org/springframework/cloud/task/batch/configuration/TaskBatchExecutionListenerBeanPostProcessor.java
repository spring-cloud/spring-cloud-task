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
package org.springframework.cloud.task.batch.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.job.AbstractJob;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.task.batch.listener.TaskBatchExecutionListener;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

/**
 * Injects a configured {@link TaskBatchExecutionListener} into any batch jobs (beans
 * assignable to {@link AbstractJob}) that are executed within the scope of a task.  The
 * context this is used within is expected to have only one bean of type
 * {@link TaskBatchExecutionListener}.
 *
 * @author Michael Minella
 */
public class TaskBatchExecutionListenerBeanPostProcessor implements BeanPostProcessor {

	@Autowired
	private ApplicationContext applicationContext;

	private List<String> jobNames = new ArrayList<>();

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		if(jobNames.size() > 0) {
			if(!jobNames.contains(beanName)) {
				return bean;
			}
		}
		int length = this.applicationContext
				.getBeanNamesForType(TaskBatchExecutionListener.class).length;

		if(bean instanceof AbstractJob) {
			if(length != 1) {
				throw new IllegalStateException("The application context is required to " +
						"have exactly 1 instance of the TaskBatchExecutionListener but has " +
						length);
			}
			((AbstractJob) bean).registerJobExecutionListener(
					this.applicationContext.getBean(TaskBatchExecutionListener.class));
		}
		return bean;
	}

	public void setJobNames(List<String> jobNames) {
		Assert.notNull(jobNames, "A list is required");

		this.jobNames = jobNames;
	}
}
