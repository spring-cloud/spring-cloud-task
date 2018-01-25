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

package org.springframework.cloud.task.executionid;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.h2.tools.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SocketUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TaskStartTests.TaskLauncherConfiguration.class})
public class TaskStartTests {

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
		DATASOURCE_URL = "jdbc:h2:tcp://localhost:" + randomPort + "/mem:dataflow;DB_CLOSE_DELAY=-1;"
				+ "DB_CLOSE_ON_EXIT=FALSE";
	}

	private DataSource dataSource;

	private Map<String, String> properties;

	private TaskExplorer taskExplorer;

	private TaskRepository taskRepository;

	private ConfigurableApplicationContext applicationContext;

	@After
	public void tearDown() {
		if (this.applicationContext != null && this.applicationContext.isActive() ) {
			this.applicationContext.close();
		}
	}

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean(dataSource);
		taskExplorer = new SimpleTaskExplorer(factoryBean);
		taskRepository = new SimpleTaskRepository(factoryBean);
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

		DataSourceInitializer initializer = new DataSourceInitializer();

		initializer.setDataSource(dataSource);
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript(new ClassPathResource("/org/springframework/cloud/task/schema-h2.sql"));
		initializer.setDatabasePopulator(databasePopulator);
		initializer.afterPropertiesSet();
	}

	@Test
	public void testWithGeneratedTaskExecution() throws Exception {
		taskRepository.createTaskExecution();
		assertEquals("Only one row is expected", 1, taskExplorer.getTaskExecutionCount());
		this.applicationContext = getTaskApplication(1).run(new String[0]);
		assertTrue(waitForDBToBePopulated());

		Page<TaskExecution> taskExecutions = taskExplorer.findAll(PageRequest.of(0, 10));
		TaskExecution te = taskExecutions.iterator().next();
		assertEquals("Only one row is expected", 1, taskExecutions.getTotalElements());
		assertEquals("return code should be 0", 0, taskExecutions.iterator().next().getExitCode().intValue());
	}

	@Test
	public void testWithGeneratedTaskExecutionWithName() throws Exception {
		final String TASK_EXECUTION_NAME = "PRE-EXECUTION-TEST-NAME";
		taskRepository.createTaskExecution(TASK_EXECUTION_NAME);
		assertEquals("Only one row is expected", 1, taskExplorer.getTaskExecutionCount());
		assertEquals(TASK_EXECUTION_NAME, taskExplorer.getTaskExecution(1).getTaskName());

		this.applicationContext = getTaskApplication(1).run(new String[0]);
		assertTrue(waitForDBToBePopulated());

		Page<TaskExecution> taskExecutions = taskExplorer.findAll(PageRequest.of(0, 10));
		TaskExecution te = taskExecutions.iterator().next();
		assertEquals("Only one row is expected", 1, taskExecutions.getTotalElements());
		assertEquals("return code should be 0", 0, taskExecutions.iterator().next().getExitCode().intValue());
		assertEquals("batchEvents", taskExplorer.getTaskExecution(1).getTaskName());
	}

	@Test(expected = ApplicationContextException.class)
	public void testWithNoTaskExecution() throws Exception {
		this.applicationContext = getTaskApplication(55).run(new String[0]);
	}

	@Test(expected = ApplicationContextException.class)
	public void testCompletedTaskExecution() throws Exception {
		taskRepository.createTaskExecution();
		assertEquals("Only one row is expected", 1, taskExplorer.getTaskExecutionCount());
		taskRepository.completeTaskExecution(1, 0, new Date(),"");
		this.applicationContext = getTaskApplication(1).run(new String[0]);
	}

	@Test
	public void testDuplicateTaskExecutionWithSingleInstanceEnabled() throws Exception {
		String params[] = {"--spring.cloud.task.singleInstanceEnabled=true",
				"--spring.cloud.task.name=foo"};
		boolean testFailed = false;
		try {
			taskRepository.createTaskExecution();
			assertEquals("Only one row is expected", 1, taskExplorer.getTaskExecutionCount());
			enableLock("foo");
			getTaskApplication(1).run(params);
		}
		catch (ApplicationContextException taskException) {
			assertEquals("Failed to start bean 'taskLifecycleListener'; nested " +
							"exception is org.springframework.cloud.task." +
							"listener.TaskExecutionException: Failed to process " +
							"@BeforeTask or @AfterTask annotation because: Task with name \"foo\" is already running.",
					taskException.getMessage());
			testFailed = true;
		}
		assertTrue("Expected TaskExecutionException for because  of " +
				"singleInstanceEnabled is enabled", testFailed);

	}

	@Test
	public void testDuplicateTaskExecutionWithSingleInstanceDisabled() throws Exception {
		taskRepository.createTaskExecution();
		TaskExecution execution = taskRepository.createTaskExecution();
		taskRepository.startTaskExecution(execution.getExecutionId(), "bar",
				new Date(), new ArrayList<>(), "");
		String params[] = {"--spring.cloud.task.name=bar"};
		enableLock("bar");
		this.applicationContext = getTaskApplication(1).run(params);
		assertTrue(waitForDBToBePopulated());
	}

	private SpringApplication getTaskApplication(Integer executionId) {
		SpringApplication myapp = new SpringApplication(TaskStartApplication.class);
		Map<String,Object> myMap = new HashMap<>();
		ConfigurableEnvironment environment = new StandardEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		myMap.put("spring.cloud.task.executionid", executionId);
		propertySources.addFirst(new MapPropertySource("EnvrionmentTestPropsource", myMap));
		myapp.setEnvironment(environment);
		return myapp;
	}

	private boolean tableExists() throws SQLException {
		boolean result;
		try (Connection conn = dataSource.getConnection();
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

	private void enableLock(String lockKey) {
		SimpleJdbcInsert taskLockInsert = new SimpleJdbcInsert(this.dataSource).withTableName("TASK_LOCK");
		Map<String, Object> taskLockParams = new HashMap<>();
		taskLockParams.put("LOCK_KEY", UUID.nameUUIDFromBytes(lockKey.getBytes()).toString());
		taskLockParams.put("REGION", "DEFAULT");
		taskLockParams.put("CLIENT_ID", "aClientID");
		taskLockParams.put("CREATED_DATE", new Date());
		taskLockInsert.execute(taskLockParams);
	}

	@Configuration
	public static class TaskLauncherConfiguration {
		private static Server defaultServer;

		@Bean()
		public Server initH2TCPServer() {
			Server server = null;
			try {
				if(defaultServer == null) {
					server = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort",
							String.valueOf(randomPort)).start();
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
