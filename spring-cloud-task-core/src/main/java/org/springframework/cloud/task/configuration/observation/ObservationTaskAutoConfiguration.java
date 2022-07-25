/*
 * Copyright 2013-2022 the original author or authors.
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

package org.springframework.cloud.task.configuration.observation;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} that registers instrumentation for Spring Cloud Task.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ObservationRegistry.class)
@ConditionalOnProperty(value = "spring.cloud.task.observation.enabled", matchIfMissing = true)
@ConditionalOnBean(ObservationRegistry.class)
public class ObservationTaskAutoConfiguration {

	@Bean
	static ObservationCommandLineRunnerBeanPostProcessor observedCommandLineRunnerBeanPostProcessor(
			BeanFactory beanFactory) {
		return new ObservationCommandLineRunnerBeanPostProcessor(beanFactory);
	}

	@Bean
	static ObservationApplicationRunnerBeanPostProcessor observedApplicationRunnerBeanPostProcessor(
			BeanFactory beanFactory) {
		return new ObservationApplicationRunnerBeanPostProcessor(beanFactory);
	}

}
