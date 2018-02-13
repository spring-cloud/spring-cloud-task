/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.cloud.task.batch.listener.support;

import javax.sql.DataSource;

import org.springframework.batch.core.JobExecution;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * JDBC based implementation of the {@link TaskBatchDao}.  Intended to be used in
 * conjunction with the JDBC based
 * {@link org.springframework.cloud.task.repository.TaskRepository}
 *
 * @author Michael Minella
 * @author Glenn Renfro
 */
public class JdbcTaskBatchDao implements TaskBatchDao {

	private String tablePrefix = TaskProperties.DEFAULT_TABLE_PREFIX;

	private static final String INSERT_STATEMENT = "INSERT INTO %PREFIX%TASK_BATCH VALUES(?, ?)";

	private JdbcOperations jdbcTemplate;

	/**
	 * Intializes the JdbcTaskBatchDao.
	 * @param dataSource {@link DataSource} where the task batch table resides.
	 * @param tablePrefix the table prefix to use for this dao.
	 */
	public JdbcTaskBatchDao(DataSource dataSource, String tablePrefix) {
		this(dataSource);
		Assert.hasText(tablePrefix, "tablePrefix must not be null nor empty.");
		this.tablePrefix = tablePrefix;
	}

	/**
	 * Intializes the JdbcTaskBatchDao and defaults the table prefix to
	 * {@link TaskProperties#DEFAULT_TABLE_PREFIX}.
	 * @param dataSource {@link DataSource} where the task batch table resides.
	 */
	public JdbcTaskBatchDao(DataSource dataSource) {
		Assert.notNull(dataSource, "A dataSource is required");

		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public void saveRelationship(TaskExecution taskExecution, JobExecution jobExecution) {
		Assert.notNull(taskExecution, "A taskExecution is required");
		Assert.notNull(jobExecution, "A jobExecution is required");
		jdbcTemplate.update(getQuery(INSERT_STATEMENT), taskExecution.getExecutionId(), jobExecution.getId());
	}

	private String getQuery(String base) {
		return StringUtils.replace(base, "%PREFIX%", tablePrefix);
	}
}
