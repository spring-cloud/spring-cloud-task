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

package org.springframework.cloud.task.configuration;

import javax.sql.DataSource;

import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Provides a strategy interface for providing configuration
 * customization to the task system.
 *
 * @author Glenn Renfro
 */
public interface TaskConfigurer {

	/**
	 * Create a {@link TaskRepository} for the Task.
	 *
	 * @return A TaskRepository
	 */
	TaskRepository getTaskRepository();

	/**
	 * Create a {@link PlatformTransactionManager} for use with the
	 * <code>TaskRepository</code>.
	 *
	 * @return A <code>PlatformTransactionManager</code>
	 */
	PlatformTransactionManager getTransactionManager();

	/**
	 * Create a {@link TaskExplorer} for the task.
	 *
	 * @return a <code>TaskExplorer</code>
	 */
	TaskExplorer getTaskExplorer();

	/**
	 * Retrieves the DataSource that will be used for task operations.  If a
	 * DataSource is not being used for the implemented TaskConfigurer this
	 * method will return null.
	 */
	DataSource getTaskDataSource();
}
