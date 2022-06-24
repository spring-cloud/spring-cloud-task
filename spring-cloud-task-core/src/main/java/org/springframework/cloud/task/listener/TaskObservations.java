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
public class TaskObservations implements Observation.KeyValuesProviderAware<TaskExecutionKeyValuesProvider> {
	/**
	 * Task parent execution id.
	 */
	public static final String TASK_PARENT_EXECUTION_ID = "spring.cloud.task.parent.execution.id";

	/**
	 * Task external execution id.
	 */
	public static final String TASK_EXTERNAL_EXECUTION_ID = "spring.cloud.task.external.execution.id";

	/**
	 * Task exit code.
	 */
	public static final String TASK_EXIT_CODE = "spring.cloud.task.exit.code";

	/**
	 * Task exception. Contains the name of the exception class in case of error or
	 * none otherwise.
	 */
	public static final String TASK_EXCEPTION = "spring.cloud.task.exception";

	/**
	 * Task status. Can be either success or failure.
	 */
	public static final String TASK_STATUS = "spring.cloud.task.status";

	/**
	 * Task execution id.
	 */
	public static final String TASK_EXECUTION_ID = "spring.cloud.task.execution.id";

	/**
	 * Prefix for task metric keys.
	 */
	public static final String TASK_PREFIX = "spring.cloud.task";

	/**
	 * Task name measurement.
	 */
	public static final String TASK_NAME = "spring.cloud.task.name";

	/**
	 * Name for the Active {@link io.micrometer.core.instrument.LongTaskTimer}.
	 */
	public static final String TASK_ACTIVE_NAME = "spring.cloud.task.active";

	/**
	 * Organization Name for CF cloud.
	 */
	public static final String TASK_CF_ORG_NAME = "cf.org.name";
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

	public TaskObservations(ObservationRegistry observationRegistry, TaskObservationCloudKeyValues taskObservationCloudKeyValues) {
		this.observationRegistry = observationRegistry;
		this.taskObservationCloudKeyValues = taskObservationCloudKeyValues;
	}

	private Observation.Scope scope;

	private TaskExecutionKeyValuesProvider tagsProvider = new DefaultTaskExecutionKeyValuesProvider();

	private TaskExecutionObservationContext taskObservationContext;

	TaskObservationCloudKeyValues taskObservationCloudKeyValues;

	public void onTaskStartup(TaskExecution taskExecution) {


		this.taskObservationContext = new TaskExecutionObservationContext(taskExecution);
		String s = (taskExecution.getParentExecutionId() != null) ? "" + taskExecution.getParentExecutionId() : UNKNOWN;

		Observation observation = Observation.createNotStarted(TASK_PREFIX, this.taskObservationContext, this.observationRegistry)
			.contextualName(String.valueOf(taskExecution.getExecutionId()))
			.keyValuesProvider(this.tagsProvider)
			.lowCardinalityKeyValue(TASK_NAME, (taskExecution.getTaskName() != null) ? taskExecution.getTaskName() : UNKNOWN)
			.lowCardinalityKeyValue(TASK_EXECUTION_ID, "" + taskExecution.getExecutionId())
			.lowCardinalityKeyValue(TASK_PARENT_EXECUTION_ID,
				((taskExecution.getParentExecutionId() != null) ? "" + taskExecution.getParentExecutionId() : UNKNOWN));
//			.lowCardinalityKeyValue(TASK_EXTERNAL_EXECUTION_ID,
//				((taskExecution.getExternalExecutionId() != null) ? taskExecution.getExternalExecutionId() : UNKNOWN));

		if (taskObservationCloudKeyValues != null) {
			observation.lowCardinalityKeyValue(TASK_CF_ORG_NAME, this.taskObservationCloudKeyValues.getOrganizationName());
			observation.lowCardinalityKeyValue("cf.space.id", this.taskObservationCloudKeyValues.getSpaceId());
			observation.lowCardinalityKeyValue("cf.space.name", this.taskObservationCloudKeyValues.getSpaceName());
			observation.lowCardinalityKeyValue("cf.app.id", this.taskObservationCloudKeyValues.getApplicationId());
			observation.lowCardinalityKeyValue("cf.app.name", this.taskObservationCloudKeyValues.getApplicationName());
			observation.lowCardinalityKeyValue("cf.app.version", this.taskObservationCloudKeyValues.getApplicationVersion());
			observation.lowCardinalityKeyValue("cf.instance.index", this.taskObservationCloudKeyValues.getInstanceIndex());
		}
		observation.start();

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
