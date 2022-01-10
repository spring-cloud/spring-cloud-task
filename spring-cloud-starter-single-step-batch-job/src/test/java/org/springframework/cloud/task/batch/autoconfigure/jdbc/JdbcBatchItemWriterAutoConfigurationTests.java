/*
 * Copyright 2020-2020 the original author or authors.
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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.h2.tools.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.item.database.ItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.batch.autoconfigure.SingleStepJobAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class JdbcBatchItemWriterAutoConfigurationTests {

	private final static String DATASOURCE_URL;

	private final static String DATASOURCE_USER_NAME = "SA";

	private final static String DATASOURCE_USER_PASSWORD = "''";

	private final static String DATASOURCE_DRIVER_CLASS_NAME = "org.h2.Driver";

	private static int randomPort;

	static {
		randomPort = SocketUtils.findAvailableTcpPort();
		DATASOURCE_URL = "jdbc:h2:tcp://localhost:" + randomPort
				+ "/mem:dataflow;DB_CLOSE_DELAY=-1;" + "DB_CLOSE_ON_EXIT=FALSE";
	}

	@AfterEach
	public void clearDB() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(DATASOURCE_DRIVER_CLASS_NAME);
		dataSource.setUrl(DATASOURCE_URL);
		dataSource.setUsername(DATASOURCE_USER_NAME);
		dataSource.setPassword(DATASOURCE_USER_PASSWORD);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("TRUNCATE TABLE item");
		jdbcTemplate.execute("DELETE FROM BATCH_STEP_EXECUTION_CONTEXT");
		jdbcTemplate.execute("DELETE FROM BATCH_STEP_EXECUTION");
		jdbcTemplate.execute("DELETE FROM BATCH_JOB_EXECUTION_PARAMS");
		jdbcTemplate.execute("DELETE FROM BATCH_JOB_EXECUTION_CONTEXT");
		jdbcTemplate.execute("DELETE FROM BATCH_JOB_EXECUTION");
		jdbcTemplate.execute("DELETE FROM BATCH_JOB_INSTANCE");
	}

	@Test
	public void baseTest() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(
					TaskLauncherConfiguration.class,
					JdbcBatchItemWriterAutoConfigurationTests.DelimitedJobConfiguration.class
						)
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
								BatchAutoConfiguration.class,
								SingleStepJobAutoConfiguration.class,
								JdbcBatchItemWriterAutoConfiguration.class));
		applicationContextRunner = updatePropertiesForTest(applicationContextRunner);

		runTest(applicationContextRunner);
	}

	@Test
	public void customSqlParameterSourceTest() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(
						TaskLauncherConfiguration.class,
						JdbcBatchItemWriterAutoConfigurationTests.DelimitedDifferentKeyNameJobConfiguration.class,
						CustomSqlParameterSourceProviderConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
								BatchAutoConfiguration.class,
								SingleStepJobAutoConfiguration.class,
								JdbcBatchItemWriterAutoConfiguration.class));
		applicationContextRunner = updatePropertiesForTest(applicationContextRunner);

		runTest(applicationContextRunner);
	}

	@Test
	public void preparedStatementSetterTest() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(
						TaskLauncherConfiguration.class,
						JdbcBatchItemWriterAutoConfigurationTests.DelimitedJobConfiguration.class,
						ItemPreparedStatementSetterConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
								BatchAutoConfiguration.class,
								SingleStepJobAutoConfiguration.class,
								JdbcBatchItemWriterAutoConfiguration.class));
		applicationContextRunner = updatePropertiesForTest(applicationContextRunner);
		runTest(applicationContextRunner);
	}

	private ApplicationContextRunner updatePropertiesForTest(
			ApplicationContextRunner applicationContextRunner) {
		return applicationContextRunner.withPropertyValues("spring.batch.job.jobName=job",
				"spring.batch.job.stepName=step1", "spring.batch.job.chunkSize=5",
				"spring.batch.job.jdbcbatchitemwriter.name=fooWriter",
				"spring.batch.job.jdbcbatchitemwriter.sql=INSERT INTO item (item_name) VALUES (:item_name)",
				"spring.batch.jdbc.initialize-schema=always");
	}

	private void validateResultAndBean(ApplicationContext context) {
		DataSource dataSource = context.getBean(DataSource.class);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		List<Map<String, Object>> result = jdbcTemplate
				.queryForList("SELECT item_name FROM item ORDER BY item_name");
		assertThat(result.size()).isEqualTo(3);

		assertThat(result.get(0).get("item_name")).isEqualTo("bar");
		assertThat(result.get(1).get("item_name")).isEqualTo("baz");
		assertThat(result.get(2).get("item_name")).isEqualTo("foo");

		JdbcBatchItemWriter writer = context.getBean(JdbcBatchItemWriter.class);
		assertThat((Boolean) ReflectionTestUtils.getField(writer, "assertUpdates"))
				.isTrue();
		assertThat((Integer) ReflectionTestUtils.getField(writer, "parameterCount"))
				.isEqualTo(1);
		assertThat((Boolean) ReflectionTestUtils.getField(writer, "usingNamedParameters"))
				.isTrue();
	}

	private void runTest(ApplicationContextRunner applicationContextRunner) {
		applicationContextRunner.run((context) -> {
			JobLauncher jobLauncher = context.getBean(JobLauncher.class);

			Job job = context.getBean(Job.class);

			JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			validateResultAndBean(context);
		});
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
					DriverManagerDataSource dataSource = new DriverManagerDataSource();
					dataSource.setDriverClassName(DATASOURCE_DRIVER_CLASS_NAME);
					dataSource.setUrl(DATASOURCE_URL);
					dataSource.setUsername(DATASOURCE_USER_NAME);
					dataSource.setPassword(DATASOURCE_USER_PASSWORD);
					ClassPathResource setupResource = new ClassPathResource(
							"schema-h2.sql");
					ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator(
							setupResource);
					resourceDatabasePopulator.execute(dataSource);
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

	@Configuration
	@EnableBatchProcessing
	public static class DelimitedJobConfiguration {

		@Bean
		public ListItemReader<Map<String, Object>> itemReader() {

			List<Map<String, Object>> items = new ArrayList<>(3);

			items.add(Collections.singletonMap("item_name", "foo"));
			items.add(Collections.singletonMap("item_name", "bar"));
			items.add(Collections.singletonMap("item_name", "baz"));

			return new ListItemReader<>(items);
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class DelimitedDifferentKeyNameJobConfiguration {

		@Bean
		public ListItemReader<Map<String, Object>> itemReader() {

			List<Map<String, Object>> items = new ArrayList<>(3);

			items.add(Collections.singletonMap("item_foo", "foo"));
			items.add(Collections.singletonMap("item_foo", "bar"));
			items.add(Collections.singletonMap("item_foo", "baz"));

			return new ListItemReader<>(items);
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class CustomSqlParameterSourceProviderConfiguration {

		@Bean
		public ItemSqlParameterSourceProvider<Map<String, Object>> itemSqlParameterSourceProvider() {
			return item -> new MapSqlParameterSource(new HashMap<String, Object>() {
				{
					put("item_name", item.get("item_foo"));
				}
			});
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class ItemPreparedStatementSetterConfiguration {

		@Bean
		public ItemPreparedStatementSetter itemPreparedStatementSetter() {
			return new ItemPreparedStatementSetter() {
				@Override
				public void setValues(Object item, PreparedStatement ps)
						throws SQLException {
					Map<String, Object> mapItem = (Map<String, Object>) item;
					StatementCreatorUtils.setParameterValue(ps, 1,
							SqlTypeValue.TYPE_UNKNOWN, mapItem.get("item_name"));
				}
			};
		}

	}

}
