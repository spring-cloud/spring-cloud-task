/*
 * Copyright 2015 the original author or authors.
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
	 * @param taskExecution the taskExecution to be stored.
	 */
	void saveTaskExecution(TaskExecution taskExecution);

	/**
	 * Update and existing {@link TaskExecution}.
	 *
	 * @param taskExecution the taskExecution to be updated.
	 */
	void updateTaskExecution(TaskExecution taskExecution);

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
	 * Retrieves current number of task executions.
	 *
	 * @return current number of task executions.
	 */
	long getTaskExecutionCount();

	/**
	 * Retrieves a set of task executions that are running for a taskName.
	 *
	 * @param taskName the name of the task to search for in the repository.
	 * @return set of running task executions.
	 */
	 Set<TaskExecution> findRunningTaskExecutions(String taskName);

	/**
	 * Retrieves a subset of task executions by task name, start location and size.
	 *
	 * @param taskName the name of the task to search for in the repository.
	 * @param start the position of the first entry to be returned from result set.
	 * @param count the number of entries to return
	 * @return a list that contains task executions from the query bound by the start
	 * position and count specified by the user.
	 */
	List<TaskExecution> getTaskExecutionsByName(String taskName, int start, int count);

	/**
	 * Retrieves a sorted list of distinct task names for the task executions.
	 *
	 * @return a list of distinct task names from the task repository..
	 */
	public List<String> getTaskNames();

	/**
	 * Retrieves all the task executions within the pageable constraints.
	 * @param pageable the constraints for the search
	 * @return page containing the results from the search
	 */

	public Page<TaskExecution> findAll(Pageable pageable);

	/**
	 * Retrieves the next available execution id for a task execution.
	 * @return long containing the executionId.
	 */
	public long getNextExecutionId();
}
