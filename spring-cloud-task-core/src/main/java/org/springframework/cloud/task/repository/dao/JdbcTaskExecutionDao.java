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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.springframework.batch.item.database.Order;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.database.PagingQueryProvider;
import org.springframework.cloud.task.repository.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Stores Task Execution Information to a JDBC DataSource.
 *
 * @author Glenn Renfro
 */
public class JdbcTaskExecutionDao implements TaskExecutionDao {


	public static final String SELECT_CLAUSE = "TASK_EXECUTION_ID, TASK_EXTERNAL_EXECUTION_ID, "
			+ "START_TIME, END_TIME, TASK_NAME, EXIT_CODE, "
			+ "EXIT_MESSAGE, LAST_UPDATED, STATUS_CODE ";

	public static final String FROM_CLAUSE = "%PREFIX%EXECUTION";

	public static final String RUNNING_TASK_WHERE_CLAUSE =
			"where TASK_NAME = ? AND END_TIME IS NULL ";

	public static final String TASK_NAME_WHERE_CLAUSE = "where TASK_NAME = ? ";

	private static final String SAVE_TASK_EXECUTION = "INSERT into %PREFIX%EXECUTION"
			+ "(TASK_EXECUTION_ID, TASK_EXTERNAL_EXECUTION_ID, START_TIME, END_TIME, "
			+ "TASK_NAME, EXIT_CODE, EXIT_MESSAGE, LAST_UPDATED, STATUS_CODE)"
			+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String CREATE_TASK_PARAMETER = "INSERT into "
			+ "%PREFIX%EXECUTION_PARAMS(TASK_EXECUTION_ID, TASK_PARAM ) values (?, ?)";

	private static final String CHECK_TASK_EXECUTION_EXISTS = "SELECT COUNT(*) FROM "
			+ "%PREFIX%EXECUTION WHERE TASK_EXECUTION_ID = ?";

	private static final String UPDATE_TASK_EXECUTION = "UPDATE %PREFIX%EXECUTION set "
			+ "START_TIME = ?, END_TIME = ?, TASK_NAME = ?, EXIT_CODE = ?, "
			+ "EXIT_MESSAGE = ?, LAST_UPDATED = ?, STATUS_CODE = ?, "
			+ "TASK_EXTERNAL_EXECUTION_ID = ? where TASK_EXECUTION_ID = ?";

	private static final String GET_EXECUTION_BY_ID = "SELECT TASK_EXECUTION_ID, " +
			"START_TIME, END_TIME, TASK_NAME, EXIT_CODE, "
			+ "EXIT_MESSAGE, LAST_UPDATED, STATUS_CODE, TASK_EXTERNAL_EXECUTION_ID "
			+ "from %PREFIX%EXECUTION where TASK_EXECUTION_ID = ?";

	private static final String FIND_PARAMS_FROM_ID = "SELECT TASK_EXECUTION_ID, "
			+ "TASK_PARAM from %PREFIX%EXECUTION_PARAMS where TASK_EXECUTION_ID = ?";

	private static final String TASK_EXECUTION_COUNT = "SELECT COUNT(*) FROM " +
			"%PREFIX%EXECUTION ";

	private static final String TASK_EXECUTION_COUNT_BY_NAME = "SELECT COUNT(*) FROM " +
			"%PREFIX%EXECUTION where TASK_NAME = ?";

	private static final String RUNNING_TASK_EXECUTION_COUNT_BY_NAME = "SELECT COUNT(*) FROM " +
			"%PREFIX%EXECUTION where TASK_NAME = ? AND END_TIME IS NULL ";

	final String FIND_TASK_NAMES = "SELECT distinct TASK_NAME from %PREFIX%EXECUTION order by TASK_NAME";

	private static final String DEFAULT_TABLE_PREFIX = "TASK_";

	private String tablePrefix = DEFAULT_TABLE_PREFIX;

	private JdbcOperations jdbcTemplate;

	private DataSource dataSource;

	private Map<String, Order> orderMap;

	private DataFieldMaxValueIncrementer taskIncrementer;

	public JdbcTaskExecutionDao(DataSource dataSource) {
		Assert.notNull(dataSource);
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.dataSource = dataSource;
		orderMap = new TreeMap<>();
		orderMap.put("START_TIME", Order.DESCENDING);
		orderMap.put("TASK_EXECUTION_ID", Order.DESCENDING);
	}

	@Override
	public void saveTaskExecution(TaskExecution taskExecution) {
		Object[] parameters = new Object[]{ taskExecution.getExecutionId(),
				taskExecution.getExternalExecutionID(),
				taskExecution.getStartTime(), taskExecution.getEndTime(),
				taskExecution.getTaskName(), taskExecution.getExitCode(),
				taskExecution.getExitMessage(), new Date(),
				taskExecution.getStatusCode() };
		jdbcTemplate.update(
				getQuery(SAVE_TASK_EXECUTION),
				parameters,
				new int[]{ Types.BIGINT, Types.VARCHAR, Types.TIMESTAMP, Types.TIMESTAMP,
						Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.TIMESTAMP,
						Types.VARCHAR });
		insertTaskParameters(taskExecution.getExecutionId(), taskExecution.getParameters());
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
				taskExecution.getExternalExecutionID(), taskExecution.getExecutionId()};
		jdbcTemplate.update(
				getQuery(UPDATE_TASK_EXECUTION),
				parameters,
				new int[]{ Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR, Types.INTEGER,
						Types.VARCHAR, Types.TIMESTAMP, Types.VARCHAR, Types.VARCHAR,
						Types.BIGINT});
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

