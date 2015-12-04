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

package org.springframework.cloud.task.repository.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.util.Assert;

/**
 * Records the task execution information to the log and to TaskExecutionDao provided.
 * @author Glenn Renfro
 */
public class SimpleTaskRepository implements TaskRepository {

	private final static Logger logger = LoggerFactory.getLogger(SimpleTaskRepository.class);

	private TaskExecutionDao taskExecutionDao;

	public SimpleTaskRepository(TaskExecutionDao taskExecutionDao){
		this.taskExecutionDao = taskExecutionDao;
	}

	@Override
	public void update(TaskExecution taskExecution) {
		validateTaskExecution(taskExecution);
		taskExecutionDao.updateTaskExecution(taskExecution);
		logger.info("Updating: " + taskExecution.toString());
	}

	@Override
	public void createTaskExecution(TaskExecution taskExecution) {
		validateTaskExecution(taskExecution);
		taskExecutionDao.saveTaskExecution(taskExecution);
		logger.info("Creating: " + taskExecution.toString());
	}


	/**
	 * Retrieves the taskExecutionDao associated with this repository.
	 * @return the taskExecutionDao
	 */
	public TaskExecutionDao getTaskExecutionDao() {
		return taskExecutionDao;
	}

	/**
	 * Validate TaskExecution. At a minimum a startTime.
	 *
	 * @param taskExecution the taskExecution to be evaluagted.
	 */
	private void validateTaskExecution(TaskExecution taskExecution) {
		Assert.notNull(taskExecution, "taskExecution should not be null");
		Assert.notNull(taskExecution.getExecutionId(), "taskExecutionId should not be null");
		Assert.notNull(taskExecution.getStartTime(), "TaskExecution start time cannot be null.");
	}
}
