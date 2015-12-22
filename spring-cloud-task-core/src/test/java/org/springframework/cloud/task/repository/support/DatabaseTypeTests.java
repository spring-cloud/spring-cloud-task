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
package org.springframework.cloud.task.repository.support;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.task.repository.support.DatabaseType.HSQL;
import static org.springframework.cloud.task.repository.support.DatabaseType.MYSQL;
import static org.springframework.cloud.task.repository.support.DatabaseType.ORACLE;
import static org.springframework.cloud.task.repository.support.DatabaseType.POSTGRES;
import static org.springframework.cloud.task.repository.support.DatabaseType.fromProductName;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.junit.Test;

/**
 * Tests that the correct database names are selected from datasource metadata.
 *
 * @author Lucas Ward
 * @author Will Schipp
 * @author Glenn Renfro
 *
 */
public class DatabaseTypeTests {

	@Test
	public void testFromProductName() {
		assertEquals(HSQL, fromProductName("HSQL Database Engine"));
		assertEquals(ORACLE, fromProductName("Oracle"));
		assertEquals(POSTGRES, fromProductName("PostgreSQL"));
		assertEquals(MYSQL, fromProductName("MySQL"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidProductName() {
		fromProductName("bad product name");
	}

	@Test
	public void testFromMetaDataForHsql() throws Exception {
		DataSource ds = getMockDataSource("HSQL Database Engine");
		assertEquals(HSQL, DatabaseType.fromMetaData(ds));
	}

	@Test
	public void testFromMetaDataForOracle() throws Exception {
		DataSource ds = getMockDataSource("Oracle");
		assertEquals(ORACLE, DatabaseType.fromMetaData(ds));
	}

	@Test
	public void testFromMetaDataForPostgres() throws Exception {
		DataSource ds = getMockDataSource("PostgreSQL");
		assertEquals(POSTGRES, DatabaseType.fromMetaData(ds));
	}

	@Test
	public void testFromMetaDataForMySQL() throws Exception {
		DataSource ds = getMockDataSource("MySQL");
		assertEquals(MYSQL, DatabaseType.fromMetaData(ds));
	}

	public  DataSource getMockDataSource(String databaseProductName) throws Exception {
		DatabaseMetaData dmd = mock(DatabaseMetaData.class);
		DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		when(ds.getConnection()).thenReturn(con);
		when(con.getMetaData()).thenReturn(dmd);
		when(dmd.getDatabaseProductName()).thenReturn(databaseProductName);
		return ds;
	}

	public DataSource getMockDataSource(Exception e) throws Exception {
		DataSource ds = mock(DataSource.class);
		when(ds.getConnection()).thenReturn(null);
		return ds;
	}
}
