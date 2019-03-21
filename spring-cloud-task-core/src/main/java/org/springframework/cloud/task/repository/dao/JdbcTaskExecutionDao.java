/*
 * Copyright 2015-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.sql.DataSource;

import org.springframework.batch.item.database.Order;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.database.PagingQueryProvider;
import org.springframework.cloud.task.repository.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Stores Task Execution Information to a JDBC DataSource.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */
public class JdbcTaskExecutionDao implements TaskExecutionDao {


	public static final String SELECT_CLAUSE = "TASK_EXECUTION_ID, "
			+ "START_TIME, END_TIME, TASK_NAME, EXIT_CODE, "
			+ "EXIT_MESSAGE, ERROR_MESSAGE, LAST_UPDATED, "
			+ "EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID ";

	public static final String FROM_CLAUSE = "%PREFIX%EXECUTION";

	public static final String RUNNING_TASK_WHERE_CLAUSE =
			"where TASK_NAME = :taskName AND END_TIME IS NULL ";

	public static final String TASK_NAME_WHERE_CLAUSE = "where TASK_NAME = :taskName ";

	private static final String SAVE_TASK_EXECUTION = "INSERT into %PREFIX%EXECUTION"
			+ "(TASK_EXECUTION_ID, START_TIME, TASK_NAME, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID)"
			+ "values (:taskExecutionId, :startTime, :taskName, :lastUpdated, :externalExecutionId, :parentExecutionId)";

	private static final String CREATE_TASK_ARGUMENT = "INSERT into "
			+ "%PREFIX%EXECUTION_PARAMS(TASK_EXECUTION_ID, TASK_PARAM ) values (:taskExecutionId, :taskParam)";

	private static final String START_TASK_EXECUTION_PREFIX = "UPDATE %PREFIX%EXECUTION set "
			+ "START_TIME = :startTime, TASK_NAME = :taskName, LAST_UPDATED = :lastUpdated";

	private static final String START_TASK_EXECUTION_EXTERNAL_ID_SUFFIX = ", "
			+ "EXTERNAL_EXECUTION_ID = :externalExecutionId, PARENT_EXECUTION_ID = :parentExecutionId where TASK_EXECUTION_ID = :taskExecutionId";

	private static final String START_TASK_EXECUTION_SUFFIX = ", PARENT_EXECUTION_ID = :parentExecutionId where TASK_EXECUTION_ID = :taskExecutionId";

	private static final String CHECK_TASK_EXECUTION_EXISTS = "SELECT COUNT(*) FROM "
			+ "%PREFIX%EXECUTION WHERE TASK_EXECUTION_ID = :taskExecutionId";

	private static final String UPDATE_TASK_EXECUTION = "UPDATE %PREFIX%EXECUTION set "
			+ "END_TIME = :endTime, EXIT_CODE = :exitCode, EXIT_MESSAGE = :exitMessage, ERROR_MESSAGE = :errorMessage, "
			+ "LAST_UPDATED = :lastUpdated where TASK_EXECUTION_ID = :taskExecutionId";

	private static final String UPDATE_TASK_EXECUTION_EXTERNAL_EXECUTION_ID = "UPDATE %PREFIX%EXECUTION set "
			+ "EXTERNAL_EXECUTION_ID = :externalExecutionId where TASK_EXECUTION_ID = :taskExecutionId";

	private static final String GET_EXECUTION_BY_ID = "SELECT TASK_EXECUTION_ID, " +
			"START_TIME, END_TIME, TASK_NAME, EXIT_CODE, "
			+ "EXIT_MESSAGE, ERROR_MESSAGE, LAST_UPDATED, EXTERNAL_EXECUTION_ID, "
			+ "PARENT_EXECUTION_ID "
			+ "from %PREFIX%EXECUTION where TASK_EXECUTION_ID = :taskExecutionId";

	private static final String FIND_ARGUMENT_FROM_ID = "SELECT TASK_EXECUTION_ID, "
			+ "TASK_PARAM from %PREFIX%EXECUTION_PARAMS where TASK_EXECUTION_ID = :taskExecutionId";

	private static final String TASK_EXECUTION_COUNT = "SELECT COUNT(*) FROM " +
			"%PREFIX%EXECUTION ";

	private static final String TASK_EXECUTION_COUNT_BY_NAME = "SELECT COUNT(*) FROM " +
			"%PREFIX%EXECUTION where TASK_NAME = :taskName";

