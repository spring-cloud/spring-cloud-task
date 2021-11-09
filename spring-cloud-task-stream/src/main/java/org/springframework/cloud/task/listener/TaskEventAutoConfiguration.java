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

package org.springframework.cloud.task.listener;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.config.BindingServiceConfiguration;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.task.batch.listener.support.TaskEventProperties;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author Michael Minella
 * @author Glenn Renfro
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(StreamBridge.class)
@ConditionalOnBean(TaskLifecycleListener.class)
@ConditionalOnExpression("T(org.springframework.util.StringUtils).isEmpty('${spring.batch.job.jobName:}')")
// @checkstyle:off
@ConditionalOnProperty(prefix = "spring.cloud.task.events", name = "enabled",
		havingValue = "true", matchIfMissing = true)
// @checkstyle:on
@PropertySource("classpath:/org/springframework/cloud/task/application.properties")
@AutoConfigureBefore(BindingServiceConfiguration.class)
@AutoConfigureAfter(SimpleTaskAutoConfiguration.class)
@EnableConfigurationProperties(TaskEventProperties.class)
public class TaskEventAutoConfiguration {

	/**
	 * Configuration for a {@link TaskExecutionListener}.
	 */
	@Configuration(proxyBeanMethods = false)
	public static class ListenerConfiguration {
		@Bean
		public TaskExecutionListener taskEventEmitter(StreamBridge streamBridge, TaskEventProperties taskEventProperties) {
			return new TaskExecutionListener() {
				@Override
				public void onTaskStartup(TaskExecution taskExecution) {
					streamBridge.send(taskEventProperties.getTaskEventBindingName(), taskExecution);
				}

				@Override
				public void onTaskEnd(TaskExecution taskExecution) {
					streamBridge.send(taskEventProperties.getTaskEventBindingName(), taskExecution);
				}

				@Override
				public void onTaskFailed(TaskExecution taskExecution, Throwable throwable) {
					streamBridge.send(taskEventProperties.getTaskEventBindingName(), taskExecution);
				}
			};
		}

	}

}
