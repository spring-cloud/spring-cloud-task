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

package org.springframework.cloud.task.database.support.db2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.task.executionid.TaskStartApplication;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.cloud.task.repository.database.support.Db2PagingQueryProvider;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.SqlConfig.ErrorMode.CONTINUE_ON_ERROR;

/**
 * @author Tyler Carpenter-Rivers
 * @author Ryan DCruz
 * @see Db2PagingQueryProvider
 */
@SpringBootTest(
	classes = {TaskStartApplication.class, Db2PagingQueryProviderTest.TestConfig.class},
	properties = "spring.cloud.task.events.enabled=false"
)
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext
@Sql(
	scripts = "classpath:/org/springframework/cloud/task/schema-db2.sql",
	config = @SqlConfig(errorMode = CONTINUE_ON_ERROR)
)
class Db2PagingQueryProviderTest {

	@Container
	public static Db2Container db2Container =
		new Db2Container(DockerImageName.parse("ibmcom/db2:11.5.7.0"))
			.withDatabaseName("test")
			.withStartupTimeoutSeconds(30)
			.withExposedPorts(50000);

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", db2Container::getJdbcUrl);
		registry.add("spring.datasource.username", db2Container::getUsername);
		registry.add("spring.datasource.password", db2Container::getPassword);
	}

	@Autowired
	private SimpleTaskExplorer simpleTaskExplorer;

	@Autowired
	private TaskExecutionDao taskExecutionDao;

	@DisplayName("Scenario: The page request size is less than the total number of elements" +
		"Given the total number of elements is 3" +
		"And the page request size is 2" +
		"When the query is executed" +
		"Then the returned page has 2 elements")
	@Test
	void pageRequest() {
		// setup test data
		taskExecutionDao.createTaskExecution(
			UUID.randomUUID().toString(), new Date(), List.of(), UUID.randomUUID().toString());
		taskExecutionDao.createTaskExecution(
			UUID.randomUUID().toString(), new Date(), List.of(), UUID.randomUUID().toString());
		taskExecutionDao.createTaskExecution(
			UUID.randomUUID().toString(), new Date(), List.of(), UUID.randomUUID().toString());

		PageRequest pageRequest = PageRequest.of(0, 2);

		// run subject under test
		Page<TaskExecution> page = simpleTaskExplorer.findAll(pageRequest);

		// assert and verify
		assertThat(page.getTotalElements())
			.as("Expected the total number of elements to exceed the page request size")
			.isGreaterThan(pageRequest.getPageSize());

		assertThat(pageRequest.getPageSize())
			.as("Expected the size of the returned page to equal what was requested")
			.isEqualTo(page.getNumberOfElements());
	}

	@TestConfiguration
	public static class TestConfig {
		@Bean
		@ConfigurationProperties("spring.datasource")
		public DataSource dataSource() {
			return new DriverManagerDataSource();
		}

		@Bean
		public TaskExecutionDaoFactoryBean taskExecutionDaoFactoryBean(DataSource dataSource) {
			return new TaskExecutionDaoFactoryBean(dataSource);
		}

		@Bean
		public TaskExecutionDao taskExecutionDao(TaskExecutionDaoFactoryBean taskExecutionDaoFactoryBean) throws Exception {
			return taskExecutionDaoFactoryBean.getObject();
		}
	}

}