	private static final String RUNNING_TASK_EXECUTION_COUNT_BY_NAME = "SELECT COUNT(*) FROM " +
			"%PREFIX%EXECUTION where TASK_NAME = :taskName AND END_TIME IS NULL ";

	private static final String LAST_TASK_EXECUTIONS_BY_TASK_NAMES =
	"select TE2.* from (" +
	"select MAX(TE.TASK_EXECUTION_ID) as TASK_EXECUTION_ID, TE.TASK_NAME, TE.START_TIME from (" +
	"select TASK_NAME, MAX(START_TIME) as START_TIME" +
	"      FROM %PREFIX%EXECUTION where TASK_NAME in (:taskNames)" +
	"      GROUP BY TASK_NAME" +
	") TE_MAX " +
	"inner join %PREFIX%EXECUTION TE ON TE.TASK_NAME = TE_MAX.TASK_NAME AND TE.START_TIME = TE_MAX.START_TIME " +
	"group by TE.TASK_NAME, TE.START_TIME" +
	") TE1 " +
	"inner join %PREFIX%EXECUTION TE2 ON TE1.TASK_EXECUTION_ID = TE2.TASK_EXECUTION_ID " +
	"order by TE2.START_TIME DESC, TE2.TASK_EXECUTION_ID DESC";

	private static final String FIND_TASK_NAMES = "SELECT distinct TASK_NAME from %PREFIX%EXECUTION order by TASK_NAME";

	private static final String FIND_TASK_EXECUTION_BY_JOB_EXECUTION_ID = "SELECT TASK_EXECUTION_ID FROM %PREFIX%TASK_BATCH WHERE JOB_EXECUTION_ID = :jobExecutionId";

	private static final String FIND_JOB_EXECUTION_BY_TASK_EXECUTION_ID = "SELECT JOB_EXECUTION_ID FROM %PREFIX%TASK_BATCH WHERE TASK_EXECUTION_ID = :taskExecutionId";

	private String tablePrefix = TaskProperties.DEFAULT_TABLE_PREFIX;

	private final NamedParameterJdbcTemplate jdbcTemplate;

	private DataSource dataSource;

	private LinkedHashMap<String, Order> orderMap;

	private DataFieldMaxValueIncrementer taskIncrementer;

	/**
	 * Initializes the JdbcTaskExecutionDao.
	 * @param dataSource used by the dao to execute queries and update the tables.
	 * @param tablePrefix the table prefix to use for this dao.
	 */
	public JdbcTaskExecutionDao(DataSource dataSource, String tablePrefix) {
		this(dataSource);
		Assert.hasText(tablePrefix, "tablePrefix must not be null nor empty");
		this.tablePrefix = tablePrefix;
	}

	/**
	 * Initializes the JdbTaskExecutionDao and defaults the table prefix to
	 * {@link TaskProperties#DEFAULT_TABLE_PREFIX}.
	 * @param dataSource used by the dao to execute queries and update the tables.
	 */
	public JdbcTaskExecutionDao(DataSource dataSource) {
		Assert.notNull(dataSource, "The dataSource must not be null.");
		this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
		this.dataSource = dataSource;
		orderMap = new LinkedHashMap<>();
		orderMap.put("START_TIME", Order.DESCENDING);
		orderMap.put("TASK_EXECUTION_ID", Order.DESCENDING);
	}

	@Override
	public TaskExecution createTaskExecution(String taskName,
			Date startTime, List<String> arguments, String externalExecutionId)  {
		return createTaskExecution(taskName, startTime, arguments,
				externalExecutionId, null);
	}

	@Override
	public TaskExecution createTaskExecution(String taskName, Date startTime,
			List<String> arguments, String externalExecutionId,
			Long parentExecutionId) {
		long nextExecutionId = getNextExecutionId();

		TaskExecution taskExecution = new TaskExecution(nextExecutionId, null, taskName,
				startTime, null, null, arguments, null, externalExecutionId);

		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
			.addValue("taskExecutionId", nextExecutionId,  Types.BIGINT)
			.addValue("startTime", startTime, Types.TIMESTAMP)
			.addValue("taskName", taskName, Types.VARCHAR)
			.addValue("lastUpdated", new Date(), Types.TIMESTAMP)
			.addValue("externalExecutionId", externalExecutionId, Types.VARCHAR)
			.addValue("parentExecutionId", parentExecutionId, Types.BIGINT);

		jdbcTemplate.update(
				getQuery(SAVE_TASK_EXECUTION),
				queryParameters);
		insertTaskArguments(nextExecutionId, arguments);
		return taskExecution;
	}

