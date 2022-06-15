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

package org.springframework.cloud.task.batch.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.task.configuration.DefaultTaskConfigurer;
import org.springframework.cloud.task.configuration.TaskConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Establishes the default {@link DataSource} for the Task when creating a {@link DataSource}
 * for  {@link org.springframework.batch.item.database.JdbcBatchItemWriter}
 * or {@link org.springframework.batch.item.database.JdbcBatchItemWriter}.
 *
 * @author Glenn Renfro
 * @since 3.0
 */
class JDBCSingleStepDataSourceAutoConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public TaskConfigurer myTaskConfigurer(DataSource dataSource) {
		return new DefaultTaskConfigurer(dataSource);
	}

	@ConditionalOnProperty(prefix = "spring.batch.job.jdbcsinglestep.datasource", name = "enable", havingValue = "true", matchIfMissing = true)
	@ConditionalOnMissingBean(name = "springDataSourceProperties")
	@Bean(name = "springDataSourceProperties")
	@ConfigurationProperties("spring.datasource")
	@Primary
	public DataSourceProperties springDataSourceProperties() {
		return new DataSourceProperties();
	}

	@ConditionalOnProperty(prefix = "spring.batch.job.jdbcsinglestep.datasource", name = "enable", havingValue = "true", matchIfMissing = true)
	@Bean(name = "springDataSource")
	@Primary
	public DataSource dataSource(@Qualifier("springDataSourceProperties")DataSourceProperties springDataSourceProperties) {
		DataSource dataSource =  springDataSourceProperties.initializeDataSourceBuilder().build();
		return dataSource;
	}
}
