/*
 * Copyright 2016-present the original author or authors.
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

package org.springframework.cloud.task.batch.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.cloud.task.listener.TaskExecutionListener;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * Responsible for storing the relationship between a Spring Batch job and the Spring
 * Cloud task it was executed within.
 *
 * @author Michael Minella
 */
public class TaskBatchExecutionListener implements JobExecutionListener, Ordered, TaskExecutionListener {

	private static final Log logger = LogFactory.getLog(TaskBatchExecutionListener.class);

	@SuppressWarnings("NullAway.Init")
	private TaskExecution taskExecution;

	private final TaskBatchDao taskBatchDao;

	/**
	 * @param taskBatchDao dao used to persist the relationship. Must not be null
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
	public void beforeJob(JobExecution jobExecution) {
		if (this.taskExecution == null) {
			logger.warn("This job was executed outside the scope of a task but still used the task listener.");
		}
		else {
			logger.info(String.format("The job execution id %s was run within the task execution %s",
					jobExecution.getId(), this.taskExecution.getExecutionId()));
			this.taskBatchDao.saveRelationship(this.taskExecution, jobExecution);
		}
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
