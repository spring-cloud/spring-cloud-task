/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.task.initializer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.h2.tools.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.task.executionid.TaskStartApplication;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { TaskInitializerTests.TaskLauncherConfiguration.class })
public class TaskInitializerTests {

	private final static int WAIT_INTERVAL = 500;

	private final static int MAX_WAIT_TIME = 5000;

	private final static String URL = "maven://io.spring.cloud:"
			+ "timestamp-task:jar:1.1.0.RELEASE";

	private final static String DATASOURCE_URL;

	private final static String DATASOURCE_USER_NAME = "SA";

	private final static String DATASOURCE_USER_PASSWORD = "''";

	private final static String DATASOURCE_DRIVER_CLASS_NAME = "org.h2.Driver";

	private final static String TASK_NAME = "TASK_LAUNCHER_SINK_TEST";

	private static int randomPort;

	static {
		randomPort = SocketUtils.findAvailableTcpPort();
		DATASOURCE_URL = "jdbc:h2:tcp://localhost:" + randomPort
				+ "/mem:dataflow;DB_CLOSE_DELAY=-1;" + "DB_CLOSE_ON_EXIT=FALSE";
	}

	private DataSource dataSource;

	private TaskExplorer taskExplorer;

	private ConfigurableApplicationContext applicationContext;

	@AfterEach
	public void tearDown() {
		if (this.applicationContext != null && this.applicationContext.isActive()) {
			this.applicationContext.close();
		}
	}

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean(
				dataSource);
		this.taskExplorer = new SimpleTaskExplorer(factoryBean);
	}

	@BeforeEach
	public void setup() {

		JdbcTemplate template = new JdbcTemplate(this.dataSource);
		template.execute("DROP TABLE IF EXISTS TASK_TASK_BATCH");
		template.execute("DROP TABLE IF EXISTS TASK_SEQ");
		template.execute("DROP TABLE IF EXISTS TASK_EXECUTION_PARAMS");
		template.execute("DROP TABLE IF EXISTS TASK_EXECUTION");
		template.execute("DROP TABLE IF EXISTS TASK_LOCK");
		template.execute("DROP TABLE IF EXISTS BATCH_STEP_EXECUTION_SEQ");
		template.execute("DROP TABLE IF EXISTS BATCH_STEP_EXECUTION_CONTEXT");
		template.execute("DROP TABLE IF EXISTS BATCH_STEP_EXECUTION");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_SEQ");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_SEQ");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_PARAMS");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_CONTEXT");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_EXECUTION");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_INSTANCE");
		template.execute("DROP SEQUENCE IF EXISTS TASK_SEQ");
	}

	@Test
	public void testNotInitialized() throws Exception {
		SpringApplication myapp = new SpringApplication(TaskStartApplication.class);
		String[] properties = { "--spring.cloud.task.initialize-enabled=false" };
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() -> {
			this.applicationContext = myapp.run(properties);
		});
	}

	@Test
	public void testWithInitialized() throws Exception {
		SpringApplication myapp = new SpringApplication(TaskStartApplication.class);
		String[] properties = { "--spring.cloud.task.initialize-enabled=true" };
		this.applicationContext = myapp.run(properties);
		assertThat(waitForDBToBePopulated()).isTrue();

		Page<TaskExecution> taskExecutions = this.taskExplorer
				.findAll(PageRequest.of(0, 10));
		TaskExecution te = taskExecutions.iterator().next();
		assertThat(taskExecutions.getTotalElements()).as("Only one row is expected")
				.isEqualTo(1);
		assertThat(taskExecutions.iterator().next().getExitCode().intValue())
				.as("return code should be 0").isEqualTo(0);
	}

	@Test
	public void testNotInitializedOriginalProperty() throws Exception {
		SpringApplication myapp = new SpringApplication(TaskStartApplication.class);
		String[] properties = { "--spring.cloud.task.initialize.enable=false" };
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() -> {
			this.applicationContext = myapp.run(properties);
		});
	}

	@Test
	public void testWithInitializedOriginalProperty() throws Exception {
		SpringApplication myapp = new SpringApplication(TaskStartApplication.class);
		String[] properties = { "--spring.cloud.task.initialize.enable=true" };
		this.applicationContext = myapp.run(properties);
		assertThat(waitForDBToBePopulated()).isTrue();

		Page<TaskExecution> taskExecutions = this.taskExplorer
				.findAll(PageRequest.of(0, 10));
		TaskExecution te = taskExecutions.iterator().next();
		assertThat(taskExecutions.getTotalElements()).as("Only one row is expected")
				.isEqualTo(1);
		assertThat(taskExecutions.iterator().next().getExitCode().intValue())
				.as("return code should be 0").isEqualTo(0);
	}

	private boolean tableExists() throws SQLException {
		boolean result;
		try (Connection conn = this.dataSource.getConnection();
				ResultSet res = conn.getMetaData().getTables(null, null, "TASK_EXECUTION",
						new String[] { "TABLE" })) {
			result = res.next();
		}
		return result;
	}

	private boolean waitForDBToBePopulated() throws Exception {
		boolean isDbPopulated = false;
		for (int waitTime = 0; waitTime <= MAX_WAIT_TIME; waitTime += WAIT_INTERVAL) {
			Thread.sleep(WAIT_INTERVAL);
			if (tableExists() && this.taskExplorer.getTaskExecutionCount() > 0) {
				isDbPopulated = true;
				break;
			}
		}
		return isDbPopulated;
	}

	@Configuration
	public static class TaskLauncherConfiguration {

		private static Server defaultServer;

		@Bean
		public Server initH2TCPServer() {
			Server server = null;
			try {
				if (defaultServer == null) {
					server = Server.createTcpServer("-ifNotExists", "-tcp",
							"-tcpAllowOthers", "-tcpPort", String.valueOf(randomPort))
							.start();
					defaultServer = server;
				}
			}
			catch (SQLException e) {
				throw new IllegalStateException(e);
			}
			return defaultServer;
		}

		@Bean
		public DataSource dataSource() {
			DriverManagerDataSource dataSource = new DriverManagerDataSource();
			dataSource.setDriverClassName(DATASOURCE_DRIVER_CLASS_NAME);
			dataSource.setUrl(DATASOURCE_URL);
			dataSource.setUsername(DATASOURCE_USER_NAME);
			dataSource.setPassword(DATASOURCE_USER_PASSWORD);
			return dataSource;
		}

	}

}
