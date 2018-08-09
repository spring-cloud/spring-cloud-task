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
package org.springframework.cloud.task.listener;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.config.BindingServiceConfiguration;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.messaging.MessageChannel;

/**
 *
 * @author Michael Minella
 */
@Configuration
@ConditionalOnClass(EnableBinding.class)
@ConditionalOnBean(TaskLifecycleListener.class)
@ConditionalOnProperty(prefix = "spring.cloud.task.events", name = "enabled", havingValue = "true", matchIfMissing = true)
@PropertySource("classpath:/org/springframework/cloud/task/application.properties")
@AutoConfigureBefore(BindingServiceConfiguration.class)
@AutoConfigureAfter(SimpleTaskAutoConfiguration.class)
public class TaskEventAutoConfiguration {

	@Configuration
	@EnableBinding(TaskEventChannels.class)
	public static class ListenerConfiguration {

		@Bean
		public GatewayProxyFactoryBean taskEventListener() {
			GatewayProxyFactoryBean factoryBean =
					new GatewayProxyFactoryBean(TaskExecutionListener.class);

			factoryBean.setDefaultRequestChannelName(TaskEventChannels.TASK_EVENTS);

			return factoryBean;
		}
	}

	public interface TaskEventChannels {

		String TASK_EVENTS = "task-events";

		@Output(TASK_EVENTS)
		MessageChannel taskEvents();
	}
}
