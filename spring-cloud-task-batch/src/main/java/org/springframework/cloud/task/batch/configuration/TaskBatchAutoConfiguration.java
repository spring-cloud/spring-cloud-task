/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.cloud.task.batch.configuration;

import java.util.Collection;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.task.batch.listener.TaskBatchExecutionListener;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

/**
 * Provides auto configuration for the {@link TaskBatchExecutionListener}.
 *
 * @author Michael Minella
 */
@Configuration
@ConditionalOnBean({Job.class})
@ConditionalOnProperty(name = "spring.cloud.task.batch.listener.enable", havingValue = "true", matchIfMissing = true)
public class TaskBatchAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TaskBatchExecutionListenerBeanPostProcessor batchTaskExecutionListenerBeanPostProcessor() {
		return new TaskBatchExecutionListenerBeanPostProcessor();
	}

	@Configuration
	@ConditionalOnMissingBean(name = "taskBatchExecutionListener")
	public static class TaskBatchExecutionListenerAutoconfiguration {

		@Autowired(required = false)
		private Collection<DataSource> dataSources;

		@Bean
		public TaskBatchExecutionListenerFactoryBean taskBatchExecutionListener(TaskExplorer taskExplorer) {
			if(!CollectionUtils.isEmpty(dataSources) && dataSources.size() == 1) {
				return new TaskBatchExecutionListenerFactoryBean(dataSources.iterator().next(), taskExplorer);
			}
			else if(CollectionUtils.isEmpty(dataSources)) {
				return new TaskBatchExecutionListenerFactoryBean(null, taskExplorer);
			}
			else {
				throw new IllegalStateException("Expected one datasource and found " + dataSources.size());
			}
		}
	}
}
