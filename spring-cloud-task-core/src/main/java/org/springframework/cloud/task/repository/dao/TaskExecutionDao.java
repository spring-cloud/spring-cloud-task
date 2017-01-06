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

package org.springframework.cloud.task.repository.dao;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Data Access Object for task executions.
 *
 * @author Glenn Renfro
 */
public interface TaskExecutionDao {

	/**
	 * Save a new {@link TaskExecution}.
	 *
	 * @param taskName the name that associated with the task execution.
	 * @param startTime the time task began.
	 * @param arguments list of key/value pairs that configure the task.
	 * @param externalExecutionId id assigned to the task by the platform
	 * @return A fully qualified {@link TaskExecution} instance.
	 */
	TaskExecution createTaskExecution( String taskName,
						   Date startTime, List<String> arguments, String externalExecutionId);

	/**
	 * Save a new {@link TaskExecution}.
	 *
	 * @param taskName the name that associated with the task execution.
	 * @param startTime the time task began.
	 * @param arguments list of key/value pairs that configure the task.
	 * @param externalExecutionId id assigned to the task by the platform
	 * @param parentExecutionId the parent task execution id.
	 * @return A fully qualified {@link TaskExecution} instance.
	 * @since 1.2.0
	 */
	TaskExecution createTaskExecution( String taskName,
			Date startTime, List<String> arguments, String externalExecutionId,
			Long parentExecutionId);

	/**
	 * Update and existing {@link TaskExecution} to mark it as started.
	 *
	 * @param executionId the id of  the taskExecution to be updated.
	 * @param taskName the name that associated with the task execution.
	 * @param startTime the time task began.
	 * @param arguments list of key/value pairs that configure the task.
	 * @param externalExecutionId id assigned to the task by the platform
	 * @since 1.1.0
	 */
	TaskExecution startTaskExecution(long executionId, String taskName,
						   Date startTime, List<String> arguments, String externalExecutionId);

	/**
	 * Update and existing {@link TaskExecution} to mark it as started.
	 *
	 * @param executionId the id of  the taskExecution to be updated.
	 * @param taskName the name that associated with the task execution.
	 * @param startTime the time task began.
	 * @param arguments list of key/value pairs that configure the task.
	 * @param externalExecutionId id assigned to the task by the platform
	 * @param parentExecutionId the parent task execution id.
	 * @since 1.2.0
	 */
	TaskExecution startTaskExecution(long executionId, String taskName,
			Date startTime, List<String> arguments, String externalExecutionId,
			Long parentExecutionId);

	/**
	 * Update and existing {@link TaskExecution} to mark it as completed.
	 *
	 * @param executionId the id of  the taskExecution to be updated.
	 * @param exitCode the status of the task upon completion.
	 * @param endTime the time the task completed.
	 * @param exitMessage the message assigned to the task upon completion.
	 * @param errorMessage error information available upon failure of a task.
	 * @since 1.1.0
	 */
	void completeTaskExecution(long executionId, Integer exitCode, Date endTime, String exitMessage, String errorMessage);

	/**
	 * Update and existing {@link TaskExecution}.
	 *
	 * @param executionId the id of  the taskExecution to be updated.
	 * @param exitCode the status of the task upon completion.
	 * @param endTime the time the task completed.
	 * @param exitMessage the message assigned to the task upon completion.
	 */
	void completeTaskExecution(long executionId, Integer exitCode, Date endTime, String exitMessage);

	/**
	 * Retrieves a task execution from the task repository.
	 *
	 * @param executionId the id associated with the task execution.
	 * @return a fully qualified TaskExecution instance.
	 */
	TaskExecution getTaskExecution(long executionId);

	/**
	 * Retrieves current number of task executions for a taskName.
	 *
	 * @param taskName the name of the task to search for in the repository.
	 * @return current number of task executions for the taskName.
	 */
	long getTaskExecutionCountByTaskName(String taskName);


	/**
	 * Retrieves current number of task executions for a taskName and with an endTime of null.
	 *
	 * @param taskName the name of the task to search for in the repository.
	 * @return current number of task executions for the taskName.
	 */
	long getRunningTaskExecutionCountByTaskName(String taskName);

	/**
	 * Retrieves current number of task executions.
	 *
	 * @return current number of task executions.
	 */
	long getTaskExecutionCount();

	/**
	 * Retrieves a set of task executions that are running for a taskName.
	 * @param taskName the name of the task to search for in the repository.
	 * @param pageable the constraints for the search.
	 * @return set of running task executions.
	 */
	 Page<TaskExecution> findRunningTaskExecutions(String taskName, Pageable pageable);

	/**
	 * Retrieves a subset of task executions by task name, start location and size.
	 * @param taskName the name of the task to search for in the repository.
	 * @param pageable the constraints for the search.
	 * @return a list that contains task executions from the query bound by the start
	 * position and count specified by the user.
	 */
	Page<TaskExecution> findTaskExecutionsByName(String taskName, Pageable pageable);

	/**
	 * Retrieves a sorted list of distinct task names for the task executions.
	 *
	 * @return a list of distinct task names from the task repository..
	 */
	List<String> getTaskNames();

	/**
	 * Retrieves all the task executions within the pageable constraints.
	 * @param pageable the constraints for the search
	 * @return page containing the results from the search
	 */

	Page<TaskExecution> findAll(Pageable pageable);

	/**
	 * Retrieves the next available execution id for a task execution.
	 * @return long containing the executionId.
	 */
	long getNextExecutionId();

	/**
	 * Returns the id of the TaskExecution that the requested Spring Batch job execution
	 * was executed within the context of.  Returns null if non were found.
	 *
	 * @param jobExecutionId the id of the JobExecution
	 * @return the id of the {@link TaskExecution}
	 */
	Long getTaskExecutionIdByJobExecutionId(long jobExecutionId);

	/**
	 * Returns the job execution ids associated with a task execution id.
	 * @param taskExecutionId id of the {@link TaskExecution}
	 * @return a <code>Set</code> of the ids of the job executions executed within the task.
	 */
	Set<Long> getJobExecutionIdsByTaskExecutionId(long taskExecutionId);

	/**
	 * Updates the externalExecutionId for the execution id specified.
	 * @param taskExecutionId the execution id for the task to be updated.
	 * @param externalExecutionId the new externalExecutionId.
	 */
	void updateExternalExecutionId(long taskExecutionId,
			String externalExecutionId);
}
