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

package org.springframework.cloud.task.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties available to configure the task initialization.
 *
 * @author Glenn Renfro
 * @since 2.2.0
 */

@ConfigurationProperties(prefix = "spring.cloud.task.initialize")
public class TaskInitializationProperties {

	/**
	 * If set to true then tables are initialized. If set to false tables are not
	 * initialized. Defaults to true.
	 */
	private boolean enable = true;

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

}
