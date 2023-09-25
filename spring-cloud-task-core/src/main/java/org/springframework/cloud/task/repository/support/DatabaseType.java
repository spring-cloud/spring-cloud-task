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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.support.DatabaseMetaDataCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.StringUtils;

/**
 * Enum representing a database type, such as DB2 or oracle. The type also contains a
 * product name, which is expected to be the same as the product name provided by the
 * database driver's metadata.
 *
 * @author Glenn Renfro
 */
public enum DatabaseType {

	/**
	 * HSQL DB.
	 */
	HSQL("HSQL Database Engine"),

	/**
	 * H2 DB.
	 */
	H2("H2"),

	/**
	 * Oracle DB.
	 */
	ORACLE("Oracle"),

	/**
	 * MySQL DB.
	 */
	MYSQL("MySQL"),

	/**
	 * MySQL DB.
	 */
	MARIADB("MariaDB"),

	/**
	 * PostgreSQL DB.
	 */
	POSTGRES("PostgreSQL"),

	/**
	 * Microsoft SQL Server DB.
	 */
	SQLSERVER("Microsoft SQL Server"),

	/**
	 * DB2 DB.
	 */
	DB2("DB2"),

	/**
	 * DB2VSE DB.
	 */
	DB2VSE("DB2VSE"),

	/**
	 * DB2ZOS DB.
	 */
	DB2ZOS("DB2ZOS"),

	/**
	 * DB2AS400 DB.
	 */
	DB2AS400("DB2AS400");

	private static final Map<String, DatabaseType> dbNameMap;

	static {
		dbNameMap = new HashMap<>();
		for (DatabaseType type : values()) {
			dbNameMap.put(type.getProductName(), type);
		}
	}

	private final String productName;

	DatabaseType(String productName) {
		this.productName = productName;
	}

	/**
	 * Convenience method that pulls a database product name from the DataSource's
	 * metadata.
	 * @param dataSource the datasource used to extact metadata.
	 * @return DatabaseType The database type associated with the datasource.
	 * @throws MetaDataAccessException thrown if failure occurs on metadata lookup.
	 */
	public static DatabaseType fromMetaData(DataSource dataSource) throws SQLException, MetaDataAccessException {
		String databaseProductName = JdbcUtils.extractDatabaseMetaData(dataSource, new DatabaseMetaDataCallback() {

			@Override
			public Object processMetaData(DatabaseMetaData dbmd) throws SQLException, MetaDataAccessException {
				return dbmd.getDatabaseProductName();
			}
		}).toString();
		if (StringUtils.hasText(databaseProductName) && !databaseProductName.equals("DB2/Linux")
				&& databaseProductName.startsWith("DB2")) {
			String databaseProductVersion = JdbcUtils
				.extractDatabaseMetaData(dataSource, new DatabaseMetaDataCallback() {

					@Override
					public Object processMetaData(DatabaseMetaData dbmd) throws SQLException, MetaDataAccessException {
						return dbmd.getDatabaseProductVersion();
					}
				})
				.toString();

			if (databaseProductVersion.startsWith("ARI")) {
				databaseProductName = "DB2VSE";
			}
			else if (databaseProductVersion.startsWith("DSN")) {
				databaseProductName = "DB2ZOS";
			}
			else if (databaseProductName.indexOf("AS") != -1 && (databaseProductVersion.startsWith("QSQ")
					|| databaseProductVersion.substring(databaseProductVersion.indexOf('V'))
						.matches("V\\dR\\d[mM]\\d"))) {
				databaseProductName = "DB2AS400";
			}
			else {
				databaseProductName = JdbcUtils.commonDatabaseName(databaseProductName);
			}
		}
		else {
			if (!databaseProductName.equals(MARIADB.getProductName())) {
				databaseProductName = JdbcUtils.commonDatabaseName(databaseProductName);
			}
		}
		return fromProductName(databaseProductName);
	}

	/**
	 * Static method to obtain a DatabaseType from the provided product name.
	 * @param productName the name of the database.
	 * @return DatabaseType for given product name.
	 * @throws IllegalArgumentException if none is found.
	 */
	public static DatabaseType fromProductName(String productName) {
		if (!dbNameMap.containsKey(productName)) {
			throw new IllegalArgumentException("DatabaseType not found for product name: [" + productName + "]");
		}
		else {
			return dbNameMap.get(productName);
		}
	}

	private String getProductName() {
		return this.productName;
	}

}
