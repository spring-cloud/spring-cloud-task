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

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration extends the micrometer metrics with additional tags such as: task
 * name, application name, instance index and guids. Later are necessary to allow
 * discrimination and aggregation of app metrics by external metrics collection and
 * visualizaiton tools.
 *
 * Use the spring.cloud.task.metrics.common.tags.enabled=false property to disable
 * inserting those tags.
 *
 * @author Christian Tzolov
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.task.metrics.common.tags.enabled",
		havingValue = "true", matchIfMissing = true)
public class SpringCloudTaskMicrometerCommonTagsConfiguration {

	@Value("${spring.cloud.task.name:unknown}")
	private String taskName;

	@Value("${spring.cloud.task.executionid:unknown}")
	private String taskExecutionId;

	@Value("${spring.cloud.task.external-execution-id:unknown}")
	private String taskExternalExecutionId;

	@Value("${spring.cloud.task.parent-execution-id:unknown}")
	private String taskParentExecutionId;

	@Bean
	public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
		return registry -> registry.config().commonTags("task.name", taskName)
				.commonTags("task.execution.id", taskExecutionId)
				.commonTags("task.external.execution.id", taskExternalExecutionId)
				.commonTags("task.parent.execution.id", taskParentExecutionId);
	}

}
