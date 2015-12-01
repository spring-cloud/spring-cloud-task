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

package org.springframework.cloud.task.repository.dao;

import java.sql.Types;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Stores Task Execution Information to a JDBC DataSource.
 *
 * @author Glenn Renfro
 */
public class JdbcTaskExecutionDao implements TaskExecutionDao {


	private static final String SAVE_TASK_EXECUTION = "INSERT into %PREFIX%EXECUTION"
			+ "(TASK_EXECUTION_ID, START_TIME, END_TIME, TASK_NAME, EXIT_CODE, "
			+ "EXIT_MESSAGE, LAST_UPDATED, STATUS_CODE) values (?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String CREATE_TASK_PARAMETER = "INSERT into "
			+ "%PREFIX%EXECUTION_PARAMS(TASK_EXECUTION_ID, TASK_PARAM ) values (?, ?)";

	private static final String CHECK_TASK_EXECUTION_EXISTS = "SELECT COUNT(*) FROM "
			+ "%PREFIX%EXECUTION WHERE TASK_EXECUTION_ID = ?";

	private static final String UPDATE_TASK_EXECUTION = "UPDATE %PREFIX%EXECUTION set "
			+ "START_TIME = ?, END_TIME = ?, TASK_NAME = ?, EXIT_CODE = ?, "
			+ "EXIT_MESSAGE = ?, LAST_UPDATED = ?, STATUS_CODE = ? "
			+ "where TASK_EXECUTION_ID = ?";


	private static final String DEFAULT_TABLE_PREFIX = "TASK_";

	private String tablePrefix = DEFAULT_TABLE_PREFIX;

	private JdbcOperations jdbcTemplate;

	public JdbcTaskExecutionDao(DataSource dataSource) {
		Assert.notNull(dataSource);
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public void saveTaskExecution(TaskExecution taskExecution) {
		Object[] parameters = new Object[]{ taskExecution.getExecutionId(),
				taskExecution.getStartTime(), taskExecution.getEndTime(),
				taskExecution.getTaskName(), taskExecution.getExitCode(),
				taskExecution.getExitMessage(), new Date(),
				taskExecution.getStatusCode() };
		int addCount = jdbcTemplate.update(
				getQuery(SAVE_TASK_EXECUTION),
				parameters,
				new int[]{ Types.VARCHAR, Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR,
						Types.INTEGER, Types.VARCHAR, Types.TIMESTAMP, Types.VARCHAR });
		insertJobParameters(taskExecution.getExecutionId(), taskExecution.getParameters());
	}

	@Override
	public void updateTaskExecution(TaskExecution taskExecution) {
		// Check if given TaskExecution's Id already exists, if none is found
		// it is invalid and an exception should be thrown.
		if (jdbcTemplate.queryForObject(getQuery(CHECK_TASK_EXECUTION_EXISTS), Integer.class,
				new Object[]{ taskExecution.getExecutionId() }) != 1) {
			throw new IllegalStateException("Invalid TaskExecution, ID " + taskExecution.getExecutionId() + " not found.");
		}

		Object[] parameters = new Object[]{ taskExecution.getStartTime(), taskExecution.getEndTime(),
				taskExecution.getTaskName(), taskExecution.getExitCode(),
				taskExecution.getExitMessage(), new Date(), taskExecution.getStatusCode(),
				taskExecution.getExecutionId() };
		int count = jdbcTemplate.update(
				getQuery(UPDATE_TASK_EXECUTION),
				parameters,
				new int[]{ Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR, Types.INTEGER,
						Types.VARCHAR, Types.TIMESTAMP, Types.VARCHAR, Types.VARCHAR });
	}

	/**
	 * Public setter for the table prefix property. This will be prefixed to all
	 * the table names before queries are executed. Defaults to
	 * {@link #DEFAULT_TABLE_PREFIX}.
	 *
	 * @param tablePrefix the tablePrefix to set
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	private String getQuery(String base) {
		return StringUtils.replace(base, "%PREFIX%", tablePrefix);
	}

	/**
	 * Convenience method that inserts all parameters from the provided
	 * task parameters.
	 *
	 * @param executionId    The executionId to which the params are associated.
	 * @param taskParameters The parameters to be stored.
	 */
	private void insertJobParameters(String executionId, List<String> taskParameters) {
		for (String param : taskParameters) {
			insertParameter(executionId, param);
		}
	}

	/**
	 * Convenience method that inserts an individual records into the
	 * TASK_EXECUTION_PARAMS table.
	 */
	private void insertParameter(String executionId, String param) {

		Object[] args = new Object[0];
		int[] argTypes = new int[]{ Types.VARCHAR, Types.VARCHAR };

		args = new Object[]{ executionId, param };
		jdbcTemplate.update(getQuery(CREATE_TASK_PARAMETER), args, argTypes);
	}

}
