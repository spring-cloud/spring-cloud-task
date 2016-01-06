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

package org.springframework.cloud.task.repository.support;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.batch.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.jdbc.support.MetaDataAccessException;

/**
 * Automates the creation of a {@link SimpleTaskRepository} which will persist task
 * execution data into a database. Requires the user to describe what kind of database
 * they are using.
 *
 * @author Glenn Renfro
 */
public class JdbcTaskRepositoryFactoryBean implements FactoryBean<TaskRepository>{

	public static final String DEFAULT_TABLE_PREFIX = "TASK_";

	private static final Log logger = LogFactory.getLog(JdbcTaskRepositoryFactoryBean.class);

	private DataSource dataSource;

	private String tablePrefix = DEFAULT_TABLE_PREFIX;

	private DataFieldMaxValueIncrementerFactory incrementerFactory;

	public JdbcTaskRepositoryFactoryBean(){

	}

	public JdbcTaskRepositoryFactoryBean(DataSource dataSource)  {
		if(dataSource != null) {
			this.dataSource = dataSource;
		}
		incrementerFactory = new DefaultDataFieldMaxValueIncrementerFactory(dataSource);
	}

	/**
	 * Sets the table prefix for all the task meta-data tables.
	 * @param tablePrefix prefix prepended to task meta-data tables
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	/**
	 * Returns the a simpleTaskRepository that utilizes a JdbcTaskExecutionDao
	 * @return instance of task repository.
	 */
	public TaskRepository getObject(){
		TaskRepository taskRepository = null;
		logger.debug(String.format("Creating SimpleTaskRepository that will use a %s",
				JdbcTaskExecutionDao.class.getName()));
		taskRepository =  new SimpleTaskRepository(createJdbcTaskExecutionDao());
		return taskRepository;
	}

	@Override
	public Class<?> getObjectType() {
		return TaskRepository.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private TaskExecutionDao createJdbcTaskExecutionDao() {
		JdbcTaskExecutionDao dao = new JdbcTaskExecutionDao(dataSource);
		String databaseType = null;
		try {
			databaseType = org.springframework.batch.support.DatabaseType.fromMetaData(dataSource).name();
		}
		catch (MetaDataAccessException e) {
			throw new IllegalStateException(e);
		}
		dao.setTaskIncrementer(incrementerFactory.getIncrementer(databaseType, tablePrefix + "SEQ"));
		dao.setTablePrefix(tablePrefix);
		return dao;
	}

}
