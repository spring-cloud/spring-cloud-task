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
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;

/**
 * Automates the creation of a {@link SimpleTaskExplorer} which will retrieve task
 * execution data from a database.
 *
 * @author Glenn Renfro
 */
public class JdbcTaskExplorerFactoryBean {

	public static final String DEFAULT_TABLE_PREFIX = "TASK_";

	private static final Log logger = LogFactory.getLog(JdbcTaskExplorerFactoryBean.class);

	private DataSource dataSource;

	private String tablePrefix = DEFAULT_TABLE_PREFIX;

	public JdbcTaskExplorerFactoryBean(){

	}

	public JdbcTaskExplorerFactoryBean(DataSource dataSource)  {
		if(dataSource != null) {
			this.dataSource = dataSource;
		}
	}

	/**
	 * Sets the table prefix for all the task meta-data tables.
	 * @param tablePrefix prefix prepended to task meta-data tables
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	/**
	 * Returns the a simpleTaskExplorer that utilizes a JdbcTaskExecutionDao
	 * @return instance of task repository.
	 */
	public TaskExplorer getObject(){
		TaskExplorer taskExplorer = null;
		logger.debug(String.format("Creating SimpleTaskExplorer that will use a %s",
				JdbcTaskExecutionDao.class.getName()));
		taskExplorer =  new SimpleTaskExplorer(createJdbcTaskExecutionDao());
		return taskExplorer;
	}

	private TaskExecutionDao createJdbcTaskExecutionDao()  {
		JdbcTaskExecutionDao dao = new JdbcTaskExecutionDao(dataSource);
		dao.setTablePrefix(tablePrefix);
		return dao;
	}

}
