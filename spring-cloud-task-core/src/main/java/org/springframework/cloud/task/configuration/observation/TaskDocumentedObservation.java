/*
 * Copyright 2013-2022 the original author or authors.
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

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.docs.DocumentedObservation;

enum TaskDocumentedObservation implements DocumentedObservation {

	/**
	 * Observation created when a task runner is executed.
	 */
	TASK_RUNNER_OBSERVATION {
		@Override
		public String getName() {
			return "spring.cloud.task.runner";
		}

		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return TaskRunnerTags.values();
		}

		@Override
		public String getPrefix() {
			return "spring.cloud.task";
		}
	},

	/**
	 * Observation created within the lifecycle of a task.
	 */
	TASK_EXECUTION_LISTENER_OBSERVATION {
		@Override
		public String getName() {
			return "spring.cloud.task.execution";
		}


		@Override
		public String getPrefix() {
			return "spring.cloud.task";
		}
	};

	/**
	 * Key names for Spring Cloud Task Command / Application runners.
	 */
	enum TaskRunnerTags implements KeyName {

		/**
		 * Name of the bean that was executed by Spring Cloud Task.
		 */
		BEAN_NAME {
			@Override
			public String getKeyName() {
				return "spring.cloud.task.runner.bean-name";
			}
		}
	}
}
