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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.cloud.task.repository.TaskExecution;
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
	 * Retrieves the TaskExecution from the datasource
	 *
	 * @param dataSource      The datasource from which to retrieve the taskExecution
	 * @param taskExecutionId The id of the task to search .
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
