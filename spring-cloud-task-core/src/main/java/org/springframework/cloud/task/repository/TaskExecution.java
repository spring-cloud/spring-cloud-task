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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Represents the state of the Task for each execution.
 *
 * @author Glenn Renfro
 */

public class TaskExecution {
	/**
	 * The unique id  associated with the task execution.
	 */
	private String executionId;

	/**
	 * The recorded exit code for the task.
	 */
	private int exitCode;

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
	 * TBD.
	 */
	private String statusCode;

	/**
	 * Message returned from the task or stacktrace.parameters.
	 */
	private String exitMessage;

	/**
	 * The parameters that were used for this task execution.
	 */
	private List<String> parameters;

	public TaskExecution() {
		parameters = new ArrayList<>();
	}

	public TaskExecution(String executionId, int exitCode, String taskName,
						 Date startTime, Date endTime, String statusCode,
						 String exitMessage, List<String> parameters) {

		Assert.hasText(executionId, "executionId must not be null nor empty");
		Assert.notNull(parameters, "parameters must not be null");
		Assert.notNull(startTime, "startTime must not be null");
		this.executionId = executionId;
		this.exitCode = exitCode;
		this.taskName = taskName;
		this.startTime = startTime;
		this.endTime = endTime;
		this.statusCode = statusCode;
		this.exitMessage = exitMessage;
		this.parameters = parameters;
	}

	public String getExecutionId() {
		return executionId;
	}

	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}

	public int getExitCode() {
		return exitCode;
	}

	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public String getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	public String getExitMessage() {
		return exitMessage;
	}

	public void setExitMessage(String exitMessage) {
		this.exitMessage = exitMessage;
	}

	public List<String> getParameters() {
		return parameters;
	}

	public void setParameters(List<String> parameters) {
		this.parameters = parameters;
	}

	@Override
	public String toString() {
		return "TaskExecution{" +
				"executionId='" + executionId + '\'' +
				", exitCode=" + exitCode +
				", taskName='" + taskName + '\'' +
				", startTime=" + startTime +
				", endTime=" + endTime +
				", statusCode='" + statusCode + '\'' +
				", exitMessage='" + exitMessage + '\'' +
				", parameters=" + parameters +
				'}';
	}
}
