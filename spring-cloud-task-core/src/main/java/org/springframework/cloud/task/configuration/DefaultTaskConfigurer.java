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
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.MapTaskExecutionDao;
import org.springframework.cloud.task.repository.support.JdbcTaskRepositoryFactoryBean;
import org.springframework.cloud.task.repository.support.MapTaskRepositoryFactoryBean;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.util.Assert;

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
 */
public class DefaultTaskConfigurer implements TaskConfigurer {

	private DataSource dataSource;

	private TaskRepository taskRepository;

	public DefaultTaskConfigurer(){
		initialize();
	}

	public DefaultTaskConfigurer(DataSource dataSource) {
		this.dataSource = dataSource;
		initialize();
	}

	public TaskRepository getTaskRepository() {
		Assert.notNull(taskRepository, "taskRepository should not be null");
		return taskRepository;
	}

	public TaskExplorer getTaskExplorer() {
		return null;
		//TODO if datasource != null use TaskRepositoryFactoryBean from above like initialize method in DefaultBatchConfigurer
	}

	private void initialize(){
		if (dataSource == null) {
			MapTaskRepositoryFactoryBean mapTaskRepositoryFactoryBean =
					new MapTaskRepositoryFactoryBean();
			taskRepository = mapTaskRepositoryFactoryBean.getObject();
		}
		else {
			JdbcTaskRepositoryFactoryBean jdbcTaskRepositoryFactoryBean =
					new JdbcTaskRepositoryFactoryBean(dataSource);
			taskRepository = jdbcTaskRepositoryFactoryBean.getObject();
		}
	}

}
