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

package org.springframework.cloud.task.batch.partition;

import java.util.Collections;
import java.util.Map;

import org.springframework.batch.item.ExecutionContext;

/**
 * A simple no-op implementation of the {@link EnvironmentVariablesProvider}. It returns
 * an empty {@link Map}.
 *
 * @author Michael Minella
 * @since 1.0.2
 * @deprecated This feature is now end-of-life and will be removed in a future release. No
 * replacement is planned. Please migrate away from using this functionality.
 */
@Deprecated
public class NoOpEnvironmentVariablesProvider implements EnvironmentVariablesProvider {

	/**
	 * @param executionContext the {@link ExecutionContext} associated with the worker's
	 * step
	 * @return an empty {@link Map}
	 */
	@Override
	public Map<String, String> getEnvironmentVariables(ExecutionContext executionContext) {
		return Collections.emptyMap();
	}

}
