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
package org.springframework.cloud.task.batch.partition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * Copies all existing environment variables as made available in the {@link Environment}
 * only if includeCurrentEnvironment is set to true (default).
 * The <code>environmentProperties</code> option provides the ability to override any
 * specific values on an as needed basis.
 *
 * @author Michael Minella
 *
 * @since 1.0.2
 */
public class SimpleEnvironmentVariablesProvider implements EnvironmentVariablesProvider {

	private Environment environment;

	private Map<String, String> environmentProperties = new HashMap<>(0);

	private boolean includeCurrentEnvironment = true;

	/**
	 * @param environment The {@link Environment} for this context
	 */
	public SimpleEnvironmentVariablesProvider(Environment environment) {
		this.environment = environment;
	}

	/**
	 * @param environmentProperties a {@link Map} of properties used to override any values
	 * configured in the current {@link Environment}
	 */
	public void setEnvironmentProperties(Map<String, String> environmentProperties) {
		this.environmentProperties = environmentProperties;
	}

	/**
	 * Establishes if current environment variables will be included as a part of the provider.
	 * @param includeCurrentEnvironment true(default) include local environment properties.  False do not include
	 * current environment properties.
	 */
	public void setIncludeCurrentEnvironment(boolean includeCurrentEnvironment) {
		this.includeCurrentEnvironment = includeCurrentEnvironment;
	}

	@Override
	public Map<String, String> getEnvironmentVariables(ExecutionContext executionContext) {

		Map<String, String> environmentProperties = new HashMap<>(this.environmentProperties.size());

		if(includeCurrentEnvironment) {
			environmentProperties.putAll(getCurrentEnvironmentProperties());
		}

		environmentProperties.putAll(this.environmentProperties);

		return environmentProperties;
	}

	private Map<String, String> getCurrentEnvironmentProperties() {
		Map<String, String> currentEnvironment = new HashMap<>();

		Set<String> keys = new HashSet<>();

		for (PropertySource<?> propertySource : ((AbstractEnvironment) this.environment).getPropertySources()) {
			if (propertySource instanceof MapPropertySource) {
				keys.addAll(Arrays.asList(((MapPropertySource) propertySource).getPropertyNames()));
			}
		}

		for (String key : keys) {
			currentEnvironment.put(key, this.environment.getProperty(key));
		}

		return currentEnvironment;
	}
}
