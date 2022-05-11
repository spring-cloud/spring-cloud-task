/*
 * Copyright 2018-2021 the original author or authors.
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

package org.springframework.cloud.task.configuration.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.task.listener.TaskExecutionListener;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.core.Ordered;

/**
 * Sets the span upon starting and closes it upon ending a task.
 *
 * @author Marcin Grzejszczak
 * @since 3.1.0
 */
class ObservationTaskExecutionListener implements TaskExecutionListener, Ordered {

	private static final Log log = LogFactory.getLog(ObservationTaskExecutionListener.class);

	private final ObservationRegistry registry;

	private final String projectName;

	ObservationTaskExecutionListener(ObservationRegistry registry, String projectName) {
		this.registry = registry;
		this.projectName = projectName;
	}

	@Override
	public void onTaskStartup(TaskExecution taskExecution) {
		Observation observation = TaskDocumentedObservation.TASK_EXECUTION_LISTENER_OBSERVATION.observation(this.registry)
			.contextualName(this.projectName)
			.start();
		observation.openScope();
		if (log.isDebugEnabled()) {
			log.debug("Put the observation [" + observation + "] in scope");
		}
	}

	@Override
	public void onTaskEnd(TaskExecution taskExecution) {
		Observation.Scope scope = this.registry.getCurrentObservationScope();
		if (scope != null) {
			scope.close();
			scope.getCurrentObservation().stop();
			if (log.isDebugEnabled()) {
				log.debug("Removed the [" + scope.getCurrentObservation() + "] from thread local");
			}
		}
	}

	@Override
	public void onTaskFailed(TaskExecution taskExecution, Throwable throwable) {
		Observation.Scope scope = this.registry.getCurrentObservationScope();
		if (scope != null) {
			Observation observation = scope.getCurrentObservation();
			observation.error(throwable);
			onTaskEnd(taskExecution);
		}
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
