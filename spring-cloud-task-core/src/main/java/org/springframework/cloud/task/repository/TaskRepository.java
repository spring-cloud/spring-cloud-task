/*
 * Copyright 2015-2017 the original author or authors.
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
	 * Notifies the repository that a taskExecution has completed.
	 *
	 * @param executionId to the task execution to be updated.
	 * @param exitCode to be stored for this task.
	 * @param endTime designated when the task completed.
	 * @param exitMessage to be stored for the task.
	 * @return the updated {@link TaskExecution}
	 * @since 1.1.0
	 */
	@Transactional
	TaskExecution completeTaskExecution(long executionId, Integer exitCode, Date endTime,
			 String exitMessage, String errorMessage);

	/**
	 * Notifies the repository that a taskExecution needs to be created.
	 *
	 * @param taskExecution a TaskExecution instance containing the startTime,
	 * arguments and externalExecutionId that will be stored in the repository.
	 * Only the values enumerated above will be stored for this
	 * TaskExecution.
	 * @return the {@link TaskExecution} that was stored in the repository.  The
	 * TaskExecution's taskExecutionId will also contain the id that was used
	 * to store the TaskExecution.
	 */
	@Transactional
	TaskExecution createTaskExecution(TaskExecution taskExecution);

	/**
	 * Creates an empty TaskExecution with just an id and name provided. This is intended to be
	 * utilized in systems where the request of launching a task is separate from the
	 * actual start of a task (the underlying system may need to deploy the task prior to
	 * launching, etc).
	 *
	 * @param name task name to be associated with the task execution.
	 *
	 * @return the initial {@link TaskExecution}
	 */
	@Transactional
	TaskExecution createTaskExecution(String name);

	/**
	 * Creates an empty TaskExecution with just an id provided. This is intended to be
	 * utilized in systems where the request of launching a task is separate from the
	 * actual start of a task (the underlying system may need to deploy the task prior to
	 * launching, etc).
	 *
	 * @return the initial {@link TaskExecution}
	 */
	@Transactional
	TaskExecution createTaskExecution();

	/**
	 * Notifies the repository that a taskExecution has has started.
	 *
	 * @param executionid         to the task execution to be updated.
	 * @param taskName            the name that associated with the task execution.
	 * @param startTime           the time task began.
	 * @param arguments           list of key/value pairs that configure the task.
	 * @param externalExecutionId id assigned to the task by the platform.
	 * @return TaskExecution created based on the parameters.
	 */
	@Transactional
	TaskExecution startTaskExecution(long executionid, String taskName,
			Date startTime,List<String> arguments, String externalExecutionId);

	/**
	 * Notifies the repository to update the taskExecution's externalExecutionId.
	 *
	 * @param executionid         to the task execution to be updated.
	 * @param externalExecutionId id assigned to the task by the platform.
	 */
	@Transactional
	void updateExternalExecutionId(long executionid,
			String externalExecutionId);

	/**
	 * Notifies the repository that a taskExecution has has started.
	 * @param executionid  to the task execution to be updated.
	 * @param taskName the name that associated with the task execution.
	 * @param startTime the time task began.
	 * @param arguments list of key/value pairs that configure the task.
	 * @param externalExecutionId id assigned to the task by the platform.
	 * @param parentExecutionId the parent task execution id.

	 * @return
	 */
	@Transactional
	TaskExecution startTaskExecution(long executionid, String taskName,
			Date startTime,List<String> arguments, String externalExecutionId,
			Long parentExecutionId);

}
