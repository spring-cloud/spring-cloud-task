/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.launcher;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

import org.h2.tools.Server;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.spi.local.LocalDeployerProperties;
import org.springframework.cloud.deployer.spi.local.LocalTaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.stream.binder.test.junit.rabbit.RabbitTestSupport;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.task.launcher.util.TaskLauncherSinkApplication;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SocketUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TaskLauncherSinkApplication.class, TaskLauncherSinkTests.TaskLauncherConfiguration.class},
	properties = {"maven.remote-repositories.repo1.url=https://repo.spring.io/libs-milestone-local"})
public class TaskLauncherSinkTests {

	private final static int WAIT_INTERVAL = 500;
	private final static int MAX_WAIT_TIME = 10000;
	private final static String URL = "maven://io.spring.cloud:"
			+ "timestamp-task:jar:1.0.0.RC1";
	private final static String DATASOURCE_URL;
	private final static String DATASOURCE_USER_NAME = "SA";
	private final static String DATASOURCE_USER_PASSWORD = "''";
	private final static String DATASOURCE_DRIVER_CLASS_NAME = "org.h2.Driver";
	private final static String TASK_NAME = "TASK_LAUNCHER_SINK_TEST";

	private static int randomPort;

	static {
		randomPort = SocketUtils.findAvailableTcpPort();
		DATASOURCE_URL = "jdbc:h2:tcp://localhost:" + randomPort + "/mem:dataflow;DB_CLOSE_DELAY=-1;"
				+ "DB_CLOSE_ON_EXIT=FALSE";
	}

	@ClassRule
	public static RabbitTestSupport rabbitTestSupport = new RabbitTestSupport();

	@Autowired
	private Sink sink;

	private DataSource dataSource;

	private Map<String, String> properties;

	private TaskExplorer taskExplorer;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		taskExplorer = new SimpleTaskExplorer(new TaskExecutionDaoFactoryBean(dataSource));
	}

	@Before
	public void setup() {
		properties = new HashMap<>();
		properties.put("spring.datasource.url", DATASOURCE_URL);
		properties.put("spring.datasource.username", DATASOURCE_USER_NAME);
		properties.put("spring.datasource.password", DATASOURCE_USER_PASSWORD);
		properties.put("spring.datasource.driverClassName", DATASOURCE_DRIVER_CLASS_NAME);
		properties.put("spring.application.name",TASK_NAME);
		properties.put("spring.cloud.task.initialize.enable", "false");

		JdbcTemplate template = new JdbcTemplate(this.dataSource);
		template.execute("DROP ALL OBJECTS");

		DataSourceInitializer initializer = new DataSourceInitializer();

		initializer.setDataSource(dataSource);
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript(new ClassPathResource("/org/springframework/cloud/task/schema-h2.sql"));
		initializer.setDatabasePopulator(databasePopulator);

		initializer.afterPropertiesSet();
	}

	@Test
	public void testWithLocalDeployer() throws Exception {
		launchTask(URL);
		assertTrue(waitForDBToBePopulated());

		Page<TaskExecution> taskExecutions = taskExplorer.findAll(PageRequest.of(0, 10));
		TaskExecution te = taskExecutions.iterator().next();
		assertEquals("Only one row is expected", 1, taskExecutions.getTotalElements());
		assertEquals("return code should be 0", 0, taskExecutions.iterator().next().getExitCode().intValue());
	}

	private boolean tableExists() throws SQLException {
		boolean result;
		try (
				Connection conn = dataSource.getConnection();
				ResultSet res = conn.getMetaData().getTables(null, null, "TASK_EXECUTION",
						new String[]{"TABLE"})) {
			result = res.next();
		}
		return result;
	}

	private boolean waitForDBToBePopulated() throws Exception {
		boolean isDbPopulated = false;
		for (int waitTime = 0; waitTime <= MAX_WAIT_TIME; waitTime += WAIT_INTERVAL) {
			Thread.sleep(WAIT_INTERVAL);
			if (tableExists() && taskExplorer.getTaskExecutionCount() > 0) {
				isDbPopulated = true;
				break;
			}
		}
		return isDbPopulated;
	}

	private void launchTask(String artifactURL) {

		TaskLaunchRequest request = new TaskLaunchRequest(artifactURL, null,
				this.properties, null, null);
		GenericMessage<TaskLaunchRequest> message = new GenericMessage<>(request);
		this.sink.input().send(message);
	}

	@Configuration
	public static class TaskLauncherConfiguration {
		@Bean
		public TaskLauncher taskLauncher() {
			LocalDeployerProperties props = new LocalDeployerProperties();
			props.setDeleteFilesOnExit(false);

			return new LocalTaskLauncher(props);
		}

		@Bean(destroyMethod = "stop")
		public Server initH2TCPServer() {
			Server server;
			try {
				server = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort",
						String.valueOf(randomPort)).start();
			}
			catch (SQLException e) {
				throw new IllegalStateException(e);
			}
			return server;
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
