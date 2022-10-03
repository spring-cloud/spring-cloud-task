/*
 * Copyright 2022-2022 the original author or authors.
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

package org.springframework.cloud.task.listener;

import java.util.function.Supplier;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;

import org.springframework.cloud.task.repository.TaskExecution;

/**
 * A mutable holder of the {@link TaskExecution} required by a {@link ObservationHandler}.
 *
 * @author Glenn Renfro
 * @since 3.0.0
 */
public class TaskExecutionObservationContext extends Observation.Context
		implements Supplier<TaskExecutionObservationContext> {

	private final TaskExecution taskExecution;

	private String exceptionMessage = "none";

	private String status = "success";

	public TaskExecutionObservationContext(TaskExecution taskExecution) {
		this.taskExecution = taskExecution;
	}

	public TaskExecution getTaskExecution() {
		return taskExecution;
	}

	public String getExceptionMessage() {
		return exceptionMessage;
	}

	public void setExceptionMessage(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public TaskExecutionObservationContext get() {
		return this;
	}

}
