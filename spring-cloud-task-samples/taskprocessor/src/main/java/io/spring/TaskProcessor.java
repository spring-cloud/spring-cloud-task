/*
 * Copyright 2016-2019 the original author or authors.
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

package io.spring;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.task.launcher.TaskLaunchRequest;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.StringUtils;

/**
 * A processor that takes the maven repository coordinates and datasource configuration
 * for a task and sends a {@link TaskLaunchRequest} message to a task sink.
 *
 * @author Glenn Renfro
 */
@EnableBinding(Processor.class)
@EnableConfigurationProperties(TaskProcessorProperties.class)
public class TaskProcessor {

	@Autowired
	private TaskProcessorProperties processorProperties;

	@Transformer(inputChannel = Processor.INPUT, outputChannel = Processor.OUTPUT)
	public Object setupRequest(String message) {
		Map<String, String> properties = new HashMap<>();
		if (StringUtils.hasText(this.processorProperties.getDataSourceUrl())) {
			properties
				.put("spring_datasource_url", this.processorProperties
					.getDataSourceUrl());
		}
		if (StringUtils
			.hasText(this.processorProperties.getDataSourceDriverClassName())) {
			properties.put("spring_datasource_driverClassName", this.processorProperties
				.getDataSourceDriverClassName());
		}
		if (StringUtils.hasText(this.processorProperties.getDataSourceUserName())) {
			properties.put("spring_datasource_username", this.processorProperties
				.getDataSourceUserName());
		}
		if (StringUtils.hasText(this.processorProperties.getDataSourcePassword())) {
			properties.put("spring_datasource_password", this.processorProperties
				.getDataSourcePassword());
		}
		properties.put("payload", message);

		TaskLaunchRequest request = new TaskLaunchRequest(
			this.processorProperties.getUri(), null, properties, null,
			this.processorProperties.getApplicationName());

		return new GenericMessage<>(request);
	}

}
