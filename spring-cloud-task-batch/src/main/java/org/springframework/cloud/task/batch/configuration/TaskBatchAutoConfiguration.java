/*
 * Copyright 2016-2018 the original author or authors.
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.task.batch.listener.TaskBatchExecutionListener;
import org.springframework.cloud.task.configuration.TaskConfigurer;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides auto configuration for the {@link TaskBatchExecutionListener}.
 *
 * The spring.cloud.task.batch.listener.enable is deprecated,
 * spring.cloud.task.batch.listener.enabled should be used.
 *
 * @author Michael Minella
 */
@Configuration
@ConditionalOnBean({Job.class})
@ConditionalOnProperty(name = {"spring.cloud.task.batch.listener.enable", "spring.cloud.task.batch.listener.enabled"}, havingValue = "true", matchIfMissing = true)
public class TaskBatchAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TaskBatchExecutionListenerBeanPostProcessor batchTaskExecutionListenerBeanPostProcessor() {
		return new TaskBatchExecutionListenerBeanPostProcessor();
	}

	@Configuration
	@ConditionalOnMissingBean(name = "taskBatchExecutionListener")
	@EnableConfigurationProperties(TaskProperties.class)
	public static class TaskBatchExecutionListenerAutoconfiguration {

		@Autowired
		private ApplicationContext context;

		@Autowired
		private TaskProperties taskProperties;

		@Bean
		public TaskBatchExecutionListenerFactoryBean taskBatchExecutionListener(TaskExplorer taskExplorer) {
			TaskConfigurer taskConfigurer = null;
			if(!this.context.getBeansOfType(TaskConfigurer.class).isEmpty()) {
				taskConfigurer = this.context.getBean(TaskConfigurer.class);
			}
			if(taskConfigurer != null && taskConfigurer.getTaskDataSource() != null) {
				return new TaskBatchExecutionListenerFactoryBean(
						taskConfigurer.getTaskDataSource(),
						taskExplorer, taskProperties.getTablePrefix());
			}
			else {
				return new TaskBatchExecutionListenerFactoryBean(null,
						taskExplorer, taskProperties.getTablePrefix());
			}
		}
	}
}
