/*
 *  Copyright 2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.configuration.SimpleTaskConfiguration;
import org.springframework.cloud.task.configuration.TaskConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Verifies that TaskRepositoryInitializer creates tables if a {@link TaskConfigurer}
 * has a {@link DataSource}.
 *
 * @author Glenn Renfro
 * @since 2.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {SimpleTaskConfiguration.class,
		EmbeddedDataSourceConfiguration.class})
public class TaskRepositoryInitializerDefaultTaskConfigurerTests {

	@Autowired
	private DataSource dataSource;

	@Test
	public void testTablesCreated() throws Exception {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		List<Map<String, Object>> rows= jdbcTemplate.queryForList("SHOW TABLES");
		assertThat(rows.size()).isEqualTo(4);
	}
}
