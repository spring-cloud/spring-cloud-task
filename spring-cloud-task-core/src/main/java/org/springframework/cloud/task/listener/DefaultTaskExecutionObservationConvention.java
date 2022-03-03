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
 * Default {@link TaskExecutionObservationConvention} implementation.
 *
 * @author Glenn Renfro
 * @since 3.0.0
 */
public class DefaultTaskExecutionObservationConvention implements TaskExecutionObservationConvention {

	@Override
	public KeyValues getLowCardinalityKeyValues(TaskExecutionObservationContext context) {
		return getKeyValuesForTaskExecution(context);
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(TaskExecutionObservationContext context) {
		return KeyValues.empty();
	}

	private KeyValues getKeyValuesForTaskExecution(TaskExecutionObservationContext context) {
		TaskExecution execution = context.getTaskExecution();
		return KeyValues.of(
			TaskExecutionObservation.TaskKeyValues.TASK_STATUS.getKeyName(), context.getStatus(),
			TaskExecutionObservation.TaskKeyValues.TASK_EXIT_CODE.getKeyName(), String.valueOf(execution.getExitCode()),
			TaskExecutionObservation.TaskKeyValues.TASK_EXECUTION_ID.getKeyName(),
			String.valueOf(execution.getExecutionId()));
	}

	@Override
	public String getName() {
		return "spring.cloud.task";
	}
}
