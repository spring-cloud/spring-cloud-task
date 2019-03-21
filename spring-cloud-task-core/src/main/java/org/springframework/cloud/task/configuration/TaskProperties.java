/*
 *  Copyright 2016-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.configuration;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties available to configure the task.
 *
 * @author Glenn Renfro
 * @author David Turanski
 */

@ConfigurationProperties(prefix = "spring.cloud.task")
public class TaskProperties {
	private static final Log logger = LogFactory.getLog(TaskProperties.class);
	
	public static final String DEFAULT_TABLE_PREFIX = "TASK_";

	/**
	 * An id that can be associated with a task.
	 */
	private String externalExecutionId;

	/**
	 * An id that will be used by the task when updating the task execution.
	 */
	private Long executionid;

	/**
	 * The id of the parent task execution id that launched this task execution.
	 * Defaults to null if task execution had no parent.
	 */
	private Long parentExecutionId;

	/**
	 * The prefix to append to the table names created by Spring Cloud Task.
	 */
	private String tablePrefix = DEFAULT_TABLE_PREFIX;

	/**
	 * When set to true the context is closed at the end of the task.  Else
	 * the context remains open.
	 */
	private Boolean closecontextEnabled = true;

	public String getExternalExecutionId() {
		return externalExecutionId;
	}

	public void setExternalExecutionId(String externalExecutionId) {
		this.externalExecutionId = externalExecutionId;
	}

	public Long getExecutionid() {
		return executionid;
	}

	public void setExecutionid(Long executionid) {
		this.executionid = executionid;
	}

	/**
	 * 
	 * @deprecated use getClosecontextEnabled()
	 * @since 1.2.0
	 */
	@Deprecated
	public Boolean getClosecontextEnable() {
		return closecontextEnabled;
	}


	/**
	 * 
	 * @deprecated use setClosecontextEnabled()
	 * @since 1.2.0
	 */
	@Deprecated
	public void setClosecontextEnable(Boolean closecontextEnable) {
		logger.warn("'closecontextEnable' is deprecated. Use 'closeContextEnabled.'");
		this.closecontextEnabled = closecontextEnable;
	}

	public Boolean getClosecontextEnabled() {
		return closecontextEnabled;
	}

	public void setClosecontextEnabled(Boolean closecontextEnabled) {
		this.closecontextEnabled = closecontextEnabled;
	}

	public String getTablePrefix() {
		return tablePrefix;
	}

	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	public Long getParentExecutionId() {
		return parentExecutionId;
	}

	public void setParentExecutionId(Long parentExecutionId) {
		this.parentExecutionId = parentExecutionId;
	}
}
