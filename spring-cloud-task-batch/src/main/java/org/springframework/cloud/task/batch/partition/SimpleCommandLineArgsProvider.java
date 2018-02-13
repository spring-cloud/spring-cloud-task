/*
 * Copyright 2016-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.cloud.task.listener.TaskExecutionListenerSupport;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.util.Assert;

/**
 * Returns any command line arguments used with the {@link TaskExecution} provided
 * appended with any additional arguments configured.
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @since 1.1.0
 */
public class SimpleCommandLineArgsProvider extends TaskExecutionListenerSupport implements CommandLineArgsProvider {

	private TaskExecution taskExecution;

	private List<String> appendedArgs;

	public SimpleCommandLineArgsProvider() {
	}

	/**
	 * @param taskExecution task execution
	 */
	public SimpleCommandLineArgsProvider(TaskExecution taskExecution) {
		Assert.notNull(taskExecution, "A taskExecution is required");

		this.taskExecution = taskExecution;
	}

	@Override
	public void onTaskStartup(TaskExecution taskExecution) {
		this.taskExecution = taskExecution;
	}

	/**
	 * Additional command line args to be appended.
	 *
	 * @param appendedArgs list of arguments
	 * @since 1.2
	 */
	public void setAppendedArgs(List<String> appendedArgs) {
		this.appendedArgs = appendedArgs;
	}

	@Override
	public List<String> getCommandLineArgs(ExecutionContext executionContext) {

		int listSize = this.taskExecution.getArguments().size() +
				(this.appendedArgs != null ? this.appendedArgs.size() : 0);

		List<String> args = new ArrayList<>(listSize);

		args.addAll(this.taskExecution.getArguments());

		if(this.appendedArgs != null) {
			args.addAll(this.appendedArgs);
		}

		return args;
	}
}
