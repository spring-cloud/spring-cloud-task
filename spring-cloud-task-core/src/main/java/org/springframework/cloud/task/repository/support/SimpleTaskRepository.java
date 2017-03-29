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

package org.springframework.cloud.task.repository.support;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.util.Assert;

/**
 * Records the task execution information to the log and to TaskExecutionDao provided.
 * @author Glenn Renfro
 */
public class SimpleTaskRepository implements TaskRepository {

	public static final int MAX_EXIT_MESSAGE_SIZE = 2500;
	public static final int MAX_TASK_NAME_SIZE = 100;
	public static final int MAX_ERROR_MESSAGE_SIZE = 2500;

	private final static Log logger = LogFactory.getLog(SimpleTaskRepository.class);

	private TaskExecutionDao taskExecutionDao;

	private FactoryBean<TaskExecutionDao> taskExecutionDaoFactoryBean;

	private boolean initialized = false;

	private int maxExitMessageSize = MAX_EXIT_MESSAGE_SIZE;

	private int maxTaskNameSize = MAX_TASK_NAME_SIZE;

	private int maxErrorMessageSize = MAX_ERROR_MESSAGE_SIZE;

	public SimpleTaskRepository(FactoryBean<TaskExecutionDao> taskExecutionDaoFactoryBean){
		Assert.notNull(taskExecutionDaoFactoryBean, "A FactoryBean that provides a TaskExecutionDao is required");

		this.taskExecutionDaoFactoryBean = taskExecutionDaoFactoryBean;
	}

	public SimpleTaskRepository(FactoryBean<TaskExecutionDao> taskExecutionDaoFactoryBean, Integer maxExitMessageSize,
			Integer maxTaskNameSize, Integer maxErrorMessageSize){
		Assert.notNull(taskExecutionDaoFactoryBean, "A FactoryBean that provides a TaskExecutionDao is required");
		if(maxTaskNameSize != null) {
			this.maxTaskNameSize = maxTaskNameSize;
		}
		if(maxExitMessageSize != null) {
			this.maxExitMessageSize = maxExitMessageSize;
		}
		if(maxErrorMessageSize != null) {
			this.maxErrorMessageSize = maxErrorMessageSize;
		}
		this.taskExecutionDaoFactoryBean = taskExecutionDaoFactoryBean;
	}

	@Override
	public TaskExecution completeTaskExecution(long executionId, Integer exitCode, Date endTime, String exitMessage) {
		return completeTaskExecution(executionId, exitCode, endTime, exitMessage, null);
	}

	@Override
	public TaskExecution completeTaskExecution(long executionId, Integer exitCode, Date endTime,
			String exitMessage, String errorMessage) {
		initialize();

		validateExitInformation(executionId, exitCode, endTime);
		exitMessage = trimMessage(exitMessage, this.maxExitMessageSize);
		errorMessage = trimMessage(errorMessage, this.maxErrorMessageSize);
		taskExecutionDao.completeTaskExecution(executionId, exitCode, endTime, exitMessage, errorMessage);
		logger.debug("Updating: TaskExecution with executionId="+executionId
				+ " with the following {"
				+ "exitCode=" + exitCode
				+ ", endTime=" + endTime
				+ ", exitMessage='" + exitMessage + '\''
				+ ", errorMessage='" + errorMessage + '\''
				+ '}');

		return taskExecutionDao.getTaskExecution(executionId);
	}

	@Override
	public TaskExecution createTaskExecution(TaskExecution taskExecution) {
		initialize();
		validateCreateInformation(taskExecution);
		TaskExecution daoTaskExecution =
				taskExecutionDao.createTaskExecution(
						taskExecution.getTaskName(),
						taskExecution.getStartTime(),
						taskExecution.getArguments(),
						taskExecution.getExternalExecutionId(),
						taskExecution.getParentExecutionId());
		logger.debug("Creating: " + taskExecution.toString());
		return daoTaskExecution;
	}

	@Override
	public TaskExecution createTaskExecution(String name) {
		initialize();
		TaskExecution taskExecution =
				taskExecutionDao.createTaskExecution(name, null,
						Collections.<String>emptyList(), null);
		logger.debug("Creating: " + taskExecution.toString());
		return taskExecution;
	}

	@Override
	public TaskExecution createTaskExecution() {
		return createTaskExecution((String)null);
	}

	@Override
	public TaskExecution startTaskExecution(long executionid, String taskName, Date startTime, List<String> arguments,
			String externalExecutionId) {
		return startTaskExecution(executionid, taskName, startTime, arguments,
				externalExecutionId, null);
	}

	@Override
	public void updateExternalExecutionId(long executionid, String externalExecutionId) {
		initialize();
		taskExecutionDao.updateExternalExecutionId(executionid, externalExecutionId);
	}

	@Override
	public TaskExecution startTaskExecution(long executionid, String taskName,
			Date startTime, List<String> arguments, String externalExecutionId,
			Long parentExecutionId) {
		initialize();
		TaskExecution taskExecution =
				taskExecutionDao.startTaskExecution(executionid, taskName,
						startTime, arguments, externalExecutionId,
						parentExecutionId);
		logger.debug("Starting: " + taskExecution.toString());
		return taskExecution;
	}

	/**
	 * Retrieves the taskExecutionDao associated with this repository.
	 * @return the taskExecutionDao
	 */
	public TaskExecutionDao getTaskExecutionDao() {
		initialize();
		return taskExecutionDao;
	}

	private void initialize() {
		if(!initialized) {
			try {
				this.taskExecutionDao = this.taskExecutionDaoFactoryBean.getObject();
				this.initialized = true;
			}
			catch (Exception e) {
				throw new IllegalStateException("Unable to create the TaskExecutionDao", e);
			}
		}
	}

	/**
	 * Validate startTime and taskName are valid.
	 */
	private void validateCreateInformation(TaskExecution taskExecution) {
		Assert.notNull(taskExecution.getStartTime(), "TaskExecution start time cannot be null.");

		if (taskExecution.getTaskName() != null &&
				taskExecution.getTaskName().length() > this.maxTaskNameSize) {
			throw new IllegalArgumentException("TaskName length exceeds "
					+ this.maxTaskNameSize + " characters");
		}
	}

	private void validateExitInformation(long executionId, Integer exitCode,  Date endTime){
		Assert.notNull(exitCode, "exitCode should not be null");
		Assert.isTrue(exitCode >= 0, "exit code must be greater than or equal to zero");
		Assert.notNull(endTime, "TaskExecution endTime cannot be null.");
	}

	private String trimMessage(String exitMessage, int maxSize){
		String result = exitMessage;
		if(exitMessage != null &&
				exitMessage.length() > maxSize) {
			result = exitMessage.substring(0, maxSize);
		}
		return result;
	}

	public void setMaxExitMessageSize(int maxExitMessageSize) {
		this.maxExitMessageSize = maxExitMessageSize;
	}

	public void setMaxTaskNameSize(int maxTaskNameSize) {
		this.maxTaskNameSize = maxTaskNameSize;
	}

	public void setMaxErrorMessageSize(int maxErrorMessageSize) {
		this.maxErrorMessageSize = maxErrorMessageSize;
	}
}