	@Override
	public TaskExecution startTaskExecution(long executionId, String taskName,
			Date startTime, List<String> arguments,
			String externalExecutionId) {
		return startTaskExecution(executionId, taskName, startTime, arguments,
				externalExecutionId, null);
	}

	@Override
	public TaskExecution startTaskExecution(long executionId, String taskName,
			Date startTime, List<String> arguments,
			String externalExecutionId, Long parentExecutionId) {
		TaskExecution taskExecution = new TaskExecution(executionId, null, taskName,
				startTime, null, null, arguments,null, externalExecutionId, parentExecutionId);

		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
			.addValue("startTime", startTime, Types.TIMESTAMP)
			.addValue("taskName", taskName, Types.VARCHAR)
			.addValue("lastUpdated", new Date(), Types.TIMESTAMP)
			.addValue("parentExecutionId", parentExecutionId, Types.BIGINT)
			.addValue("taskExecutionId", executionId,  Types.BIGINT);

		String updateString = START_TASK_EXECUTION_PREFIX;

		if(externalExecutionId == null) {
			updateString += START_TASK_EXECUTION_SUFFIX;
		}
		else {
			updateString += START_TASK_EXECUTION_EXTERNAL_ID_SUFFIX;
			queryParameters.addValue("externalExecutionId", externalExecutionId, Types.VARCHAR);
		}

		jdbcTemplate.update(getQuery(updateString), queryParameters);
		insertTaskArguments(executionId, arguments);
		return taskExecution;
	}

	@Override
	public void completeTaskExecution(long taskExecutionId, Integer exitCode, Date endTime,
			String exitMessage, String errorMessage) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
			.addValue("taskExecutionId", taskExecutionId, Types.BIGINT);

		// Check if given TaskExecution's Id already exists, if none is found
		// it is invalid and an exception should be thrown.
		if (jdbcTemplate.queryForObject(getQuery(CHECK_TASK_EXECUTION_EXISTS), queryParameters, Integer.class) != 1) {
			throw new IllegalStateException("Invalid TaskExecution, ID " + taskExecutionId + " not found.");
		}

		final MapSqlParameterSource parameters = new MapSqlParameterSource()
			.addValue("endTime", endTime, Types.TIMESTAMP)
			.addValue("exitCode", exitCode, Types.INTEGER)
			.addValue("exitMessage", exitMessage, Types.VARCHAR)
			.addValue("errorMessage", errorMessage, Types.VARCHAR)
			.addValue("lastUpdated", new Date(), Types.TIMESTAMP)
			.addValue("taskExecutionId", taskExecutionId, Types.BIGINT);

