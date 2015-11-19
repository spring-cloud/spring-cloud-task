/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.repository;

import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents the state of the Task for each execution
 *
 * @author Glenn Renfro
 */
@AllArgsConstructor
@NoArgsConstructor
@ToString
public
@Data
class TaskExecution {

	/**
	 * The unique id  associated with the task execution
	 */
	private String executionId;

	/**
	 * The recorded exit code for the task
	 */
	private int exitCode;

	/**
	 * User defined name for the task
	 */
	private String taskName;

	/**
	 * Time of when the task was started
	 */
	private Date startTime;

	/**
	 * Timestamp of when the task was completed/terminated
	 */
	private Date endTime;

	/**
	 * TBD
	 */
	private String statusCode;

	/**
	 * Message returned from the task or stacktrace.parameters
	 */
	private String exitMessage;

	/**
	 * The parameters that were used for this task execution
	 */
	private List<String> parameters;

}
