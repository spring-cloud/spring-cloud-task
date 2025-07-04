/*
 * Copyright 2018-2025 the original author or authors.
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

package org.springframework.cloud.task;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.configuration.DefaultTaskConfigurer;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.cloud.task.configuration.SingleTaskConfiguration;
import org.springframework.cloud.task.configuration.TaskConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Verifies that TaskRepositoryInitializer does not create tables if a
 * {@link TaskConfigurer} has no {@link DataSource}.
 *
 * @author Glenn Renfro
 * @since 2.0.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SimpleTaskAutoConfiguration.class, SingleTaskConfiguration.class,
		EmbeddedDataSourceConfiguration.class, DefaultTaskConfigurer.class })
public class TaskRepositoryInitializerNoDataSourceTaskConfigurerTests {

	@Autowired
	private DataSource dataSource;

	@Test
	public void testNoTablesCreated() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSource);
		List<Map<String, Object>> rows = jdbcTemplate.queryForList("SHOW TABLES");
		assertThat(rows.size()).isEqualTo(0);
	}

}
