/*
 * Copyright 2019-present the original author or authors.
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
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.Nullable;

import org.springframework.cloud.task.configuration.TaskObservationCloudKeyValues;
import org.springframework.cloud.task.repository.TaskExecution;

/**
 * Utility class for publishing Spring Cloud Task specific Observations via Micrometer.
 * Intended for internal use only.
 *
 * @author Christian Tzolov
 * @author Glenn Renfro
 * @since 2.2
 */
public class TaskObservations {

	/**
	 * Successful task execution status indicator.
	 */
	public static final String STATUS_SUCCESS = "success";

	/**
	 * Failing task execution status indicator.
	 */
	public static final String STATUS_FAILURE = "failure";

	/**
	 * Default for when value is not present.
	 */
	public static final String UNKNOWN = "unknown";

	private ObservationRegistry observationRegistry;

	private ObservationConvention customObservationConvention;

	public TaskObservations(ObservationRegistry observationRegistry,
			TaskObservationCloudKeyValues taskObservationCloudKeyValues,
			ObservationConvention customObservationConvention) {
		this.observationRegistry = observationRegistry;
		this.taskObservationCloudKeyValues = taskObservationCloudKeyValues;
		this.customObservationConvention = customObservationConvention;
	}

	@SuppressWarnings("NullAway.Init")
	private Observation.Scope scope;

	@SuppressWarnings("NullAway.Init")
	private TaskExecutionObservationConvention observationsProvider = new DefaultTaskExecutionObservationConvention();

	@SuppressWarnings("NullAway.Init")
	private TaskExecutionObservationContext taskObservationContext;

	@Nullable
	TaskObservationCloudKeyValues taskObservationCloudKeyValues;

	public void onTaskStartup(TaskExecution taskExecution) {

		this.taskObservationContext = new TaskExecutionObservationContext(taskExecution);

		Observation observation = TaskExecutionObservation.TASK_ACTIVE
			.observation(this.customObservationConvention, new DefaultTaskExecutionObservationConvention(),
					this.taskObservationContext, this.observationRegistry)
			.contextualName(String.valueOf(taskExecution.getExecutionId()))
			.observationConvention(this.observationsProvider)
			.lowCardinalityKeyValue(TaskExecutionObservation.TaskKeyValues.TASK_NAME.asString(),
					getValueOrDefault(taskExecution.getTaskName()))
			.lowCardinalityKeyValue(TaskExecutionObservation.TaskKeyValues.TASK_EXECUTION_ID.asString(),
					"" + taskExecution.getExecutionId())
			.lowCardinalityKeyValue(TaskExecutionObservation.TaskKeyValues.TASK_PARENT_EXECUTION_ID.asString(),
					(getValueOrDefault(taskExecution.getParentExecutionId())))
			.lowCardinalityKeyValue(TaskExecutionObservation.TaskKeyValues.TASK_EXTERNAL_EXECUTION_ID.asString(),
					(getValueOrDefault(taskExecution.getExternalExecutionId())));

		if (taskObservationCloudKeyValues != null) {
			observation.lowCardinalityKeyValue(TaskExecutionObservation.TaskKeyValues.TASK_CF_ORG_NAME.asString(),
					this.taskObservationCloudKeyValues.getOrganizationName());
			observation.lowCardinalityKeyValue(TaskExecutionObservation.TaskKeyValues.TASK_CF_SPACE_ID.asString(),
					this.taskObservationCloudKeyValues.getSpaceId());
			observation.lowCardinalityKeyValue(TaskExecutionObservation.TaskKeyValues.TASK_CF_SPACE_NAME.asString(),
					this.taskObservationCloudKeyValues.getSpaceName());
			observation.lowCardinalityKeyValue(TaskExecutionObservation.TaskKeyValues.TASK_CF_APP_ID.asString(),
					this.taskObservationCloudKeyValues.getApplicationId());
			observation.lowCardinalityKeyValue(TaskExecutionObservation.TaskKeyValues.TASK_CF_APP_NAME.asString(),
					this.taskObservationCloudKeyValues.getApplicationName());
			observation.lowCardinalityKeyValue(TaskExecutionObservation.TaskKeyValues.TASK_CF_APP_VERSION.asString(),
					this.taskObservationCloudKeyValues.getApplicationVersion());
			observation.lowCardinalityKeyValue(TaskExecutionObservation.TaskKeyValues.TASK_CF_INSTANCE_INDEX.asString(),
					this.taskObservationCloudKeyValues.getInstanceIndex());
		}
		observation.start();

		this.scope = observation.openScope();
	}

	private String getValueOrDefault(@Nullable Object value) {
		return (value != null) ? value.toString() : UNKNOWN;
	}

	public void onTaskFailed(Throwable throwable) {
		this.taskObservationContext.setStatus(STATUS_FAILURE);
		this.scope.getCurrentObservation().error(throwable);
	}

	public void onTaskEnd(TaskExecution taskExecution) {
		if (this.scope != null) {
			this.taskObservationContext.getTaskExecution().setExitCode(taskExecution.getExitCode());
			this.scope.close();
			this.scope.getCurrentObservation().stop();
		}
	}

}
