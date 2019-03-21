/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.cloud.task.repository.support;

import java.util.List;
import java.util.Set;

import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

/**
 * TaskExplorer for that gathers task information from a task repository.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 * @author Gunnar Hillert
 * @author David Turanski
 */
public class SimpleTaskExplorer implements TaskExplorer {

	private TaskExecutionDao taskExecutionDao;

	public SimpleTaskExplorer(TaskExecutionDaoFactoryBean taskExecutionDaoFactoryBean) {
		Assert.notNull(taskExecutionDaoFactoryBean,
				"taskExecutionDaoFactoryBean must not be null");

		try {
			this.taskExecutionDao = taskExecutionDaoFactoryBean.getObject();
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to create a TaskExecutionDao", e);
		}
	}

	@Override
	public TaskExecution getTaskExecution(long executionId) {
		return this.taskExecutionDao.getTaskExecution(executionId);
	}

	@Override
	public Page<TaskExecution> findRunningTaskExecutions(String taskName,
			Pageable pageable) {
		return this.taskExecutionDao.findRunningTaskExecutions(taskName, pageable);
	}

	@Override
	public List<String> getTaskNames() {
		return this.taskExecutionDao.getTaskNames();
	}

	@Override
	public long getTaskExecutionCountByTaskName(String taskName) {
		return this.taskExecutionDao.getTaskExecutionCountByTaskName(taskName);
	}

	@Override
	public long getTaskExecutionCount() {
		return this.taskExecutionDao.getTaskExecutionCount();
	}

	@Override
	public long getRunningTaskExecutionCount() {
		return this.taskExecutionDao.getRunningTaskExecutionCount();
	}

	@Override
	public Page<TaskExecution> findTaskExecutionsByName(String taskName,
			Pageable pageable) {
		return this.taskExecutionDao.findTaskExecutionsByName(taskName, pageable);
	}

	@Override
	public Page<TaskExecution> findAll(Pageable pageable) {
		return this.taskExecutionDao.findAll(pageable);
	}

	@Override
	public Long getTaskExecutionIdByJobExecutionId(long jobExecutionId) {
		return this.taskExecutionDao.getTaskExecutionIdByJobExecutionId(jobExecutionId);
	}

	@Override
	public Set<Long> getJobExecutionIdsByTaskExecutionId(long taskExecutionId) {
		return this.taskExecutionDao.getJobExecutionIdsByTaskExecutionId(taskExecutionId);
	}

	@Override
	public List<TaskExecution> getLatestTaskExecutionsByTaskNames(String... taskNames) {
		return this.taskExecutionDao.getLatestTaskExecutionsByTaskNames(taskNames);
	}

	@Override
	public TaskExecution getLatestTaskExecutionForTaskName(String taskName) {
		return this.taskExecutionDao.getLatestTaskExecutionForTaskName(taskName);
	}

}
