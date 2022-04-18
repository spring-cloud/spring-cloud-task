/*
 * Copyright 2015-2022 the original author or authors.
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

package org.springframework.cloud.task.repository.support;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.task.util.TestDBUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.cloud.task.repository.support.DatabaseType.*;

/**
 * Tests that the correct database names are selected from datasource metadata.
 *
 * @author Lucas Ward
 * @author Will Schipp
 * @author Glenn Renfro
 * @author Ben Blinebury
 *
 */
public class DatabaseTypeTests {

	@Test
	public void testFromProductName() {
		assertThat(fromProductName("HSQL Database Engine")).isEqualTo(HSQL);
		assertThat(fromProductName("Oracle")).isEqualTo(ORACLE);
		assertThat(fromProductName("PostgreSQL")).isEqualTo(POSTGRES);
		assertThat(fromProductName("MySQL")).isEqualTo(MYSQL);
		assertThat(fromProductName("MariaDB")).isEqualTo(MARIADB);
	}

	@Test
	public void testInvalidProductName() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> fromProductName("bad product name"));
	}

	@Test
	public void testFromMetaDataForHsql() throws Exception {
		DataSource ds = TestDBUtils.getMockDataSource("HSQL Database Engine");
		assertThat(DatabaseType.fromMetaData(ds)).isEqualTo(HSQL);
	}

	@Test
	public void testFromMetaDataForOracle() throws Exception {
		DataSource ds = TestDBUtils.getMockDataSource("Oracle");
		assertThat(DatabaseType.fromMetaData(ds)).isEqualTo(ORACLE);
	}

	@Test
	public void testFromMetaDataForPostgres() throws Exception {
		DataSource ds = TestDBUtils.getMockDataSource("PostgreSQL");
		assertThat(DatabaseType.fromMetaData(ds)).isEqualTo(POSTGRES);
	}

	@Test
	public void testFromMetaDataForMySQL() throws Exception {
		DataSource ds = TestDBUtils.getMockDataSource("MySQL");
		assertThat(DatabaseType.fromMetaData(ds)).isEqualTo(MYSQL);
	}

	@Test
	public void testFromMetaDataForMariaDB() throws Exception {
		DataSource ds = TestDBUtils.getMockDataSource("MariaDB");
		assertThat(DatabaseType.fromMetaData(ds)).isEqualTo(MARIADB);
	}

}
