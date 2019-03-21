/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.cloud.task.repository.database.support;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.springframework.cloud.task.repository.database.PagingQueryProvider;
import org.springframework.cloud.task.util.TestDBUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
@RunWith(Parameterized.class)
public class WhereClausePagingQueryProviderTests {

	private String databaseProductName;

	private String expectedQuery;

	private Pageable pageable = PageRequest.of(0, 10);

	public WhereClausePagingQueryProviderTests(String databaseProductName,
			String expectedQuery) {
		this.databaseProductName = databaseProductName;
		this.expectedQuery = expectedQuery;
	}

	// @checkstyle:off
	@Parameterized.Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "Oracle", "SELECT TASK_EXECUTION_ID, START_TIME, END_TIME, TASK_NAME, "
						+ "EXIT_CODE, EXIT_MESSAGE, ERROR_MESSAGE, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID FROM "
						+ "(SELECT TASK_EXECUTION_ID, START_TIME, END_TIME, TASK_NAME, "
						+ "EXIT_CODE, EXIT_MESSAGE, ERROR_MESSAGE, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID, ROWNUM as "
						+ "TMP_ROW_NUM FROM (SELECT TASK_EXECUTION_ID, START_TIME, "
						+ "END_TIME, TASK_NAME, EXIT_CODE, EXIT_MESSAGE, ERROR_MESSAGE, "
						+ "LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID FROM %PREFIX%EXECUTION "
						+ "WHERE TASK_EXECUTION_ID = '0000' ORDER BY START_TIME DESC, "
						+ "TASK_EXECUTION_ID DESC)) WHERE TMP_ROW_NUM >= 1 AND "
						+ "TMP_ROW_NUM < 11" },
				{ "HSQL Database Engine", "SELECT LIMIT 0 10 TASK_EXECUTION_ID, "
						+ "START_TIME, END_TIME, TASK_NAME, EXIT_CODE, EXIT_MESSAGE, "
						+ "ERROR_MESSAGE, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID FROM %PREFIX%EXECUTION "
						+ "WHERE TASK_EXECUTION_ID = '0000' ORDER BY "
						+ "START_TIME DESC, TASK_EXECUTION_ID DESC" },
				{ "PostgreSQL", "SELECT TASK_EXECUTION_ID, START_TIME, END_TIME, "
						+ "TASK_NAME, EXIT_CODE, EXIT_MESSAGE, ERROR_MESSAGE, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID "
						+ "FROM %PREFIX%EXECUTION WHERE TASK_EXECUTION_ID = '0000' "
						+ "ORDER BY START_TIME DESC, "
						+ "TASK_EXECUTION_ID DESC LIMIT 10 OFFSET 0" },
				{ "MySQL", "SELECT TASK_EXECUTION_ID, START_TIME, END_TIME, TASK_NAME, "
						+ "EXIT_CODE, EXIT_MESSAGE, ERROR_MESSAGE, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID FROM "
						+ "%PREFIX%EXECUTION WHERE TASK_EXECUTION_ID = '0000' "
						+ "ORDER BY START_TIME DESC, "
						+ "TASK_EXECUTION_ID DESC LIMIT 0, 10" },
				{ "Microsoft SQL Server",
						"SELECT TASK_EXECUTION_ID, START_TIME, END_TIME, "
								+ "TASK_NAME, EXIT_CODE, EXIT_MESSAGE, ERROR_MESSAGE, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID FROM "
								+ "(SELECT TASK_EXECUTION_ID, START_TIME, END_TIME, TASK_NAME, "
								+ "EXIT_CODE, EXIT_MESSAGE, ERROR_MESSAGE, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID, ROW_NUMBER() "
								+ "OVER (ORDER BY START_TIME DESC, TASK_EXECUTION_ID DESC) AS "
								+ "TMP_ROW_NUM  FROM %PREFIX%EXECUTION WHERE TASK_EXECUTION_ID = "
								+ "'0000') TASK_EXECUTION_PAGE  WHERE TMP_ROW_NUM >= 1 "
								+ "AND TMP_ROW_NUM < 11 ORDER BY START_TIME DESC, TASK_EXECUTION_ID DESC" } });
	}
	// @checkstyle:on

	@Test
	public void testGeneratedQuery() throws Exception {
		PagingQueryProvider pagingQueryProvider = TestDBUtils.getPagingQueryProvider(
				this.databaseProductName, "TASK_EXECUTION_ID = '0000'");
		String actualQuery = pagingQueryProvider.getPageQuery(this.pageable);
		assertThat(actualQuery).as(
				String.format("the generated query for %s, was not the expected query",
						this.databaseProductName))
				.isEqualTo(this.expectedQuery);
	}

}
