/*
 *  Copyright 2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties available to configure the task.
 *
 * @author Glenn Renfro
 */

@ConfigurationProperties(prefix = "spring.cloud.task")
public class TaskProperties {

	public static final String DEFAULT_TABLE_PREFIX = "TASK_";

	/**
	 * An id that can be associated with a task.
	 */
	private String externalExecutionId;

	/**
	 * An id that will be used by the task when updating the task execution.
	 */
	private Integer executionid;

	/**
	 * The prefix to append to the table names created by Spring Cloud Task.
	 */
	private String tablePrefix = DEFAULT_TABLE_PREFIX;

	/**
	 * When set to true the context is closed at the end of the task.  Else
	 * the context remains open.
	 */
	private Boolean closecontextEnable = true;

	public String getExternalExecutionId() {
		return externalExecutionId;
	}

	public void setExternalExecutionId(String externalExecutionId) {
		this.externalExecutionId = externalExecutionId;
	}

	public Integer getExecutionid() {
		return executionid;
	}

	public void setExecutionid(Integer executionid) {
		this.executionid = executionid;
	}

	public Boolean getClosecontextEnable() {
		return closecontextEnable;
	}

	public void setClosecontextEnable(Boolean closecontextEnable) {
		this.closecontextEnable = closecontextEnable;
	}

	public String getTablePrefix() {
		return tablePrefix;
	}

	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}
}
