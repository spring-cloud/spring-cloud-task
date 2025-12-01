/*
 * Copyright 2015-present the original author or authors.
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

package org.springframework.cloud.task.configuration;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.MapTaskExecutionDao;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskNameResolver;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * Default implementation of the TaskConfigurer interface. If no {@link TaskConfigurer}
 * implementation is present, then this configuration will be used. The following defaults
 * will be used:
 * <ul>
 * <li>{@link SimpleTaskRepository} is the default {@link TaskRepository} returned. If a
 * data source is present then a data will be stored in the database
 * {@link JdbcTaskExecutionDao} else it will be stored in a map
 * {@link MapTaskExecutionDao}.
 * </ul>
 *
 * @author Glenn Renfro
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
public class DefaultTaskConfigurer implements TaskConfigurer {

	private static final Log logger = LogFactory.getLog(DefaultTaskConfigurer.class);

	private @Nullable TaskProperties taskProperties;

	private TaskRepository taskRepository;

	private TaskExplorer taskExplorer;

	private @Nullable PlatformTransactionManager transactionManager;

	private @Nullable DataSource dataSource;

	private @Nullable ApplicationContext context;

	public DefaultTaskConfigurer() {
		this(TaskProperties.DEFAULT_TABLE_PREFIX);
	}

	/**
	 * Initializes the DefaultTaskConfigurer and retrieves table prefix from
	 * {@link TaskProperties}.
	 */
	public DefaultTaskConfigurer(TaskProperties taskProperties) {
		this(null, null, null, taskProperties);
	}

	/**
	 * Initializes the DefaultTaskConfigurer and sets the default table prefix to
	 * {@link TaskProperties#DEFAULT_TABLE_PREFIX}.
	 * @param dataSource references the {@link DataSource} to be used as the Task
	 * repository. If none is provided, a Map will be used (not recommended for production
	 * use).
	 */
	public DefaultTaskConfigurer(@Nullable DataSource dataSource) {
		this(dataSource, TaskProperties.DEFAULT_TABLE_PREFIX, null);
	}

	/**
	 * Initializes the DefaultTaskConfigurer and retrieves table prefix from *
	 * {@link TaskProperties}.
	 * @param dataSource references the {@link DataSource} to be used as the Task
	 * repository. If none is provided, a Map will be used (not recommended for production
	 * use).
	 * @param taskProperties the task properties used to obtain tablePrefix if not set by
	 * tablePrefix field.
	 */
	public DefaultTaskConfigurer(@Nullable DataSource dataSource, TaskProperties taskProperties) {
		this(dataSource, null, null, taskProperties);
	}

	/**
	 * Initializes the DefaultTaskConfigurer.
	 * @param tablePrefix the prefix to apply to the task table names used by task
	 * infrastructure.
	 */
	public DefaultTaskConfigurer(@Nullable String tablePrefix) {
		this(null, tablePrefix, null);
	}

	/**
	 * Initializes the DefaultTaskConfigurer.
	 * @param tablePrefix the prefix to apply to the task table names used by task
	 * infrastructure.
	 * @param taskProperties the task properties used to obtain tablePrefix if not set by
	 * tablePrefix field.
	 */
	public DefaultTaskConfigurer(@Nullable String tablePrefix, TaskProperties taskProperties) {
		this(null, tablePrefix, null, taskProperties);
	}

	/**
	 * Initializes the DefaultTaskConfigurer.
	 * @param dataSource references the {@link DataSource} to be used as the Task
	 * repository. If none is provided, a Map will be used (not recommended for production
	 * use).
	 * @param tablePrefix the prefix to apply to the task table names used by task
	 * infrastructure.
	 * @param context the context to be used.
	 */
	public DefaultTaskConfigurer(@Nullable DataSource dataSource, @Nullable String tablePrefix,
			@Nullable ApplicationContext context) {
		this(dataSource, tablePrefix, context, null);
	}

	/**
	 * Initializes the DefaultTaskConfigurer.
	 * @param dataSource references the {@link DataSource} to be used as the Task
	 * repository. If none is provided, a Map will be used (not recommended for production
	 * use).
	 * @param tablePrefix the prefix to apply to the task table names used by task
	 * infrastructure.
	 * @param context the context to be used.
	 * @param taskProperties the task properties used to obtain tablePrefix if not set by
	 * tablePrefix field.
	 */
	public DefaultTaskConfigurer(@Nullable DataSource dataSource, @Nullable String tablePrefix,
			@Nullable ApplicationContext context, @Nullable TaskProperties taskProperties) {
		this.dataSource = dataSource;
		this.context = context;

		TaskExecutionDaoFactoryBean taskExecutionDaoFactoryBean;
		this.taskProperties = taskProperties;

		if (tablePrefix == null) {
			tablePrefix = (taskProperties != null && !taskProperties.getTablePrefix().isEmpty())
					? taskProperties.getTablePrefix() : TaskProperties.DEFAULT_TABLE_PREFIX;
		}

		if (this.dataSource != null) {
			taskExecutionDaoFactoryBean = new TaskExecutionDaoFactoryBean(this.dataSource, tablePrefix);
		}
		else {
			taskExecutionDaoFactoryBean = new TaskExecutionDaoFactoryBean();
		}

		this.taskRepository = new SimpleTaskRepository(taskExecutionDaoFactoryBean);
		this.taskExplorer = new SimpleTaskExplorer(taskExecutionDaoFactoryBean);
	}

	@Override
	public TaskRepository getTaskRepository() {
		return this.taskRepository;
	}

	@Override
	public TaskExplorer getTaskExplorer() {
		return this.taskExplorer;
	}

	@Override
	public @Nullable DataSource getTaskDataSource() {
		return this.dataSource;
	}

	@Override
	public TaskNameResolver getTaskNameResolver() {
		return new SimpleTaskNameResolver();
	}

	@Override
	public PlatformTransactionManager getTransactionManager() {
		if (this.transactionManager == null) {
			if (isDataSourceAvailable()) {
				try {
					Class.forName("jakarta.persistence.EntityManager");
					if (this.context != null && this.context.getBeanNamesForType(EntityManager.class).length > 0) {
						logger.debug("EntityManager was found, using JpaTransactionManager");
						this.transactionManager = new JpaTransactionManager();
					}
				}
				catch (ClassNotFoundException ignore) {
					logger.debug("No EntityManager was found, using DataSourceTransactionManager");
				}
				finally {
					if (this.transactionManager == null) {
						Assert.state(this.dataSource != null, "DataSource must be non-null when available");
						this.transactionManager = new JdbcTransactionManager(this.dataSource);
					}
				}
			}
			else {
				logger.debug("No DataSource was found, using ResourcelessTransactionManager");
				this.transactionManager = new ResourcelessTransactionManager();
			}
		}
		return this.transactionManager;
	}

	private boolean isDataSourceAvailable() {
		return this.dataSource != null;
	}

}
