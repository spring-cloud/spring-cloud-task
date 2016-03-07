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

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.batch.listener.TaskBatchExecutionListener;
import org.springframework.cloud.task.batch.listener.support.JdbcTaskBatchDao;
import org.springframework.cloud.task.batch.listener.support.MapTaskBatchDaoFactoryBean;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.configuration.SimpleTaskConfiguration;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.type.AnnotatedTypeMetadata;

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

	@Configuration
	@ConditionalOnBean(DataSource.class)
	@ConditionalOnMissingBean(name = "batchTaskExecutionListener")
	public static class JdbcTaskBatchExecutionListenerConfiguration {

		@Bean
		public TaskBatchExecutionListener batchTaskExecutionListener(DataSource dataSource) {
			return new TaskBatchExecutionListener(new JdbcTaskBatchDao(dataSource));
		}
	}

	@Configuration
	@Conditional(NoDataSourceCondition.class)
	@ConditionalOnMissingBean(name = "batchTaskExecutionListener")
	public static class MapTaskBatchExecutionListenerConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@DependsOn("taskExplorer")
		public TaskBatchExecutionListener batchTaskExecutionListener(SimpleTaskExplorer taskExplorer) throws Exception {
			return new TaskBatchExecutionListener(taskBatchDao(taskExplorer));
		}

		@Bean
		@ConditionalOnMissingBean
		@DependsOn("taskExplorer")
		public TaskBatchDao taskBatchDao(SimpleTaskExplorer taskExplorer) throws Exception {
			MapTaskBatchDaoFactoryBean mapTaskBatchDaoFactoryBean = new MapTaskBatchDaoFactoryBean();

			mapTaskBatchDaoFactoryBean.setTaskExplorer(taskExplorer);

			return mapTaskBatchDaoFactoryBean.getObject();
		}
	}

	public static class NoDataSourceCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			String[] beanNamesForType = context.getBeanFactory().getBeanNamesForType(DataSource.class);
			return new ConditionOutcome(beanNamesForType.length == 0, "");
		}
	}
}
