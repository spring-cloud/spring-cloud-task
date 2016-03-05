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
package org.springframework.cloud.task.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.cloud.task.listener.TaskExecutionListener;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.util.Assert;

/**
 * Responsible for storing the relationship between a Spring Batch job and the Spring
 * Cloud task it was executed within.
 *
 * @author Michael Minella
 */
public class TaskBatchExecutionListener extends JobExecutionListenerSupport implements TaskExecutionListener {

	private TaskExecution taskExecution;

	private TaskBatchDao taskBatchDao;

	private final static Logger logger = LoggerFactory.getLogger(TaskBatchExecutionListener.class);

	/**
	 * @param taskBatchDao dao used to persist the relationship.  Must not be null
	 */
	public TaskBatchExecutionListener(TaskBatchDao taskBatchDao) {
		Assert.notNull(taskBatchDao, "A TaskBatchDao is required");

		this.taskBatchDao = taskBatchDao;
	}

	@Override
	public void onTaskStartup(TaskExecution taskExecution) {
		this.taskExecution = taskExecution;
	}

	@Override
	public void onTaskEnd(TaskExecution taskExecution) {
		// no-op
	}

	@Override
	public void onTaskFailed(TaskExecution taskExecution, Throwable throwable) {
		// no-op
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		if(this.taskExecution == null) {
			logger.warn("This job was executed outside the scope of a task but still used the task listener.");
		}
		else {
			taskBatchDao.saveRelationship(taskExecution, jobExecution);
		}
	}
}