		jdbcTemplate.update(
				getQuery(UPDATE_TASK_EXECUTION),
				parameters);
	}

	@Override
	public void completeTaskExecution(long taskExecutionId, Integer exitCode, Date endTime,
			String exitMessage) {
		completeTaskExecution(taskExecutionId, exitCode, endTime, exitMessage, null);
	}

	/**
	 * Public setter for the table prefix property. This will be prefixed to all
	 * the table names before queries are executed. Defaults to
	 * {@link TaskProperties#DEFAULT_TABLE_PREFIX}.
	 *
	 * @param tablePrefix the tablePrefix to set
	 */
	@Deprecated
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	@Override
	public TaskExecution getTaskExecution(long executionId) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
			.addValue("taskExecutionId", executionId, Types.BIGINT);

		try {
			TaskExecution taskExecution = jdbcTemplate.queryForObject(getQuery(GET_EXECUTION_BY_ID),
					queryParameters, new TaskExecutionRowMapper());
			taskExecution.setArguments(getTaskArguments(executionId));
			return taskExecution;
		}
		catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public long getTaskExecutionCountByTaskName(String taskName) {

		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
			.addValue("taskName", taskName, Types.VARCHAR);

		try {
			return jdbcTemplate.queryForObject(
				getQuery(TASK_EXECUTION_COUNT_BY_NAME), queryParameters, Long.class);
		}
		catch (EmptyResultDataAccessException e) {
			return 0;
		}
	}

	@Override
	public long getRunningTaskExecutionCountByTaskName(String taskName) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
			.addValue("taskName", taskName, Types.VARCHAR);

		try {
			return jdbcTemplate.queryForObject(
					getQuery(RUNNING_TASK_EXECUTION_COUNT_BY_NAME), queryParameters, Long.class);
		}
		catch (EmptyResultDataAccessException e) {
			return 0;
		}
	}

	@Override
	public List<TaskExecution> getLatestTaskExecutionsByTaskNames(String... taskNames) {
		Assert.notEmpty(taskNames, "At least 1 task name must be provided.");

		final List<String> taskNamesAsList = new ArrayList<>();

		for (String taskName : taskNames) {
			if (StringUtils.hasText(taskName)) {
				taskNamesAsList.add(taskName);
			}
		}

		Assert.isTrue(taskNamesAsList.size() == taskNames.length,
			String.format("Task names must not contain any empty elements but %s of %s were empty or null.",
					taskNames.length - taskNamesAsList.size(), taskNames.length));

		try {
			final Map<String, List<String>> paramMap = Collections.singletonMap("taskNames", taskNamesAsList);
			return this.jdbcTemplate.query(
					getQuery(LAST_TASK_EXECUTIONS_BY_TASK_NAMES), paramMap, new TaskExecutionRowMapper());
		}
		catch (EmptyResultDataAccessException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public TaskExecution getLatestTaskExecutionForTaskName(String taskName) {
		Assert.hasText(taskName, "The task name must not be empty.");
		final List<TaskExecution> taskExecutions = this.getLatestTaskExecutionsByTaskNames(taskName);
		if (taskExecutions.isEmpty()) {
			return null;
		}
		else if (taskExecutions.size() == 1) {
			return taskExecutions.get(0);
		}
		else {
			throw new IllegalStateException("Only expected a single TaskExecution but received " + taskExecutions.size());
		}
	}

	@Override
	public long getTaskExecutionCount() {

		try {
			return jdbcTemplate.queryForObject(
					getQuery(TASK_EXECUTION_COUNT), new MapSqlParameterSource(), Long.class);
		}
		catch (EmptyResultDataAccessException e) {
			return 0;
		}
	}

	@Override
	public Page<TaskExecution> findRunningTaskExecutions(String taskName, Pageable pageable) {
		return queryForPageableResults(pageable, SELECT_CLAUSE, FROM_CLAUSE,
				RUNNING_TASK_WHERE_CLAUSE, new MapSqlParameterSource("taskName", taskName),
				getRunningTaskExecutionCountByTaskName(taskName));
	}

	@Override
	public Page<TaskExecution> findTaskExecutionsByName(String taskName, Pageable pageable) {
		return queryForPageableResults(pageable, SELECT_CLAUSE, FROM_CLAUSE,
				TASK_NAME_WHERE_CLAUSE, new MapSqlParameterSource("taskName", taskName),
				getTaskExecutionCountByTaskName(taskName));
	}

	@Override
	public List<String> getTaskNames() {
		return jdbcTemplate.queryForList(getQuery(FIND_TASK_NAMES), new MapSqlParameterSource(), String.class);
	}

	@Override
	public Page<TaskExecution> findAll(Pageable pageable) {
		return queryForPageableResults(pageable, SELECT_CLAUSE, FROM_CLAUSE, null,
				new MapSqlParameterSource(), getTaskExecutionCount());
	}

	public void setTaskIncrementer(DataFieldMaxValueIncrementer taskIncrementer) {
		this.taskIncrementer = taskIncrementer;
	}

	public long getNextExecutionId(){
		return taskIncrementer.nextLongValue();
	}

	@Override
	public Long getTaskExecutionIdByJobExecutionId(long jobExecutionId) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
			.addValue("jobExecutionId", jobExecutionId, Types.BIGINT);

		try {
			return jdbcTemplate.queryForObject(
					getQuery(FIND_TASK_EXECUTION_BY_JOB_EXECUTION_ID),
					queryParameters,
					Long.class);
		}
		catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public Set<Long> getJobExecutionIdsByTaskExecutionId(long taskExecutionId) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
			.addValue("taskExecutionId", taskExecutionId, Types.BIGINT);

		try {
			return jdbcTemplate.query(
					getQuery(FIND_JOB_EXECUTION_BY_TASK_EXECUTION_ID),
					queryParameters,
					new ResultSetExtractor<Set<Long>>() {
						@Override
						public Set<Long> extractData(ResultSet resultSet) throws SQLException, DataAccessException {
							Set<Long> jobExecutionIds = new TreeSet<>();

							while(resultSet.next()) {
								jobExecutionIds.add(resultSet.getLong("JOB_EXECUTION_ID"));
							}

							return jobExecutionIds;
						}
					});
		}
		catch (DataAccessException e) {
			return Collections.emptySet();
		}
	}

	@Override
	public void updateExternalExecutionId(long taskExecutionId, String externalExecutionId) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
			.addValue("externalExecutionId", externalExecutionId, Types.VARCHAR)
			.addValue("taskExecutionId", taskExecutionId, Types.BIGINT);

		if (jdbcTemplate.update(
				getQuery(UPDATE_TASK_EXECUTION_EXTERNAL_EXECUTION_ID),
				queryParameters) != 1) {
			throw new IllegalStateException("Invalid TaskExecution, ID "
					+ taskExecutionId + " not found.");
		}
	}

	private Page<TaskExecution> queryForPageableResults(Pageable pageable,
														String selectClause,
														String fromClause,
														String whereClause,
														MapSqlParameterSource queryParameters,
														long totalCount){
		SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
		factoryBean.setSelectClause(selectClause);
		factoryBean.setFromClause(fromClause);
		if(StringUtils.hasText(whereClause)){
			factoryBean.setWhereClause(whereClause);
		}
		final Sort sort = pageable.getSort();
		final LinkedHashMap<String, Order> sortOrderMap = new LinkedHashMap<>();

		if (sort != null) {
			for (Sort.Order sortOrder : sort) {
				sortOrderMap.put(sortOrder.getProperty(), sortOrder.isAscending() ? Order.ASCENDING : Order.DESCENDING);
			}
		}

		if (!CollectionUtils.isEmpty(sortOrderMap)) {
			factoryBean.setSortKeys(sortOrderMap);
		}
		else {
			factoryBean.setSortKeys(this.orderMap);
		}

		factoryBean.setDataSource(this.dataSource);
		PagingQueryProvider pagingQueryProvider;
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
				queryParameters,
				new TaskExecutionRowMapper());
		return new PageImpl<>(resultList, pageable, totalCount);
	}

	private String getQuery(String base) {
		return StringUtils.replace(base, "%PREFIX%", tablePrefix);
	}

	/**
	 * Convenience method that inserts all arguments from the provided
	 * task arguments.
	 *
	 * @param executionId    The executionId to which the arguments are associated.
	 * @param taskArguments  The arguments to be stored.
	 */
	private void insertTaskArguments(long executionId, List<String> taskArguments) {
		for (String args : taskArguments) {
			insertArgument(executionId, args);
		}
	}

	/**
	 * Convenience method that inserts an individual records into the
	 * TASK_EXECUTION_PARAMS table.
	 */
	private void insertArgument(long taskExecutionId, String taskParam) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
			.addValue("taskExecutionId", taskExecutionId, Types.BIGINT)
			.addValue("taskParam", taskParam, Types.VARCHAR);
		jdbcTemplate.update(getQuery(CREATE_TASK_ARGUMENT), queryParameters);
	}

	private List<String> getTaskArguments(long taskExecutionId){
		final List<String> params= new ArrayList<>();
		RowCallbackHandler handler = new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				params.add(rs.getString(2));
			}
		};
		jdbcTemplate.query(getQuery(FIND_ARGUMENT_FROM_ID), new MapSqlParameterSource("taskExecutionId", taskExecutionId),
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
			Long parentExecutionId = rs.getLong("PARENT_EXECUTION_ID");
			if(rs.wasNull()) {
				parentExecutionId = null;
			}
			return new TaskExecution(id,
					getNullableExitCode(rs),
					rs.getString("TASK_NAME"),
					rs.getTimestamp("START_TIME"),
					rs.getTimestamp("END_TIME"),
					rs.getString("EXIT_MESSAGE"),
					getTaskArguments(id),
					rs.getString("ERROR_MESSAGE"),
					rs.getString("EXTERNAL_EXECUTION_ID"),
					parentExecutionId);
		}

		private Integer getNullableExitCode(ResultSet rs) throws SQLException {
			int exitCode = rs.getInt("EXIT_CODE");
			return !rs.wasNull() ? exitCode : null;
		}
	}
}
