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

package org.springframework.cloud.task.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Offers methods that allow users to query the task executions that are available.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 */
public interface TaskExplorer {

	/**
	 * Retrieve a {@link TaskExecution} by its id.
	 *
	 * @param executionId the task execution id
	 * @return the {@link TaskExecution} with this id, or null if not found
	 */
	TaskExecution getTaskExecution(long executionId);


	/**
	 * Retrieve a collection of taskExecutions that have the task name provided.
	 *
	 * @param taskName the name of the task
	 * @param pageable the constraints for the search
	 * @return the set of running executions for tasks with the specified name
	 */
	Page<TaskExecution> findRunningTaskExecutions(String taskName, Pageable pageable);

	/**
	 * Retrieve a list of available task names.
	 *
	 * @return the set of task names that have been executed
	 */
	List<String> getTaskNames();

	/**
	 * Get number of executions for a taskName.
	 *
	 * @param taskName the name of the task to be searched
	 * @return the number of running tasks that have the taskname specified
	 */
	long getTaskExecutionCountByTaskName(String taskName);

	/**
	 * Retrieves current number of task executions.
	 *
	 * @return current number of task executions.
	 */
	long getTaskExecutionCount();

	/**
	 * Get a collection/page of executions
	 *
	 * @param taskName the name of the task to be searched
	 * @param pageable the constraints for the search
	 * @return list of task executions
	 */
	Page<TaskExecution> findTaskExecutionsByName(String taskName, Pageable pageable);

	/**
	 * Retrieves all the task executions within the pageable constraints sorted by
	 * start date descending, taskExecution id descending.
	 *
	 * @param pageable the constraints for the search
	 * @return page containing the results from the search
	 */
	Page<TaskExecution> findAll(Pageable pageable);

	/**
	 * Returns the id of the TaskExecution that the requested Spring Batch job execution
	 * was executed within the context of.  Returns null if none were found.
	 *
	 * @param jobExecutionId the id of the JobExecution
	 * @return the id of the {@link TaskExecution}
	 */
	Long getTaskExecutionIdByJobExecutionId(long jobExecutionId);

	/**
	 * Returns a Set of JobExecution ids for the jobs that were executed within the scope
	 * of the requested task.
	 *
	 * @param taskExecutionId id of the {@link TaskExecution}
	 * @return a <code>Set</code> of the ids of the job executions executed within the task.
	 */
	Set<Long> getJobExecutionIdsByTaskExecutionId(long taskExecutionId);
}
