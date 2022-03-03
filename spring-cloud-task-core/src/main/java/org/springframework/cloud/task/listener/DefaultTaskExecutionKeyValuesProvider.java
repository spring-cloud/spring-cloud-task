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
		return KeyValues.of(TaskExecutionMeters.TaskTags.TASK_NAME_TAG.of(execution.getTaskName()),
			TaskExecutionMeters.TaskTags.TASK_STATUS_TAG.of(context.getStatus()),
			TaskExecutionMeters.TaskTags.TASK_PARENT_EXECUTION_ID_TAG.of(String.valueOf(execution.getParentExecutionId())),
			TaskExecutionMeters.TaskTags.TASK_EXIT_CODE_TAG.of(String.valueOf(execution.getExitCode())),
			TaskExecutionMeters.TaskTags.TASK_EXCEPTION_TAG.of(context.getExceptionMessage()),
			TaskExecutionMeters.TaskTags.TASK_EXECUTION_ID_TAG.of(String.valueOf(execution.getExecutionId())));
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(TaskExecutionObservationContext context) {
		TaskExecution execution = context.getTaskExecution();
		return KeyValues.of(TaskExecutionMeters.TaskTags.TASK_NAME_TAG.of(execution.getTaskName()));
	}
}
