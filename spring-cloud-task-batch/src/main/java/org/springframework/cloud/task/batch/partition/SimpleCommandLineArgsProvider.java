/*
 * Copyright 2016 the original author or authors.
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

import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.util.Assert;

/**
 * Returns any command line arguments used with the {@link TaskExecution} provided.
 *
 * @author Michael Minella
 * @since 1.1.0
 */
public class SimpleCommandLineArgsProvider implements CommandLineArgsProvider {

	private final TaskExecution taskExecution;

	public SimpleCommandLineArgsProvider(TaskExecution taskExecution) {
		Assert.notNull(taskExecution, "A taskExecution is required");

		this.taskExecution = taskExecution;
	}

	@Override
	public List<String> getCommandLineArgs(ExecutionContext executionContext) {
		return this.taskExecution.getArguments();
	}
}
