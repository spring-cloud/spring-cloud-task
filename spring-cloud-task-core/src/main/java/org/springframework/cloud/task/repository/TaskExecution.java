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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Represents the state of the Task for each execution.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 */

public class TaskExecution {

	/**
	 * The unique id  associated with the task execution.
	 */
	private long executionId;

	/**
	 * The parent task execution id.
	 */
	private Long parentExecutionId;

	/**
	 * The recorded exit code for the task.
	 */
	private Integer exitCode;

	/**
	 * User defined name for the task.
	 */
	private String taskName;

	/**
	 * Time of when the task was started.
	 */
	private Date startTime;

	/**
	 * Timestamp of when the task was completed/terminated.
	 */
	private Date endTime;

	/**
	 * Message returned from the task or stacktrace.
	 */
	private String exitMessage;

	/**
	 * Id assigned to the task by the platform.
	 *
	 * @since 1.1.0
	 */
	private String externalExecutionId;

	/**
	 * Error information available upon the failure of a task
	 *
	 * @since 1.1.0
	 */
	private String errorMessage;

	/**
	 * The arguments that were used for this task execution.
	 */
	private List<String> arguments;

	public TaskExecution() {
		arguments = new ArrayList<>();
	}

	public TaskExecution(long executionId, Integer exitCode, String taskName,
						 Date startTime, Date endTime,
						 String exitMessage, List<String> arguments,
						 String errorMessage, String externalExecutionId,
						 Long parentExecutionId) {

		Assert.notNull(arguments, "arguments must not be null");
		this.executionId = executionId;
		this.exitCode = exitCode;
		this.taskName = taskName;
		this.exitMessage = exitMessage;
		this.arguments = new ArrayList<>(arguments);
		this.startTime = (startTime != null) ? (Date)startTime.clone() : null;
		this.endTime = (endTime != null) ? (Date)endTime.clone() : null;
		this.errorMessage = errorMessage;
		this.externalExecutionId = externalExecutionId;
		this.parentExecutionId = parentExecutionId;
	}

	public TaskExecution(long executionId, Integer exitCode, String taskName,
			Date startTime, Date endTime,
			String exitMessage, List<String> arguments,
			String errorMessage, String externalExecutionId) {

		this(executionId, exitCode, taskName, startTime, endTime, exitMessage,
				arguments, errorMessage,externalExecutionId, null);
	}

	public long getExecutionId() {
		return executionId;
	}

	public Integer getExitCode() {
		return (exitCode == null) ? 0 : exitCode;
	}

	public void setExitCode(Integer exitCode) {
		this.exitCode = exitCode;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public Date getStartTime() {
		return (startTime != null) ? (Date)startTime.clone() : null;
	}

	public void setStartTime(Date startTime) {
		this.startTime = (startTime != null) ? (Date)startTime.clone() : null;
	}

	public Date getEndTime() {
		return (endTime != null) ? (Date)endTime.clone() : null;
	}

	public void setEndTime(Date endTime) {
		this.endTime = (endTime != null) ? (Date)endTime.clone() : null;
	}

	public String getExitMessage() {
		return exitMessage;
	}

	public void setExitMessage(String exitMessage) {
		this.exitMessage = exitMessage;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = new ArrayList<> (arguments);
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getExternalExecutionId() {
		return externalExecutionId;
	}

	public void setExternalExecutionId(String externalExecutionId) {
		this.externalExecutionId = externalExecutionId;
	}

	public Long getParentExecutionId() {
		return parentExecutionId;
	}

	public void setParentExecutionId(Long parentExecutionId) {
		this.parentExecutionId = parentExecutionId;
	}

	@Override
	public String toString() {
		return "TaskExecution{" +
				"executionId=" + executionId +
				", parentExecutionId=" + parentExecutionId +
				", exitCode=" + exitCode +
				", taskName='" + taskName + '\'' +
				", startTime=" + startTime +
				", endTime=" + endTime +
				", exitMessage='" + exitMessage + '\'' +
				", externalExecutionId='" + externalExecutionId + '\'' +
				", errorMessage='" + errorMessage + '\'' +
				", arguments=" + arguments +
				'}';
	}
}
