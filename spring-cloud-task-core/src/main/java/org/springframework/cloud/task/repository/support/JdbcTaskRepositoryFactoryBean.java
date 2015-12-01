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
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Automates the creation of a {@link SimpleTaskRepository} using JDBC DAO implementations
 * which persist task execution data into the database. Requires the user to describe
 * what kind of database they are using.
 *
 * @author Glenn Renfro
 */
public class JdbcTaskRepositoryFactoryBean implements TaskRepositoryFactoryBean{

	public static final String DEFAULT_TABLE_PREFIX = "";

	protected static final Log logger = LogFactory.getLog(JdbcTaskRepositoryFactoryBean.class);

	private JdbcOperations jdbcOperations;

	private String tablePrefix = DEFAULT_TABLE_PREFIX;

	public JdbcTaskRepositoryFactoryBean(DataSource dataSource)  {
		if(dataSource != null && jdbcOperations == null) {
			jdbcOperations = new JdbcTemplate(dataSource);
		}
	}

	/**
	 * Sets the table prefix for all the batch meta-data tables.
	 * @param tablePrefix prefix prepended to batch meta-data tables
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	public TaskExecutionDao createJdbcTaskExecutionDao()  {
		JdbcTaskExecutionDao dao = new JdbcTaskExecutionDao(jdbcOperations);
		dao.setTablePrefix(tablePrefix);
		return dao;
	}

	public TaskRepository getObject(){
		TaskRepository taskRepository = null;
		taskRepository =  new SimpleTaskRepository(createJdbcTaskExecutionDao());
		return taskRepository;
	}



}