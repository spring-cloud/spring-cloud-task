/*
 * Copyright 2016 the original author or authors.
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

import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.batch.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.MapTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link FactoryBean} implementation that creates the appropriate
 * {@link TaskExecutionDao} based on the provided information.
 *
 * @author Michael Minella
 */
public class TaskExecutionDaoFactoryBean implements FactoryBean<TaskExecutionDao> {

	public static final String DEFAULT_TABLE_PREFIX = "TASK_";

	private ConfigurableApplicationContext context;

	private TaskExecutionDao dao = null;

	private String dataSourceName;

	private String tablePrefix = DEFAULT_TABLE_PREFIX;

	/**
	 * Default constructor will result in a Map based TaskExecutionDao.  <b>This is only
	 * intended for testing purposes.</b>
	 */
	public TaskExecutionDaoFactoryBean() {
	}

	/**
	 * ApplicationContext provided will be used to obtain the appropriate
	 * {@link DataSource}.
	 *
	 * @param context context for this application
	 */
	public TaskExecutionDaoFactoryBean(ConfigurableApplicationContext context) {
		Assert.notNull(context, "An ApplicationContext is required");

		this.context = context;
	}

	@Override
	public TaskExecutionDao getObject() throws Exception {
		if(this.dao == null) {
			if(this.context != null) {
				if (StringUtils.hasText(this.dataSourceName)) {
					if(!this.context.containsBean(this.dataSourceName)) {
						throw new IllegalArgumentException("The configured dataSourceName is not available in the current context");
					}

					DataSource dataSource = (DataSource) this.context.getBean(this.dataSourceName);
					buildTaskExecutionDao(dataSource);
				}
				else if (this.context.getBeanNamesForType(DataSource.class).length == 1) {
					DataSource dataSource = this.context.getBean(DataSource.class);
					buildTaskExecutionDao(dataSource);

				}
				else {
					this.dao = new MapTaskExecutionDao();
				}
			}
			else {
				this.dao = new MapTaskExecutionDao();
			}
		}

		return this.dao;
	}

	@Override
	public Class<?> getObjectType() {
		return TaskExecutionDao.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	/**
	 * Identifies the {@link DataSource} to be used if one is to be used.  By default, the
	 * name is not specified and it is assumed that only one DataSource exists within the
	 * context.
	 *
	 * @param dataSourceName bean id for the DataSource to be used.
	 */
	public void setDataSourceName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}

	/**
	 * Indicates a prefix for all of the task repository's tables if the jdbc option is
	 * used.
	 *
	 * @param tablePrefix the string prefix for the task table names
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	private void buildTaskExecutionDao(DataSource dataSource) {
		DataFieldMaxValueIncrementerFactory incrementerFactory = new DefaultDataFieldMaxValueIncrementerFactory(dataSource);
		this.dao = new JdbcTaskExecutionDao(dataSource);
		String databaseType;
		try {
			databaseType = org.springframework.batch.support.DatabaseType.fromMetaData(dataSource).name();
		}
		catch (MetaDataAccessException e) {
			throw new IllegalStateException(e);
		}
		((JdbcTaskExecutionDao) this.dao).setTaskIncrementer(incrementerFactory.getIncrementer(databaseType, this.tablePrefix + "SEQ"));
		((JdbcTaskExecutionDao) this.dao).setTablePrefix(this.tablePrefix);
	}
}
