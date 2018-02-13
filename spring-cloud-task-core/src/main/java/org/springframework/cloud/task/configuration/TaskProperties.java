/*
 *  Copyright 2016-2018 the original author or authors.
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
	private Boolean closecontextEnabled = false;

	/**
	 * When set to true it
	 * will check to see if a task execution with the same task name is already
	 * running.  If a task is still running then it will throw a
	 * {@link org.springframework.cloud.task.listener.TaskExecutionException}.
	 * When task execution ends the lock is released.
	 */
	private boolean singleInstanceEnabled = false;

	/**
	 * Declares the maximum amount of time (in millis) that a task execution can
	 * hold a lock to prevent another task from executing with a specific task
	 * name when the singleInstanceEnabled is set to true. Default time is: Integer.MAX_VALUE.
	 */
	private int singleInstanceLockTtl = Integer.MAX_VALUE;

	/**
	 * Declares the  time (in millis) that a task execution will wait between
	 * checks. Default time is: 500 millis.
	 */
	private int singleInstanceLockCheckInterval = 500;

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

	public boolean getSingleInstanceEnabled() {
		return singleInstanceEnabled;
	}

	public void setSingleInstanceEnabled(boolean singleInstanceEnabled) {
		this.singleInstanceEnabled = singleInstanceEnabled;
	}

	public int getSingleInstanceLockTtl() {
		return singleInstanceLockTtl;
	}

	public void setSingleInstanceLockTtl(int singleInstanceLockTtl) {
		this.singleInstanceLockTtl = singleInstanceLockTtl;
	}

	public int getSingleInstanceLockCheckInterval() {
		return singleInstanceLockCheckInterval;
	}

	public void setSingleInstanceLockCheckInterval(int singleInstanceLockCheckInterval) {
		this.singleInstanceLockCheckInterval = singleInstanceLockCheckInterval;
	}
}
