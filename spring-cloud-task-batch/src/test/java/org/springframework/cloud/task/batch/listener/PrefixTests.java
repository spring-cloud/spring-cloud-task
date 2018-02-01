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

package org.springframework.cloud.task.batch.listener;

import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;

import org.h2.tools.Server;
import org.junit.After;
import org.junit.Test;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.cloud.task.batch.configuration.TaskBatchAutoConfiguration;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
public class PrefixTests {

	private final static String DATASOURCE_URL;

	private final static String DATASOURCE_USER_NAME = "SA";

	private final static String DATASOURCE_USER_PASSWORD = "''";

	private final static String DATASOURCE_DRIVER_CLASS_NAME = "org.h2.Driver";

	private static int randomPort;

	static {
		randomPort = SocketUtils.findAvailableTcpPort();
		DATASOURCE_URL = "jdbc:h2:tcp://localhost:" + randomPort + "/mem:dataflow;DB_CLOSE_DELAY=-1;"
				+ "DB_CLOSE_ON_EXIT=FALSE";
	}

	private ConfigurableApplicationContext applicationContext;

	@After
	public void tearDown() {
		if (this.applicationContext != null) {
			this.applicationContext.close();
		}
	}

	@Test
	public void testPrefix() {
		this.applicationContext = SpringApplication.run(new Class[] {
				TaskDBConfiguration.class, JobConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				BatchAutoConfiguration.class,
				TaskBatchAutoConfiguration.class }, new String[] { "--spring.cloud.task.tablePrefix=FOO_" });

		TaskExplorer taskExplorer = this.applicationContext.getBean(TaskExplorer.class);

		Set<Long> jobIds = taskExplorer.getJobExecutionIdsByTaskExecutionId(1);
		assertThat(jobIds.size()).isEqualTo(1);
		assertThat(jobIds.contains(1L));
	}

	@Configuration
	public static class TaskDBConfiguration {

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
			initializeDB();
			return server;
		}

		private void initializeDB() {
			JdbcTemplate template = new JdbcTemplate(dataSourceInit());
			template.execute("CREATE SEQUENCE FOO_SEQ");
			template.execute("CREATE TABLE FOO_EXECUTION  (" +
					"TASK_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY ," +
					"START_TIME TIMESTAMP DEFAULT NULL ," +
					"END_TIME TIMESTAMP DEFAULT NULL ," +
					"TASK_NAME  VARCHAR(100) ," +
					"EXIT_CODE INTEGER ," +
					"EXIT_MESSAGE VARCHAR(2500) ," +
					"ERROR_MESSAGE VARCHAR(2500) ," +
					"LAST_UPDATED TIMESTAMP," +
					"EXTERNAL_EXECUTION_ID VARCHAR(255)," +
					"PARENT_EXECUTION_ID BIGINT" +
					")");
			template.execute("CREATE TABLE FOO_EXECUTION_PARAMS  (" +
					"TASK_EXECUTION_ID BIGINT NOT NULL ," +
					"TASK_PARAM VARCHAR(2500) ," +
					"constraint TASK_EXEC_PARAMS_FK foreign key (TASK_EXECUTION_ID)" +
					"references FOO_EXECUTION(TASK_EXECUTION_ID)" +
					")");
			template.execute("CREATE TABLE FOO_TASK_BATCH (" +
					"  TASK_EXECUTION_ID BIGINT NOT NULL ," +
					"  JOB_EXECUTION_ID BIGINT NOT NULL ," +
					"constraint TASK_EXEC_BATCH_FK foreign key (TASK_EXECUTION_ID)" +
					"references FOO_EXECUTION(TASK_EXECUTION_ID)" +
					")");
		}

		@Bean
		public DataSource dataSource(Server server) {
			return dataSourceInit();
		}

		public DataSource dataSourceInit() {
			DriverManagerDataSource dataSource = new DriverManagerDataSource();
			dataSource.setDriverClassName(DATASOURCE_DRIVER_CLASS_NAME);
			dataSource.setUrl(DATASOURCE_URL);
			dataSource.setUsername(DATASOURCE_USER_NAME);
			dataSource.setPassword(DATASOURCE_USER_PASSWORD);
			return dataSource;
		}

	}

	@Configuration
	@EnableBatchProcessing
	@EnableTask
	public static class JobConfiguration {

		@Autowired
		private JobBuilderFactory jobBuilderFactory;

		@Autowired
		private StepBuilderFactory stepBuilderFactory;

		@Bean
		public Job job() {
			return jobBuilderFactory.get("job")
					.start(stepBuilderFactory.get("step1").tasklet(new Tasklet() {
						@Override
						public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
								throws Exception {
							System.out.println("Executed");
							return RepeatStatus.FINISHED;
						}
					}).build())
					.build();
		}
	}

}
