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

package org.springframework.cloud.task.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.springframework.batch.item.database.Order;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.database.PagingQueryProvider;
import org.springframework.cloud.task.repository.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Provides a suite of tools that allow tests the ability to retrieve results from a
 * relational database.
 *
 * @author Glenn Renfro
 */
public class TestDBUtils {

	/**
	 * Retrieves the TaskExecution from the datasource.
	 *
	 * @param dataSource      The datasource from which to retrieve the taskExecution.
	 * @param taskExecutionId The id of the task to search.
	 * @return taskExecution
	 */
	public static TaskExecution getTaskExecutionFromDB(DataSource dataSource,
													   String taskExecutionId) {
		String sql = "SELECT * FROM TASK_EXECUTION WHERE "
				+ "TASK_EXECUTION_ID = '"
				+ taskExecutionId + "'";

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		List<TaskExecution> rows = jdbcTemplate.query(sql, new RowMapper<TaskExecution>(){
			@Override
			public TaskExecution mapRow(ResultSet rs, int rownumber) throws SQLException {
				TaskExecution taskExecution=new TaskExecution();
				taskExecution.setExecutionId(rs.getString(1));
				taskExecution.setStartTime(rs.getTimestamp("START_TIME"));
				taskExecution.setEndTime(rs.getTimestamp("END_TIME"));
				taskExecution.setExitCode(rs.getInt("EXIT_CODE"));
				taskExecution.setExitMessage(rs.getString("EXIT_MESSAGE"));
				taskExecution.setStatusCode(rs.getString("STATUS_CODE"));
				taskExecution.setTaskName(rs.getString("TASK_NAME"));
				return taskExecution;
			}
		});
		assertEquals("only one row should be returned", 1, rows.size());
		TaskExecution taskExecution = rows.get(0);

		populateParamsToDB(dataSource, taskExecution);
		return taskExecution;
	}

	/**
	 * Create a pagingQueryProvider specific database type with a findAll.
	 * @param databaseProductName of the database.
	 * @return a PagingQueryPovider that will return all the requested information.
	 * @throws Exception
	 */
	public static PagingQueryProvider getPagingQueryProvider(String databaseProductName) throws Exception{
		return getPagingQueryProvider(databaseProductName, null);
	}

	/**
	 * Create a pagingQueryProvider specific database type with a query containing a where clause.
	 * @param databaseProductName of the database.
	 * @param whereClause  to be applied to the query.
	 * @return a PagingQueryProvider that will return the requested information.
	 * @throws Exception
	 */
	public static PagingQueryProvider getPagingQueryProvider(String databaseProductName,
															 String whereClause) throws Exception{
		DataSource dataSource = getMockDataSource(databaseProductName);
		Map<String, Order> orderMap = new TreeMap<>();
		orderMap.put("START_TIME", Order.DESCENDING);
		orderMap.put("TASK_EXECUTION_ID", Order.DESCENDING);
		SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
		factoryBean.setSelectClause(JdbcTaskExecutionDao.SELECT_CLAUSE);
		factoryBean.setFromClause(JdbcTaskExecutionDao.FROM_CLAUSE);
		if(whereClause != null){
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
		return pagingQueryProvider;
	}

	public static DataSource getMockDataSource(String databaseProductName) throws Exception {
		DatabaseMetaData dmd = mock(DatabaseMetaData.class);
		DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		when(ds.getConnection()).thenReturn(con);
		when(con.getMetaData()).thenReturn(dmd);
		when(dmd.getDatabaseProductName()).thenReturn(databaseProductName);
		return ds;
	}

	private static void populateParamsToDB(DataSource dataSource, TaskExecution taskExecution) {
		String sql = "SELECT * FROM TASK_EXECUTION_PARAMS WHERE TASK_EXECUTION_ID = '"
				+ taskExecution.getExecutionId() + "'";

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
		List<String> params = new ArrayList<>();
		for (Map row : rows) {
			params.add((String) row.get("TASK_PARAM"));
		}
		taskExecution.setParameters(params);
	}
}
