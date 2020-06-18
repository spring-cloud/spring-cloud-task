/*
 * Copyright 2020-2020 the original author or authors.
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

import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author Michael Minella
 * @since 2.3
 */
@Configuration
@EnableConfigurationProperties(JdbcCursorItemReaderProperties.class)
@AutoConfigureAfter(BatchAutoConfiguration.class)
@ConditionalOnProperty(prefix = "spring.batch.job.jdbccursorreader", name = "name")
public class JdbcCursorItemReaderAutoConfiguration {

	private final JdbcCursorItemReaderProperties properties;

	private final DataSource dataSource;

	@Autowired(required = false)
	private PreparedStatementSetter preparedStatementSetter;

	@Autowired(required = false)
	private RowMapper<Map<Object, Object>> rowMapper;

	public JdbcCursorItemReaderAutoConfiguration(
			JdbcCursorItemReaderProperties properties, DataSource dataSource) {
		this.properties = properties;
		this.dataSource = dataSource;
	}

	@Bean
	@ConditionalOnMissingBean
	public JdbcCursorItemReader<Map<Object, Object>> itemReader() {
		return new JdbcCursorItemReaderBuilder<Map<Object, Object>>()
				.name(this.properties.getName())
				.currentItemCount(this.properties.getCurrentItemCount())
				.dataSource(this.dataSource)
				.driverSupportsAbsolute(this.properties.isDriverSupportsAbsolute())
				.fetchSize(this.properties.getFetchSize())
				.ignoreWarnings(this.properties.isIgnoreWarnings())
				.maxItemCount(this.properties.getMaxItemCount())
				.maxRows(this.properties.getMaxRows())
				.queryTimeout(this.properties.getQueryTimeout())
				.saveState(this.properties.isSaveState()).sql(this.properties.getSql())
				.rowMapper(this.rowMapper)
				.preparedStatementSetter(this.preparedStatementSetter)
				.verifyCursorPosition(this.properties.isVerifyCursorPosition())
				.useSharedExtendedConnection(
						this.properties.isUseSharedExtendedConnection())
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public RowMapper<Map<Object, Object>> rowMapper() {
		return new MapRowMapper();
	}

	public static class MapRowMapper implements RowMapper<Map<Object, Object>> {

		@Override
		public Map<Object, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
			Map<Object, Object> item = new HashMap<>(rs.getMetaData().getColumnCount());

			for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
				item.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
			}

			return item;
		}

	}

}
