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
package org.springframework.cloud.task.repository.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

/**
 * Simple implementation of the {@link TaskNameResolver} interface.  Names the task based
 * on the following order of precidence:
 * <ol>
 *    <li>A configured property <code>spring.cloud.task.name</code></li>
 *    <li>The {@link ApplicationContext}'s id.</li>
 * </ol>
 *
 * @author Michael Minella
 * @see org.springframework.boot.context.ContextIdApplicationContextInitializer
 */
public class SimpleTaskNameResolver implements TaskNameResolver, ApplicationContextAware {

	private ApplicationContext context;

	private String configuredName;

	@Value("${spring.cloud.task.name:}")
	public void setConfiguredName(String configuredName) {
		this.configuredName = configuredName;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}

	@Override
	public String getTaskName() {
		if(StringUtils.hasText(configuredName)) {
			return configuredName;
		}
		else {
			return context.getId().replace(":", "_");
		}
	}
}
