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

import java.util.Map;

import org.springframework.batch.item.ExecutionContext;

/**
 * Strategy interface to allow for advanced configuration of environment variables for
 * each worker in a partitioned job.
 *
 * @author Michael Minella
 *
 * @since 1.0.2
 */
public interface EnvironmentVariablesProvider {

	/**
	 * Provides a {@link Map} of Strings to be used as environment variables.  This method
	 * will be called for each worker step.  For example, if there are 5 partitions, this
	 * method will be called 5 times.
	 *
	 * @param executionContext the {@link ExecutionContext} associated with the worker's
	 * 			step
	 * @return A {@link Map} of values to be used as environment variables
	 */
	Map<String, String> getEnvironmentVariables(ExecutionContext executionContext);
}
