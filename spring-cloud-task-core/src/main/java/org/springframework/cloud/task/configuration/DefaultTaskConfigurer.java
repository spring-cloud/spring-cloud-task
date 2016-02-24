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

import javax.sql.DataSource;

import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.MapTaskExecutionDao;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
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

	private TaskRepository taskRepository;

	private TaskExplorer taskExplorer;

	private PlatformTransactionManager transactionManager;

	private ConfigurableApplicationContext context;

	private TaskExecutionDaoFactoryBean taskExecutionDaoFactoryBean;

	public DefaultTaskConfigurer(ConfigurableApplicationContext context) {
		this.context = context;

		this.taskExecutionDaoFactoryBean = new TaskExecutionDaoFactoryBean(this.context);
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
	public PlatformTransactionManager getTransactionManager() {
		if(this.transactionManager == null) {
			if(isDataSourceAvailable()) {
				this.transactionManager = new DataSourceTransactionManager(this.context.getBean(DataSource.class));
			}
			else {
				this.transactionManager = new ResourcelessTransactionManager();
			}
		}

		return this.transactionManager;
	}

	private boolean isDataSourceAvailable() {
		return this.context.getBeanNamesForType(DataSource.class).length == 1;
	}
}
