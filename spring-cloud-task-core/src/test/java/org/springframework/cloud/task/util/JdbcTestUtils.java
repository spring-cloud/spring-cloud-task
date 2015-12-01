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

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base set of verifiers for data stored in a JDBC Repository.
 *
 * @author Glenn Renfro
 */
public class JdbcTestUtils {

	public static void verifyTaskExecution(DataSource dataSource,
											   TaskExecution taskExecution){
		String sql = "SELECT * FROM TASK_EXECUTION WHERE TASK_EXECUTION_ID = '"
				+ taskExecution.getExecutionId() + "'";

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
		assertEquals("only one row should be returned", 1, rows.size());
		for (Map row : rows) {
			String resultExecutionId = (String)row.get("TASK_EXECUTION_ID");
			Date resultStartTime = (Date) row.get("START_TIME");
			Date resultEndTime = (Date) row.get("END_TIME");
			Integer resultExitCode = (Integer)row.get("EXIT_CODE");
			String resultExitMessage = (String)row.get("EXIT_MESSAGE");
			String resultStatusCode = (String)row.get("STATUS_CODE");
			String resultTaskName = (String)row.get("TASK_NAME");
			assertEquals("taskExecutionId must be equal", taskExecution.getExecutionId(),
					resultExecutionId);
			assertEquals("startTime must be equal",
					taskExecution.getStartTime(), resultStartTime);
			assertEquals("endTime must be equal",
					taskExecution.getEndTime(), resultEndTime);
			assertEquals("exitCode must be equal",
					Integer.valueOf(taskExecution.getExitCode()), resultExitCode);
			assertEquals("taskName must be equal",
					taskExecution.getTaskName(), resultTaskName);
			assertEquals("exitMessage must be equal",
					taskExecution.getExitMessage(), resultExitMessage);
			assertEquals("statusCode must be equal",
					taskExecution.getStatusCode(), resultStatusCode);
		}
	}
	private static void verifyParams(DataSource dataSource, TaskExecution taskExecution){

	}
}
