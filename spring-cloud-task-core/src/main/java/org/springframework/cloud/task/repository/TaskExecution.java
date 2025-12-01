/*
 * Copyright 2015-present the original author or authors.
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

package org.springframework.cloud.task.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Represents the state of the Task for each execution.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 * @author Ilayaperumal Gopinathan
 */

public class TaskExecution {

	/**
	 * The unique id associated with the task execution.
	 */
	private long executionId;

	/**
	 * The parent task execution id.
	 */
	private @Nullable Long parentExecutionId;

	/**
	 * The recorded exit code for the task.
	 */
	private @Nullable Integer exitCode;

	/**
	 * User defined name for the task.
	 */
	private @Nullable String taskName;

	/**
	 * Time of when the task was started.
	 */
	private @Nullable LocalDateTime startTime;

	/**
	 * Timestamp of when the task was completed/terminated.
	 */
	private @Nullable LocalDateTime endTime;

	/**
	 * Message returned from the task or stacktrace.
	 */
	private @Nullable String exitMessage;

	/**
	 * Id assigned to the task by the platform.
	 *
	 * @since 1.1.0
	 */
	private @Nullable String externalExecutionId;

	/**
	 * Error information available upon the failure of a task.
	 *
	 * @since 1.1.0
	 */
	private @Nullable String errorMessage;

	/**
	 * The arguments that were used for this task execution.
	 */
	private List<String> arguments;

	public TaskExecution() {
		this.arguments = new ArrayList<>();
	}

	public TaskExecution(long executionId, @Nullable Integer exitCode, @Nullable String taskName,
			@Nullable LocalDateTime startTime, @Nullable LocalDateTime endTime, @Nullable String exitMessage,
			List<String> arguments, @Nullable String errorMessage, @Nullable String externalExecutionId,
			@Nullable Long parentExecutionId) {

		Assert.notNull(arguments, "arguments must not be null");
		this.executionId = executionId;
		this.exitCode = exitCode;
		this.taskName = taskName;
		this.exitMessage = exitMessage;
		this.arguments = new ArrayList<>(arguments);
		this.startTime = startTime;
		this.endTime = endTime;
		this.errorMessage = errorMessage;
		this.externalExecutionId = externalExecutionId;
		this.parentExecutionId = parentExecutionId;
	}

	public TaskExecution(long executionId, @Nullable Integer exitCode, @Nullable String taskName,
			@Nullable LocalDateTime startTime, @Nullable LocalDateTime endTime, @Nullable String exitMessage,
			List<String> arguments, @Nullable String errorMessage, @Nullable String externalExecutionId) {

		this(executionId, exitCode, taskName, startTime, endTime, exitMessage, arguments, errorMessage,
				externalExecutionId, null);
	}

	public long getExecutionId() {
		return this.executionId;
	}

	public @Nullable Integer getExitCode() {
		return this.exitCode;
	}

	public void setExitCode(@Nullable Integer exitCode) {
		this.exitCode = exitCode;
	}

	public @Nullable String getTaskName() {
		return this.taskName;
	}

	public void setTaskName(@Nullable String taskName) {
		this.taskName = taskName;
	}

	public @Nullable LocalDateTime getStartTime() {
		return this.startTime;
	}

	public void setStartTime(@Nullable LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public @Nullable LocalDateTime getEndTime() {
		return this.endTime;
	}

	public void setEndTime(@Nullable LocalDateTime endTime) {
		this.endTime = endTime;
	}

	public @Nullable String getExitMessage() {
		return this.exitMessage;
	}

	public void setExitMessage(@Nullable String exitMessage) {
		this.exitMessage = exitMessage;
	}

	public List<String> getArguments() {
		return this.arguments;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = new ArrayList<>(arguments);
	}

	public @Nullable String getErrorMessage() {
		return this.errorMessage;
	}

	public void setErrorMessage(@Nullable String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public @Nullable String getExternalExecutionId() {
		return this.externalExecutionId;
	}

	public void setExternalExecutionId(@Nullable String externalExecutionId) {
		this.externalExecutionId = externalExecutionId;
	}

	public @Nullable Long getParentExecutionId() {
		return this.parentExecutionId;
	}

	public void setParentExecutionId(@Nullable Long parentExecutionId) {
		this.parentExecutionId = parentExecutionId;
	}

	@Override
	public String toString() {
		return "TaskExecution{" + "executionId=" + this.executionId + ", parentExecutionId=" + this.parentExecutionId
				+ ", exitCode=" + this.exitCode + ", taskName='" + this.taskName + '\'' + ", startTime="
				+ this.startTime + ", endTime=" + this.endTime + ", exitMessage='" + this.exitMessage + '\''
				+ ", externalExecutionId='" + this.externalExecutionId + '\'' + ", errorMessage='" + this.errorMessage
				+ '\'' + ", arguments=" + this.arguments + '}';
	}

}
