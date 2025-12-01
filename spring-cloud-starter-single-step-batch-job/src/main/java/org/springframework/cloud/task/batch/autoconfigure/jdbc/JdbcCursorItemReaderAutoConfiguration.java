/*
 * Copyright 2020-present the original author or authors.
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

package org.springframework.cloud.task.batch.autoconfigure.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

/**
 * @author Michael Minella
 * @author Glenn Renfro
 * @since 2.3
 */
@AutoConfiguration
@EnableConfigurationProperties(JdbcCursorItemReaderProperties.class)
@AutoConfigureAfter(BatchAutoConfiguration.class)
@ConditionalOnProperty(prefix = "spring.batch.job.jdbccursoritemreader", name = "name")
@Import(JDBCSingleStepDataSourceAutoConfiguration.class)
public class JdbcCursorItemReaderAutoConfiguration {

	private static final Log logger = LogFactory.getLog(JdbcCursorItemReaderAutoConfiguration.class);

	@Autowired
	ApplicationContext applicationContext;

	private final JdbcCursorItemReaderProperties properties;

	private final DataSource dataSource;

	public JdbcCursorItemReaderAutoConfiguration(JdbcCursorItemReaderProperties properties, DataSource dataSource) {
		this.properties = properties;
		this.dataSource = dataSource;
	}

	@Bean
	@ConditionalOnMissingBean
	public JdbcCursorItemReader<Map<String, Object>> itemReader(
			@Autowired(required = false) RowMapper<Map<String, Object>> rowMapper,
			@Autowired(required = false) PreparedStatementSetter preparedStatementSetter) {
		DataSource readerDataSource = this.dataSource;
		try {
			readerDataSource = this.applicationContext.getBean("jdbcCursorItemReaderSpringDataSource",
					DataSource.class);
		}
		catch (Exception e) {
			logger.info("Using Default Data Source for the JdbcCursorItemReader");

		}
		Assert.state(this.properties.getSql() != null, "sql must not be null");
		Assert.state(this.properties.getName() != null, "name must not be null");
		return new JdbcCursorItemReaderBuilder<Map<String, Object>>().name(this.properties.getName())
			.currentItemCount(this.properties.getCurrentItemCount())
			.dataSource(readerDataSource)
			.driverSupportsAbsolute(this.properties.isDriverSupportsAbsolute())
			.fetchSize(this.properties.getFetchSize())
			.ignoreWarnings(this.properties.isIgnoreWarnings())
			.maxItemCount(this.properties.getMaxItemCount())
			.maxRows(this.properties.getMaxRows())
			.queryTimeout(this.properties.getQueryTimeout())
			.saveState(this.properties.isSaveState())
			.sql(this.properties.getSql())
			.rowMapper(rowMapper)
			.preparedStatementSetter(preparedStatementSetter)
			.verifyCursorPosition(this.properties.isVerifyCursorPosition())
			.useSharedExtendedConnection(this.properties.isUseSharedExtendedConnection())
			.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public RowMapper<Map<String, Object>> rowMapper() {
		return new MapRowMapper();
	}

	@ConditionalOnProperty(prefix = "spring.batch.job.jdbccursoritemreader.datasource", name = "enable",
			havingValue = "true")
	@Bean(name = "jdbcCursorItemReaderDataSourceProperties")
	@ConfigurationProperties("jdbccursoritemreader.datasource")
	public DataSourceProperties jdbcCursorItemReaderDataSourceProperties() {
		return new DataSourceProperties();
	}

	@ConditionalOnProperty(prefix = "spring.batch.job.jdbccursoritemreader.datasource", name = "enable",
			havingValue = "true")
	@Bean(name = "jdbcCursorItemReaderSpringDataSource")
	public DataSource readerDataSource(
			@Qualifier("jdbcCursorItemReaderDataSourceProperties") DataSourceProperties readerDataSourceProperties) {
		DataSource result = readerDataSourceProperties.initializeDataSourceBuilder().build();
		return result;
	}

	public static class MapRowMapper implements RowMapper<Map<String, Object>> {

		@Override
		public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
			Map<String, Object> item = new HashMap<>(rs.getMetaData().getColumnCount());

			for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
				item.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
			}

			return item;
		}

	}

}