	@Override
	public TaskExecution getTaskExecution(long executionId) {
		try {
			TaskExecution taskExecution = jdbcTemplate.queryForObject(getQuery(GET_EXECUTION_BY_ID),
					new TaskExecutionRowMapper(), executionId);
			taskExecution.setParameters(getTaskParameters(executionId));
			return taskExecution;
		}
		catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public long getTaskExecutionCountByTaskName(String taskName) {
		try {
		return jdbcTemplate.queryForObject(
				getQuery(TASK_EXECUTION_COUNT_BY_NAME), new Object[] { taskName }, Long.class);
		}
		catch (EmptyResultDataAccessException e) {
			return 0;
		}
	}

	@Override
	public long getRunningTaskExecutionCountByTaskName(String taskName) {
		try {
			return jdbcTemplate.queryForObject(
					getQuery(RUNNING_TASK_EXECUTION_COUNT_BY_NAME), new Object[] { taskName }, Long.class);
		}
		catch (EmptyResultDataAccessException e) {
			return 0;
		}
	}

	@Override
	public long getTaskExecutionCount() {
		try {
			return jdbcTemplate.queryForObject(
					getQuery(TASK_EXECUTION_COUNT), new Object[] {  }, Long.class);
		}
		catch (EmptyResultDataAccessException e) {
			return 0;
		}
	}

	@Override
	public Page<TaskExecution> findRunningTaskExecutions(String taskName, Pageable pageable) {
		return queryForPageableResults(pageable, SELECT_CLAUSE, FROM_CLAUSE,
				RUNNING_TASK_WHERE_CLAUSE, new Object[]{ taskName },
				getRunningTaskExecutionCountByTaskName(taskName));
	}

	@Override
	public Page<TaskExecution> findTaskExecutionsByName(String taskName, Pageable pageable) {
		return queryForPageableResults(pageable, SELECT_CLAUSE, FROM_CLAUSE,
				TASK_NAME_WHERE_CLAUSE, new Object[]{ taskName },
				getTaskExecutionCountByTaskName(taskName));
	}

	@Override
	public List<String> getTaskNames() {
		return jdbcTemplate.queryForList(getQuery(FIND_TASK_NAMES), String.class);
	}

	@Override
	public Page<TaskExecution> findAll(Pageable pageable) {
		return queryForPageableResults(pageable, SELECT_CLAUSE, FROM_CLAUSE, null,
				new Object[]{  }, getTaskExecutionCount());
	}

	public void setTaskIncrementer(DataFieldMaxValueIncrementer taskIncrementer) {
		this.taskIncrementer = taskIncrementer;
	}

	public long getNextExecutionId(){
		return taskIncrementer.nextLongValue();
	}

	private Page<TaskExecution> queryForPageableResults(Pageable pageable,
														String selectClause,
														String fromClause,
														String whereClause,
														Object[] queryParam,
														long totalCount){
		SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
		factoryBean.setSelectClause(selectClause);
		factoryBean.setFromClause(fromClause);
		if(StringUtils.hasText(whereClause)){
			factoryBean.setWhereClause(whereClause);
		}
		factoryBean.setSortKeys(orderMap);
		factoryBean.setDataSource(dataSource);
		PagingQueryProvider pagingQueryProvider = null;
		try {
			pagingQueryProvider = factoryBean.getObject();
			pagingQueryProvider.init(dataSource);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
		String query = pagingQueryProvider.getPageQuery(pageable);
		List<TaskExecution> resultList = jdbcTemplate.query(
				getQuery(query),
				queryParam,
				new TaskExecutionRowMapper());
		return new PageImpl<TaskExecution>(resultList, pageable, totalCount);
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
	private void insertTaskParameters(long executionId, List<String> taskParameters) {
		for (String param : taskParameters) {
			insertParameter(executionId, param);
		}
	}

	/**
	 * Convenience method that inserts an individual records into the
	 * TASK_EXECUTION_PARAMS table.
	 */
	private void insertParameter(long executionId, String param) {
		int[] argTypes = new int[]{ Types.BIGINT, Types.VARCHAR };
		Object[] args = new Object[]{ executionId, param };
		jdbcTemplate.update(getQuery(CREATE_TASK_PARAMETER), args, argTypes);
	}

	private List<String> getTaskParameters(long executionId){
		final List<String> params= new ArrayList<>();
		RowCallbackHandler handler = new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				params.add(rs.getString(2));
			}
		};

		jdbcTemplate.query(getQuery(FIND_PARAMS_FROM_ID), new Object[] { executionId },
				handler);
		return params;
	}
	/**
	 * Re-usable mapper for {@link TaskExecution} instances.
	 *
	 */
	private final class TaskExecutionRowMapper implements RowMapper<TaskExecution> {

		public TaskExecutionRowMapper() {
		}

		@Override
		public TaskExecution mapRow(ResultSet rs, int rowNum) throws SQLException {
			long  id = rs.getLong("TASK_EXECUTION_ID");
			TaskExecution taskExecution=new TaskExecution(id,
					rs.getInt("EXIT_CODE"),
					rs.getString("TASK_NAME"),
					rs.getTimestamp("START_TIME"),
					rs.getTimestamp("END_TIME"),
					rs.getString("STATUS_CODE"),
					rs.getString("EXIT_MESSAGE"),
					getTaskParameters(id),
					rs.getString("TASK_EXTERNAL_EXECUTION_ID"));
			return taskExecution;
		}
	}

}
