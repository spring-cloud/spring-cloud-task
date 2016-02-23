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

package org.springframework.cloud.task.listener;

import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;

/**
 * The listener interface for receiving task execution events.
 * @author Glenn Renfro
 */
public interface TaskExecutionListener {

	/**
	 * Invoked after the {@link TaskExecution} has been stored in the {@link TaskRepository}.
	 * @param taskExecution instance containing the information about the current task.
	 */
	public void onTaskStartup(TaskExecution taskExecution);

	/**
	 * Invoked before the {@link TaskExecution} has been updated in the {@link TaskRepository}
	 * upon task end.
	 * @param taskExecution instance containing the information about the current task.
	 */
	public void onTaskEnd(TaskExecution taskExecution);

	/**
	 * Invoked if an uncaught exception occurs during a task execution.  This invocation
	 * will occur before the {@link TaskExecution} has been updated in the {@link TaskRepository}
	 * and before the onTaskEnd is called.
	 * @param taskExecution instance containing the information about the current task.
	 * @param throwable the uncaught exception that was thrown during task execution.
	 */
	public void onTaskFailed(TaskExecution taskExecution, Throwable throwable);
}
