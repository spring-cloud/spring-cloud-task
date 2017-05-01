/*
 * Copyright 2015-2016 the original author or authors.
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

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.MapTaskExecutionDao;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Default implementation of the TaskConfigurer interface.  If no {@link TaskConfigurer}
 * implementation is present, then this configuration will be used.
 * The following defaults will be used:
 * <ul>
 * <li>{@link SimpleTaskRepository} is the default {@link TaskRepository} returned.
 * If a data source is present then a data will be stored in the database {@link JdbcTaskExecutionDao} else it will
 * be stored in a map {@link MapTaskExecutionDao}.
 * </ul>
 *
 * @author Glenn Renfro
 * @author Michael Minella
 */
public class DefaultTaskConfigurer implements TaskConfigurer {

	private static final Log logger = LogFactory.getLog(DefaultTaskConfigurer.class);

	private TaskRepository taskRepository;

	private TaskExplorer taskExplorer;

	private PlatformTransactionManager transactionManager;

	private TaskExecutionDaoFactoryBean taskExecutionDaoFactoryBean;

	private DataSource dataSource;

	private ApplicationContext context;

	public DefaultTaskConfigurer() {
		this(TaskProperties.DEFAULT_TABLE_PREFIX);
	}

	/**
	 * Initializes the DefaultTaskConfigurer and sets the default table prefix
	 * to {@link TaskProperties#DEFAULT_TABLE_PREFIX}.
	 *
	 * @param dataSource references the {@link DataSource} to be used as the Task
	 * repository.  If none is provided, a Map will be used (not recommended for
	 * production use.
	 */
	public DefaultTaskConfigurer(DataSource dataSource) {
		this(dataSource, TaskProperties.DEFAULT_TABLE_PREFIX, null);
	}

	/**
	 * Initializes the DefaultTaskConfigurer.
	 *
	 * @param tablePrefix the prefix to apply to the task table names used by
	 * task infrastructure.
	 */
	public DefaultTaskConfigurer(String tablePrefix) {
		this(null, tablePrefix, null);
	}

	/**
	 * Initializes the DefaultTaskConfigurer.
	 *
	 * @param dataSource references the {@link DataSource} to be used as the Task
	 * repository.  If none is provided, a Map will be used (not recommended for
	 * production use.
	 * @param tablePrefix the prefix to apply to the task table names used by
	 * task infrastructure.
	 */
	public DefaultTaskConfigurer(DataSource dataSource, String tablePrefix, ApplicationContext context) {
		this.dataSource = dataSource;
		this.context = context;

		if(this.dataSource != null) {
			this.taskExecutionDaoFactoryBean = new
					TaskExecutionDaoFactoryBean(this.dataSource, tablePrefix);
		}
		else {
			this.taskExecutionDaoFactoryBean = new TaskExecutionDaoFactoryBean();
		}
		this.taskRepository = new SimpleTaskRepository(this.taskExecutionDaoFactoryBean);
		this.taskExplorer = new SimpleTaskExplorer(this.taskExecutionDaoFactoryBean);
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
	public DataSource getTaskDataSource() {
		return this.dataSource;
	}

	@Override
	public PlatformTransactionManager getTransactionManager() {
		if (this.transactionManager == null) {
			if (isDataSourceAvailable()) {
				try {
					Class.forName("javax.persistence.EntityManager");
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
						this.transactionManager = new DataSourceTransactionManager(this.dataSource);
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
