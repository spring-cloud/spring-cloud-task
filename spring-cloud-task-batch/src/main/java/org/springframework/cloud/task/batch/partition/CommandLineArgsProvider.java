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

import java.util.List;

import org.springframework.batch.item.ExecutionContext;

/**
 * Strategy to allow for the customization of command line arguments passed to each
 * partition's execution.
 *
 * @author Michael Minella
 * @since 1.1.0
 */
public interface CommandLineArgsProvider {

	/**
	 * Returns a unique list of command line arguements to be passed to the partition's
	 * worker for the specified {@link ExecutionContext}.
	 *
	 * Note: This method is called once per partition.
	 *
	 * @param executionContext the unique state for the step to be executed.
	 * @return a list of formatted command line arguments to be passed to the worker (the
	 *         list will be joined via spaces).
	 */
	List<String> getCommandLineArgs(ExecutionContext executionContext);
}
