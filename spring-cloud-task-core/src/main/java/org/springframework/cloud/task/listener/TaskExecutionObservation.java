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

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.docs.DocumentedObservation;

/**
 * Enumeration for task execution observations.
 *
 * @author Glenn Renfro
 * @since 3.0.0
 */
public enum TaskExecutionObservation implements DocumentedObservation {
	/**
	 * Metrics created around a task execution.
	 */
	TASK_ACTIVE {
		@Override
		public String getName() {
			return "spring.cloud.task";
		}

		@Override
		public String getPrefix() {
			return "spring.cloud.task";
		}
	};
	@Override
	public KeyName[] getLowCardinalityKeyNames() {
		return TaskKeyValues.values();
	}

	public enum TaskKeyValues implements KeyName {

		/**
		 * Task name measurement.
		 */
		TASK_NAME {
			@Override
			public String getKeyName() {
				return "spring.cloud.task.name";
			}
		},

		/**
		 * Task execution id.
		 */
		TASK_EXECUTION_ID {
			@Override
			public String getKeyName() {
				return "spring.cloud.task.execution.id";
			}
		},

		/**
		 * Task parent execution id.
		 */
		TASK_PARENT_EXECUTION_ID {
			@Override
			public String getKeyName() {
				return "spring.cloud.task.parent.execution.id";
			}
		},

		/**
		 * External execution id for task.
		 */
		TASK_EXTERNAL_EXECUTION_ID {
			@Override
			public String getKeyName() {
				return "spring.cloud.task.external.execution.id";
			}
		},
		/**
		 * Task exit code.
		 */
		TASK_EXIT_CODE {
			@Override
			public String getKeyName() {
				return "spring.cloud.task.exit.code";
			}
		},

		/**
		 * task status. Can be either success or failure.
		 */
		TASK_STATUS {
			@Override
			public String getKeyName() {
				return "spring.cloud.task.status";
			}
		},

		/**
		 * Organization Name for CF cloud.
		 */
		TASK_CF_ORG_NAME {
			@Override
			public String getKeyName() {
				return "cf.org.name";
			}
		},

		/**
		 * Space id for CF cloud.
		 */
		TASK_CF_SPACE_ID {
			@Override
			public String getKeyName() {
				return "cf.space.id";
			}
		},

		/**
		 * Space name for CF cloud.
		 */
		TASK_CF_SPACE_NAME {
			@Override
			public String getKeyName() {
				return "cf.space.name";
			}
		},

		/**
		 * App name for CF cloud.
		 */
		TASK_CF_APP_NAME {
			@Override
			public String getKeyName() {
				return "cf.app.name";
			}
		},

		/**
		 * App id for CF cloud.
		 */
		TASK_CF_APP_ID {
			@Override
			public String getKeyName() {
				return "cf.app.id";
			}
		},

		/**
		 * App version for CF cloud.
		 */
		TASK_CF_APP_VERSION {
			@Override
			public String getKeyName() {
				return "cf.app.version";
			}
		},

		/**
		 * Instance index for CF cloud.
		 */
		TASK_CF_INSTANCE_INDEX {
			@Override
			public String getKeyName() {
				return "cf.instance.index";
			}
		}

	}

}
