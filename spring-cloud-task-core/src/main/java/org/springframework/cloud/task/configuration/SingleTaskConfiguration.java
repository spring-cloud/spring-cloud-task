/*
 *  Copyright 2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.integration.support.locks.PassThruLockRegistry;

/**
 * Autoconfiguration of {@link SingleInstanceTaskListener}.
 *
 * @author Glenn Renfro
 * @since 2.0.0
 */

@Order(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@ConditionalOnProperty(prefix = "spring.cloud.task", name = "single-instance-enabled", havingValue = "true")
public class SingleTaskConfiguration {

	@Autowired
	private TaskProperties taskProperties;

	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	private TaskConfigurer taskConfigurer;


	@Bean
	public SingleInstanceTaskListener taskListener(TaskNameResolver resolver) {
		if (taskConfigurer.getTaskDataSource() == null) {
			return new SingleInstanceTaskListener(new PassThruLockRegistry(),
					resolver, this.taskProperties, this.applicationEventPublisher);
		}

		return new SingleInstanceTaskListener(taskConfigurer.getTaskDataSource(),
				resolver,
				this.taskProperties,
				this.applicationEventPublisher);
	}
}
