/*
 * Copyright 2022-2022 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.batch.item.database.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Henning Pöttker
 */
class H2PagingQueryProviderTests {

	static Stream<Arguments> testH2PagingQueryProvider() {
		return Arrays.stream(org.h2.engine.Mode.ModeEnum.values())
			.map(mode -> Arguments.of(mode.toString()));
	}

	@ParameterizedTest
	@MethodSource
	void testH2PagingQueryProvider(String compatibilityMode) {
		String connectionUrl = String.format("jdbc:h2:mem:%s;MODE=%s", UUID.randomUUID(), compatibilityMode);
		DataSource dataSource = new SimpleDriverDataSource(new org.h2.Driver(), connectionUrl, "sa", "");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		PlatformTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		transactionTemplate.executeWithoutResult(status -> {
			jdbcTemplate.execute("CREATE TABLE TEST_TABLE (ID BIGINT NOT NULL, STRING VARCHAR(10) NOT NULL)");
			jdbcTemplate.execute("INSERT INTO TEST_TABLE (ID, STRING) VALUES (1, 'Spring')");
			jdbcTemplate.execute("INSERT INTO TEST_TABLE (ID, STRING) VALUES (2, 'Cloud')");
			jdbcTemplate.execute("INSERT INTO TEST_TABLE (ID, STRING) VALUES (3, 'Task')");

			H2PagingQueryProvider queryProvider = new H2PagingQueryProvider();
			queryProvider.setSelectClause("STRING");
			queryProvider.setFromClause("TEST_TABLE");
			Map<String, Order> sortKeys = new HashMap<>();
			sortKeys.put("ID", Order.ASCENDING);
			queryProvider.setSortKeys(sortKeys);

			List<String> firstPage = jdbcTemplate.queryForList(
				queryProvider.getPageQuery(PageRequest.of(0, 2)),
				String.class
			);
			assertThat(firstPage).containsExactly("Spring", "Cloud");

			List<String> secondPage = jdbcTemplate.queryForList(
				queryProvider.getPageQuery(PageRequest.of(1, 2)),
				String.class
			);
			assertThat(secondPage).containsExactly("Task");
		});
	}

}
