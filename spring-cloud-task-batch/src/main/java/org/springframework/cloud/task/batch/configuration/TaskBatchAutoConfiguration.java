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

import org.springframework.batch.core.Job;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.task.batch.listener.TaskBatchExecutionListener;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides auto configuration for the {@link TaskBatchExecutionListener}.
 *
 * @author Michael Minella
 */
@Configuration
@ConditionalOnClass({Job.class, EnableTask.class})
public class TaskBatchAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TaskBatchExecutionListenerBeanPostProcessor batchTaskExecutionListenerBeanPostProcessor() {
		return new TaskBatchExecutionListenerBeanPostProcessor();
	}

	@Bean
	@ConditionalOnMissingBean
	public TaskBatchExecutionListenerFactoryBean taskBatchExecutionListener(ConfigurableApplicationContext context) {
		return new TaskBatchExecutionListenerFactoryBean(context);
	}
}
