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

package org.springframework.cloud.task.partitioner;

import java.sql.SQLException;
import javax.sql.DataSource;

import io.spring.PartitionedBatchJobApplication;
import org.h2.tools.Server;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SocketUtils;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TaskPartitionerTests.TaskLauncherConfiguration.class})
public class TaskPartitionerTests {

	private static String DATASOURCE_URL;
	private final static String DATASOURCE_USER_NAME = "SA";
	private final static String DATASOURCE_USER_PASSWORD = "";
	private final static String DATASOURCE_DRIVER_CLASS_NAME = "org.h2.Driver";

	private TaskExplorer taskExplorer;

	@Autowired
	private DataSource dataSource;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		taskExplorer = new SimpleTaskExplorer(new TaskExecutionDaoFactoryBean(dataSource));
	}

	private static int randomPort;

	static {
		randomPort = SocketUtils.findAvailableTcpPort();
		DATASOURCE_URL = "jdbc:h2:tcp://localhost:" + randomPort + "/mem:dataflow;DB_CLOSE_DELAY=-1;"
				+ "DB_CLOSE_ON_EXIT=FALSE";
	}

	@Before
	public void setup() {
		JdbcTemplate template = new JdbcTemplate(this.dataSource);
		template.execute("DROP TABLE IF EXISTS TASK_TASK_BATCH");
		template.execute("DROP TABLE IF EXISTS TASK_SEQ");
		template.execute("DROP TABLE IF EXISTS TASK_EXECUTION_PARAMS");
		template.execute("DROP TABLE IF EXISTS TASK_EXECUTION");
		template.execute("DROP TABLE IF EXISTS BATCH_STEP_EXECUTION_SEQ");
		template.execute("DROP TABLE IF EXISTS BATCH_STEP_EXECUTION_CONTEXT");
		template.execute("DROP TABLE IF EXISTS BATCH_STEP_EXECUTION");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_SEQ");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_SEQ");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_PARAMS");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_CONTEXT");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_EXECUTION");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_INSTANCE");
	}

	@Test
	public void testWithLocalDeployer() throws Exception {
		SpringApplication app = new SpringApplication(PartitionedBatchJobApplication.class);
		app.setAdditionalProfiles("master");
		app.run("--server.port=0", "--spring.datasource.url=" + DATASOURCE_URL,
						"--spring.datasource.username=" + DATASOURCE_USER_NAME,
						"--spring.datasource.driverClassName=" + DATASOURCE_DRIVER_CLASS_NAME,
						"--spring.cloud.deployer.local.use-spring-application-json=false");

		Page<TaskExecution> taskExecutions = taskExplorer.findAll(PageRequest.of(0, 10));
		assertEquals("Five rows are expected", 5, taskExecutions.getTotalElements());
		assertEquals("Only One master is expected", 1, taskExplorer.getTaskExecutionCountByTaskName("Partitioned Batch Job Task:master:0"));
		assertEquals("4 partitions is expected", 4, taskExplorer.getTaskExecutionCountByTaskName("Partitioned Batch Job Task:worker:0"));
		for (TaskExecution taskExecution : taskExecutions) {
			assertEquals("return code should be 0", 0, taskExecution.getExitCode().intValue());
		}
	}

	@Configuration
	public static class TaskLauncherConfiguration {

		@Bean(destroyMethod = "stop")
		public org.h2.tools.Server initH2TCPServer() {
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
