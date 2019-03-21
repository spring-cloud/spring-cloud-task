/*
 * Copyright 2015 the original author or authors.
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


import static org.springframework.cloud.task.repository.support.DatabaseType.HSQL;
import static org.springframework.cloud.task.repository.support.DatabaseType.H2;
import static org.springframework.cloud.task.repository.support.DatabaseType.MYSQL;
import static org.springframework.cloud.task.repository.support.DatabaseType.ORACLE;
import static org.springframework.cloud.task.repository.support.DatabaseType.POSTGRES;
import static org.springframework.cloud.task.repository.support.DatabaseType.SQLSERVER;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.item.database.Order;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cloud.task.repository.database.PagingQueryProvider;
import org.springframework.cloud.task.repository.support.DatabaseType;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory bean for {@link PagingQueryProvider} interface. The database type
 * will be determined from the data source if not provided explicitly. Valid
 * types are given by the {@link DatabaseType} enum.
 *
 * @author Glenn Renfro
 */
public class SqlPagingQueryProviderFactoryBean implements FactoryBean<PagingQueryProvider> {

	private DataSource dataSource;

	private String databaseType;

	private String fromClause;

	private String whereClause;

	private String selectClause;

	private Map<String, Order> sortKeys;

	private Map<DatabaseType, AbstractSqlPagingQueryProvider> providers = new HashMap<DatabaseType, AbstractSqlPagingQueryProvider>();


	{
		providers.put(HSQL, new HsqlPagingQueryProvider());
		providers.put(H2, new H2PagingQueryProvider());
		providers.put(MYSQL, new MySqlPagingQueryProvider());
		providers.put(POSTGRES, new PostgresPagingQueryProvider());
		providers.put(ORACLE, new OraclePagingQueryProvider());
		providers.put(SQLSERVER, new SqlServerPagingQueryProvider());
	}

	/**
	 * @param databaseType the databaseType to set
	 */
	public void setDatabaseType(String databaseType) {
		Assert.hasText(databaseType, "databaseType must not be empty nor null");
		this.databaseType = databaseType;
	}

	/**
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(DataSource dataSource) {
		Assert.notNull(dataSource, "dataSource must not be null");
		this.dataSource = dataSource;
	}

	/**
	 * @param fromClause the fromClause to set
	 */
	public void setFromClause(String fromClause) {
		Assert.hasText(fromClause, "fromClause must not be empty nor null");
		this.fromClause = fromClause;
	}

	/**
	 * @param whereClause the whereClause to set
	 */
	public void setWhereClause(String whereClause) {
		this.whereClause = whereClause;
	}

	/**
	 * @param selectClause the selectClause to set
	 */
	public void setSelectClause(String selectClause) {
		Assert.hasText(selectClause, "selectClause must not be empty nor null");
		this.selectClause = selectClause;
	}

	/**
	 * @param sortKeys the sortKeys to set
	 */
	public void setSortKeys(Map<String, Order> sortKeys) {
		this.sortKeys = sortKeys;
	}

	/**
	 * Get a {@link PagingQueryProvider} instance using the provided properties
	 * and appropriate for the given database type.
	 *
	 * @see FactoryBean#getObject()
	 */
	@Override
	public PagingQueryProvider getObject() throws Exception {

		DatabaseType type;
		try {
			type = databaseType != null ? DatabaseType.valueOf(databaseType.toUpperCase()) : DatabaseType
					.fromMetaData(dataSource);
		}
		catch (MetaDataAccessException e) {
			throw new IllegalArgumentException(
					"Could not inspect meta data for database type.  You have to supply it explicitly.", e);
		}

		AbstractSqlPagingQueryProvider provider = providers.get(type);
		Assert.state(provider != null, "Should not happen: missing PagingQueryProvider for DatabaseType=" + type);

		provider.setFromClause(fromClause);
		provider.setWhereClause(whereClause);
		provider.setSortKeys(sortKeys);
		if (StringUtils.hasText(selectClause)) {
			provider.setSelectClause(selectClause);
		}
		provider.init(dataSource);

		return provider;

	}

	/**
	 * Always returns {@link PagingQueryProvider}.
	 *
	 * @see FactoryBean#getObjectType()
	 */
	@Override
	public Class<PagingQueryProvider> getObjectType() {
		return PagingQueryProvider.class;
	}

	/**
	 * Always returns true.
	 *
	 * @see FactoryBean#isSingleton()
	 */
	@Override
	public boolean isSingleton() {
		return true;
	}
}
