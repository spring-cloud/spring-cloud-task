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

import io.micrometer.common.KeyValues;

import org.springframework.cloud.task.repository.TaskExecution;

import static org.springframework.cloud.task.listener.TaskObservations.TASK_EXCEPTION;
import static org.springframework.cloud.task.listener.TaskObservations.TASK_EXECUTION_ID;
import static org.springframework.cloud.task.listener.TaskObservations.TASK_EXIT_CODE;
import static org.springframework.cloud.task.listener.TaskObservations.TASK_EXTERNAL_EXECUTION_ID;
import static org.springframework.cloud.task.listener.TaskObservations.TASK_NAME;
import static org.springframework.cloud.task.listener.TaskObservations.TASK_PARENT_EXECUTION_ID;
import static org.springframework.cloud.task.listener.TaskObservations.TASK_STATUS;
import static org.springframework.cloud.task.listener.TaskObservations.UNKNOWN;

/**
 * /**
 * Default {@link TaskExecutionKeyValuesProvider} implementation.
 *
 * @author Glenn Renfro
 * @since 3.0.0
 */
public class DefaultTaskExecutionKeyValuesProvider implements TaskExecutionKeyValuesProvider {

	@Override
	public KeyValues getLowCardinalityKeyValues(TaskExecutionObservationContext context) {
		TaskExecution execution = context.getTaskExecution();
		return KeyValues.of(TASK_NAME, (execution.getTaskName() != null) ? execution.getTaskName() : UNKNOWN,
			TASK_STATUS, context.getStatus(),
			TASK_PARENT_EXECUTION_ID, String.valueOf((execution.getParentExecutionId() != null) ? execution.getParentExecutionId() : UNKNOWN),
			TASK_EXTERNAL_EXECUTION_ID, String.valueOf((execution.getExternalExecutionId() != null) ? execution.getExternalExecutionId() : UNKNOWN),
			TASK_EXIT_CODE, String.valueOf(execution.getExitCode()),
			TASK_EXCEPTION, context.getExceptionMessage(),
			TASK_EXECUTION_ID, String.valueOf(execution.getExecutionId()));
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(TaskExecutionObservationContext context) {
		TaskExecution execution = context.getTaskExecution();
		return KeyValues.of(TASK_NAME,  (execution.getTaskName() != null) ? execution.getTaskName() : UNKNOWN);
	}
}
