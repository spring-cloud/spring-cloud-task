/*
 * Copyright 2019-2022 the original author or authors.
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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.cloud.task.repository.TaskExecution;

/**
 * Utility class for publishing Spring Cloud Task specific metrics via Micrometer.
 * Intended for internal use only.
 *
 * @author Christian Tzolov
 * @author Glenn Renfro
 * @since 2.2
 */
public class TaskMetrics implements Observation.KeyValuesProviderAware<TaskExecutionKeyValuesProvider> {
	private ObservationRegistry observationRegistry;

	public TaskMetrics(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
	}

	/**
	 * Successful task execution status indicator.
	 */
	public static final String STATUS_SUCCESS = "success";

	/**
	 * Failing task execution status indicator.
	 */
	public static final String STATUS_FAILURE = "failure";

	private Observation.Scope scope;

	private TaskExecutionKeyValuesProvider tagsProvider = new DefaultTaskExecutionKeyValuesProvider();

	private TaskExecutionObservationContext taskObservationContext;

	public void onTaskStartup(TaskExecution taskExecution) {
		this.taskObservationContext = new TaskExecutionObservationContext(taskExecution);
		Observation observation = Observation.createNotStarted(TaskExecutionMeters.TASK_ACTIVE.getPrefix(), this.taskObservationContext, this.observationRegistry)
			.contextualName(String.valueOf(taskExecution.getExecutionId()))
			.keyValuesProvider(this.tagsProvider)
			.lowCardinalityKeyValue(TaskExecutionMeters.TaskTags.TASK_NAME_TAG.getKeyName(), taskExecution.getTaskName())
			.lowCardinalityKeyValue(TaskExecutionMeters.TaskTags.TASK_EXECUTION_ID_TAG.getKeyName(), "" + taskExecution.getExecutionId())
			.lowCardinalityKeyValue(TaskExecutionMeters.TaskTags.TASK_PARENT_EXECUTION_ID_TAG.getKeyName(),
				"" + taskExecution.getParentExecutionId()).start();

		this.scope = observation.openScope();
	}

	public void onTaskFailed(Throwable throwable) {
			this.taskObservationContext.setExceptionMessage(throwable.getClass().getSimpleName());
			this.taskObservationContext.setStatus(STATUS_FAILURE);
	}

	public void onTaskEnd(TaskExecution taskExecution) {
		if (this.scope != null) {
			this.taskObservationContext.getTaskExecution().setExitCode(taskExecution.getExitCode());
			this.scope.getCurrentObservation().stop();
			this.scope.close();
		}
	}

	@Override
	public void setKeyValuesProvider(TaskExecutionKeyValuesProvider tagsProvider) {
		this.tagsProvider = tagsProvider;
	}
}
