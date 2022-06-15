/*
 * Copyright 2020-2022 the original author or authors.
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

import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.item.database.ItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Autconfiguration for a {@code JdbcBatchItemWriter}.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 * @since 2.3
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JdbcBatchItemWriterProperties.class)
@AutoConfigureAfter(BatchAutoConfiguration.class)
@Import(JDBCSingleStepDataSourceAutoConfiguration.class)
public class JdbcBatchItemWriterAutoConfiguration {

	private static final Log logger = LogFactory
		.getLog(JdbcBatchItemWriterAutoConfiguration.class);

	@Autowired(required = false)
	private ItemPreparedStatementSetter itemPreparedStatementSetter;

	@Autowired(required = false)
	private ItemSqlParameterSourceProvider itemSqlParameterSourceProvider;

	@Autowired
	ApplicationContext applicationContext;

	private JdbcBatchItemWriterProperties properties;

	private DataSource dataSource;

	public JdbcBatchItemWriterAutoConfiguration(DataSource dataSource,
			JdbcBatchItemWriterProperties properties) {
		this.dataSource = dataSource;
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.batch.job.jdbcbatchitemwriter", name = "name")
	public JdbcBatchItemWriter<Map<String, Object>> itemWriter() {
		DataSource writerDataSource = this.dataSource;
		try {
			writerDataSource = this.applicationContext.getBean("jdbcBatchItemWriterSpringDataSource", DataSource.class);
		}
		catch (Exception ex) {
			logger.info("Using Default Data Source for the JdbcBatchItemWriter");
		}

		JdbcBatchItemWriterBuilder<Map<String, Object>> jdbcBatchItemWriterBuilder = new JdbcBatchItemWriterBuilder<Map<String, Object>>()
				.dataSource(writerDataSource).sql(this.properties.getSql());
		if (this.itemPreparedStatementSetter != null) {
			jdbcBatchItemWriterBuilder
					.itemPreparedStatementSetter(this.itemPreparedStatementSetter);
		}
		else if (this.itemSqlParameterSourceProvider != null) {
			jdbcBatchItemWriterBuilder
					.itemSqlParameterSourceProvider(this.itemSqlParameterSourceProvider);
		}
		else {
			jdbcBatchItemWriterBuilder.columnMapped();
		}
		jdbcBatchItemWriterBuilder.assertUpdates(this.properties.isAssertUpdates());
		return jdbcBatchItemWriterBuilder.build();
	}

	@ConditionalOnProperty(prefix = "spring.batch.job.jdbcbatchitemwriter.datasource", name = "enable", havingValue = "true")
	@Bean(name = "jdbcBatchItemWriterDataSourceProperties")
	@ConfigurationProperties("jdbcbatchitemwriter.datasource")
	public DataSourceProperties jdbcBatchItemWriterDataSourceProperties() {
		return new DataSourceProperties();
	}

	@ConditionalOnProperty(prefix = "spring.batch.job.jdbcbatchitemwriter.datasource", name = "enable", havingValue = "true")
	@Bean(name = "jdbcBatchItemWriterSpringDataSource")
	public DataSource writerDataSource(@Qualifier("jdbcBatchItemWriterDataSourceProperties") DataSourceProperties writerDataSourceProperties) {
		DataSource result =  writerDataSourceProperties.initializeDataSourceBuilder().build();
		return result;
	}
}
