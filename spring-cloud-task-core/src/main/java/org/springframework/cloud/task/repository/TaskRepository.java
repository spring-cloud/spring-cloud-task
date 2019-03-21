/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.task.repository;

import java.util.Date;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

/**
 * TaskRepository interface offers methods that create and update task execution
 * information.
 *
 * @author Glenn Renfro
 */
public interface TaskRepository {

	/**
	 * Notifies the repository that a taskExecution has completed.
	 *
	 * @param executionId to the task execution to be updated.
	 * @param exitCode to be stored for this task.
	 * @param endTime designated when the task completed.
	 * @param exitMessage to be stored for the task.
	 * @return the updated {@link TaskExecution}
	 */
	@Transactional
	TaskExecution completeTaskExecution(long executionId, Integer exitCode, Date endTime,
			 String exitMessage);

	/**
	 * Notifies the repository that a taskExecution needs to be created.
	 *
	 * @param taskName the name that associated with the task execution.
	 * @param startTime the time task began.
	 * @param arguments list of key/value pairs that configure the task.
	 * @return the initial {@link TaskExecution}
	 */
	@Transactional
	TaskExecution createTaskExecution(String taskName,
			Date startTime,List<String> arguments);

}
