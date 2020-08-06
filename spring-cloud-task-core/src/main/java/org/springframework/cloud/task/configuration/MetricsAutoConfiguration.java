/*
 * Copyright 2019-2019 the original author or authors.
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

package org.springframework.cloud.task.configuration;

import java.util.Arrays;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Forked and simplified version of the
 * org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration form
 * spring-boot-actuator
 *
 * {@link EnableAutoConfiguration Auto-configuration} for Micrometer-based metrics.
 *
 * @author Michael Minella
 * @since 2.2
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Timed.class)
@AutoConfigureBefore(name = {
		"org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration" })
public class MetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public Clock micrometerClockFork() {
		return Clock.SYSTEM;
	}

	@Bean
	public static MeterRegistryPostProcessor meterRegistryPostProcessorFork() {
		return new MeterRegistryPostProcessor();
	}

	static class MeterRegistryPostProcessor
			implements BeanPostProcessor, EnvironmentAware {

		private Environment environment;

		@Value("${spring.cloud.task.name:unknown}")
		private String taskName;

		@Value("${spring.cloud.task.executionid:unknown}")
		private String taskExecutionId;

		@Value("${spring.cloud.task.external-execution-id:unknown}")
		private String taskExternalExecutionId;

		@Value("${spring.cloud.task.parent-execution-id:unknown}")
		private String taskParentExecutionId;

		@Value("${vcap.application.org_name:default}")
		private String organizationName;

		@Value("${vcap.application.space_id:unknown}")
		private String spaceId;

		@Value("${vcap.application.space_name:unknown}")
		private String spaceName;

		@Value("${vcap.application.application_name:unknown}")
		private String applicationName;

		@Value("${vcap.application.application_id:unknown}")
		private String applicationId;

		@Value("${vcap.application.application_version:unknown}")
		private String applicationVersion;

		@Value("${vcap.application.instance_index:0}")
		private String instanceIndex;

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {

			if (bean instanceof MeterRegistry) {
				MeterRegistry registry = (MeterRegistry) bean;

				if (Arrays.asList(this.environment.getActiveProfiles())
						.contains("cloud")) {
					registry.config().commonTags("cf.org.name", organizationName)
							.commonTags("cf.space.id", spaceId)
							.commonTags("cf.space.name", spaceName)
							.commonTags("cf.app.id", applicationId)
							.commonTags("cf.app.name", applicationName)
							.commonTags("cf.app.version", applicationVersion)
							.commonTags("cf.instance.index", instanceIndex);
				}

				registry.config().commonTags("task.name", taskName)
						.commonTags("task.execution.id", taskExecutionId)
						.commonTags("task.external.execution.id", taskExternalExecutionId)
						.commonTags("task.parent.execution.id", taskParentExecutionId);
			}

			return bean;
		}

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

	}

}
