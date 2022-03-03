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
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.docs.DocumentedMeter;

/**
 * Enumeration for task execution metrics.
 *
 * @author Glenn Renfro
 * @since 3.0.0
 */
public enum TaskExecutionMeters implements DocumentedMeter {
	/**
	 * Metrics created around a task execution.
	 */
	TASK_ACTIVE {
		@Override
		public String getName() {
			return "spring.cloud.task.active";
		}

		@Override
		public Meter.Type getType() {
			return Meter.Type.LONG_TASK_TIMER;
		}


		@Override
		public String getPrefix() {
			return "spring.cloud.task";
		}
	};

	public enum TaskTags implements KeyName {

		/**
		 * Task name measurement tag.
		 */
		TASK_NAME_TAG {
			@Override
			public String getKeyName() {
				return "spring.cloud.task.name";
			}
		},

		/**
		 * Task execution id tag.
		 */
		TASK_EXECUTION_ID_TAG {
			@Override
			public String getKeyName() {
				return "spring.cloud.task.execution.id";
			}
		},

		/**
		 * Task parent execution id tag.
		 */
		TASK_PARENT_EXECUTION_ID_TAG {
			@Override
			public String getKeyName() {
				return "spring.cloud.task.parent.execution.id";
			}
		},

		/**
		 * Task exit code tag.
		 */
		TASK_EXIT_CODE_TAG {
			@Override
			public String getKeyName() {
				return "spring.cloud.task.exit.code";
			}
		},

		/**
		 * Task exception tag. Contains the name of the exception class in case of error or
		 * none otherwise.
		 */
		TASK_EXCEPTION_TAG {
			@Override
			public String getKeyName() {
				return "spring.cloud.task.exception";
			}
		},

		/**
		 * task status tag. Can be either success or failure.
		 */
		TASK_STATUS_TAG {
			@Override
			public String getKeyName() {
				return "spring.cloud.task.status";
			}
		}

	}

}
