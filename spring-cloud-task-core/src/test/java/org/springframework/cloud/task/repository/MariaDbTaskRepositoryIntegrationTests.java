/*
 * Copyright 2022-present the original author or authors.
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

package org.springframework.cloud.task.repository;

import javax.sql.DataSource;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.MariaDbDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("DockerRequired")
@Testcontainers
@SpringJUnitConfig
public class MariaDbTaskRepositoryIntegrationTests {

	private static final DockerImageName MARIADB_IMAGE = DockerImageName.parse("mariadb:10.9.3");

	/**
	 * Provide a mariadb test container for tests.
	 */
	@Container
	public static MariaDBContainer mariaDBContainer = new MariaDBContainer(MARIADB_IMAGE);

	@Test
	public void testTaskExplorer() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withUserConfiguration(MariaDbTaskRepositoryIntegrationTests.TestConfiguration.class);

		applicationContextRunner.run((context -> {
			TaskExplorer taskExplorer = context.getBean(TaskExplorer.class);
			assertThat(taskExplorer.getTaskExecutionCount()).isOne();
		}));
		applicationContextRunner.run((context -> {
			TaskExplorer taskExplorer = context.getBean(TaskExplorer.class);
			assertThat(taskExplorer.getTaskExecutionCount()).isEqualTo(2);
		}));
	}

	@EnableTask
	@ImportAutoConfiguration(SimpleTaskAutoConfiguration.class)
	static class TestConfiguration {

		public static boolean firstTime = true;

		@Bean
		public DataSource dataSource() throws Exception {
			MariaDbDataSource datasource = new MariaDbDataSource();
			datasource.setUrl(mariaDBContainer.getJdbcUrl());
			datasource.setUser(mariaDBContainer.getUsername());
			datasource.setPassword(mariaDBContainer.getPassword());
			if (firstTime) {
				ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
				databasePopulator
					.addScript(new ClassPathResource("/org/springframework/cloud/task/schema-mariadb.sql"));
				databasePopulator.execute(datasource);
				firstTime = false;
			}
			return datasource;
		}

	}

}
