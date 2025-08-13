/*
 * Copyright 2020-present the original author or authors.
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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.h2.tools.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.batch.item.util.ExecutionContextUserSupport;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.batch.autoconfigure.SingleStepJobAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Michael Minella
 * @author Glenn Renfro
 */
public class JdbcCursorItemReaderAutoConfigurationTests {

	private final static String DATASOURCE_URL;

	private final static String DATASOURCE_USER_NAME = "SA";

	private final static String DATASOURCE_USER_PASSWORD = "''";

	private final static String DATASOURCE_DRIVER_CLASS_NAME = "org.h2.Driver";

	private static int randomPort;

	static {
		randomPort = TestSocketUtils.findAvailableTcpPort();
		DATASOURCE_URL = "jdbc:h2:tcp://localhost:" + randomPort + "/mem:dataflow;DB_CLOSE_DELAY=-1;"
				+ "DB_CLOSE_ON_EXIT=FALSE";
	}

	@AfterAll
	public static void clearDB() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(DATASOURCE_DRIVER_CLASS_NAME);
		dataSource.setUrl(DATASOURCE_URL);
		dataSource.setUsername(DATASOURCE_USER_NAME);
		dataSource.setPassword(DATASOURCE_USER_PASSWORD);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("TRUNCATE TABLE item");
		jdbcTemplate.execute("DROP TABLE BATCH_JOB_EXECUTION CASCADE");
		jdbcTemplate.execute("DROP TABLE BATCH_JOB_INSTANCE CASCADE");
	}

	@Test
	public void testIntegration() {
		ApplicationContextRunner applicationContextRunner = applicationContextRunner().withPropertyValues(
				"spring.batch.job.jobName=integrationJob", "spring.batch.job.stepName=step1",
				"spring.batch.job.chunkSize=5", "spring.batch.job.jdbccursoritemreader.name=fooReader",
				"spring.batch.job.jdbccursoritemreader.sql=select item_name from item",
				"spring.batch.jdbc.initialize-schema=always",
				"spring.batch.job.jdbcsinglestep.datasource.enable=false");

		applicationContextRunner.run((context) -> {
			JobOperator jobOperator = context.getBean(JobOperator.class);

			Job job = context.getBean(Job.class);

			JobExecution jobExecution = jobOperator.start(job, new JobParameters());

			JobRepository jobRepository = context.getBean(JobRepository.class);

			while (jobRepository.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			List<Map<String, Object>> items = context.getBean(ListItemWriter.class).getWrittenItems();

			assertThat(items.size()).isEqualTo(3);
			assertThat(items.get(0).get("ITEM_NAME")).isEqualTo("foo");
			assertThat(items.get(1).get("ITEM_NAME")).isEqualTo("bar");
			assertThat(items.get(2).get("ITEM_NAME")).isEqualTo("baz");
			assertThatThrownBy(() -> context.getBean("readerSpringDataSource"))
				.isInstanceOf(NoSuchBeanDefinitionException.class)
				.hasMessageContaining("No bean named 'readerSpringDataSource' available");
		});
	}

	private ApplicationContextRunner applicationContextRunner() {
		return new ApplicationContextRunner()
			.withUserConfiguration(TaskLauncherConfiguration.class, BaseConfiguration.class)
			.withConfiguration(
					AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class, BatchAutoConfiguration.class,
							SingleStepJobAutoConfiguration.class, JdbcCursorItemReaderAutoConfiguration.class));
	}

	@Test
	public void testIntegrationReaderDataSourceEnabled() {
		ApplicationContextRunner applicationContextRunner = applicationContextRunner().withPropertyValues(
				"spring.batch.job.jobName=integrationReaderJob", "spring.batch.job.stepName=step1",
				"spring.batch.job.chunkSize=5", "spring.batch.job.jdbccursoritemreader.name=fooReader",
				"spring.batch.job.jdbccursoritemreader.sql=select item_name from item",
				"spring.batch.jdbc.initialize-schema=always", "spring.batch.job.jdbcsinglestep.datasource.enable=false",
				"spring.batch.job.jdbccursoritemreader.datasource.enable=true",
				"jdbccursoritemreader.datasource.url=" + DATASOURCE_URL,
				"jdbccursoritemreader.datasource.username=" + DATASOURCE_USER_NAME,
				"jdbccursoritemreader.datasource.password=" + DATASOURCE_USER_PASSWORD,
				"jdbccursoritemreader.datasource.driverClassName=" + DATASOURCE_DRIVER_CLASS_NAME);

		applicationContextRunner.run((context) -> {
			JobOperator jobOperator = context.getBean(JobOperator.class);

			Job job = context.getBean(Job.class);

			JobExecution jobExecution = jobOperator.start(job, new JobParameters());

			JobRepository jobRepository = context.getBean(JobRepository.class);

			while (jobRepository.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			List<Map<String, Object>> items = context.getBean(ListItemWriter.class).getWrittenItems();

			assertThat(items.size()).isEqualTo(3);
			assertThat(items.get(0).get("ITEM_NAME")).isEqualTo("foo");
			assertThat(items.get(1).get("ITEM_NAME")).isEqualTo("bar");
			assertThat(items.get(2).get("ITEM_NAME")).isEqualTo("baz");
			assertThat(context.getBean("jdbcCursorItemReaderSpringDataSource")).isNotNull();
		});
	}

	@Test
	public void testCustomRowMapper() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TaskLauncherConfiguration.class, RowMapperConfiguration.class)
			.withConfiguration(
					AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class, BatchAutoConfiguration.class,
							SingleStepJobAutoConfiguration.class, JdbcCursorItemReaderAutoConfiguration.class))
			.withPropertyValues("spring.batch.job.jobName=rowMapperJob", "spring.batch.job.stepName=step1",
					"spring.batch.job.chunkSize=5", "spring.batch.job.jdbccursoritemreader.name=fooReader",
					"spring.batch.job.jdbccursoritemreader.sql=select * from item",
					"spring.batch.jdbc.initialize-schema=always",
					"spring.batch.job.jdbcsinglestep.datasource.enable=false");

		applicationContextRunner.run((context) -> {
			JobOperator jobOperator = context.getBean(JobOperator.class);

			Job job = context.getBean(Job.class);

			JobExecution jobExecution = jobOperator.start(job, new JobParameters());

			JobRepository jobRepository = context.getBean(JobRepository.class);

			while (jobRepository.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			List<Map<String, Object>> items = context.getBean(ListItemWriter.class).getWrittenItems();

			assertThat(items.size()).isEqualTo(3);
			assertThat(items.get(0).get("item")).isEqualTo("foo");
			assertThat(items.get(1).get("item")).isEqualTo("bar");
			assertThat(items.get(2).get("item")).isEqualTo("baz");
		});
	}

	@Test
	public void testRoseyScenario() {
		final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TaskLauncherConfiguration.class, BaseConfiguration.class)
			.withConfiguration(
					AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class, BatchAutoConfiguration.class,
							SingleStepJobAutoConfiguration.class, JdbcCursorItemReaderAutoConfiguration.class))
			.withPropertyValues("spring.batch.job.jobName=roseyJob", "spring.batch.job.stepName=step1",
					"spring.batch.job.chunkSize=5", "spring.batch.job.jdbccursoritemreader.saveState=false",
					"spring.batch.job.jdbccursoritemreader.name=fooReader",
					"spring.batch.job.jdbccursoritemreader.maxItemCount=15",
					"spring.batch.job.jdbccursoritemreader.currentItemCount=2",
					"spring.batch.job.jdbccursoritemreader.fetchSize=4",
					"spring.batch.job.jdbccursoritemreader.maxRows=6",
					"spring.batch.job.jdbccursoritemreader.queryTimeout=8",
					"spring.batch.job.jdbccursoritemreader.ignoreWarnings=true",
					"spring.batch.job.jdbccursoritemreader.verifyCursorPosition=true",
					"spring.batch.job.jdbccursoritemreader.driverSupportsAbsolute=true",
					"spring.batch.job.jdbccursoritemreader.useSharedExtendedConnection=true",
					"spring.batch.job.jdbccursoritemreader.sql=select * from foo");

		applicationContextRunner.run((context) -> {

			JdbcCursorItemReader<Map<String, Object>> itemReader = context.getBean(JdbcCursorItemReader.class);

			validateBean(itemReader);
		});
	}

	private void validateBean(JdbcCursorItemReader itemReader) {
		assertThat(itemReader.getSql()).isEqualTo("select * from foo");
		assertThat(itemReader.getDataSource()).isNotNull();
		assertThat((Boolean) ReflectionTestUtils.getField(itemReader, "saveState")).isFalse();
		assertThat(ReflectionTestUtils.getField(
				(ExecutionContextUserSupport) ReflectionTestUtils.getField(itemReader, "executionContextUserSupport"),
				"name"))
			.isEqualTo("fooReader");
		assertThat((Integer) ReflectionTestUtils.getField(itemReader, "maxItemCount")).isEqualTo(15);
		assertThat((Integer) ReflectionTestUtils.getField(itemReader, "currentItemCount")).isEqualTo(2);
		assertThat((Integer) ReflectionTestUtils.getField(itemReader, "fetchSize")).isEqualTo(4);
		assertThat((Integer) ReflectionTestUtils.getField(itemReader, "maxRows")).isEqualTo(6);
		assertThat((Integer) ReflectionTestUtils.getField(itemReader, "queryTimeout")).isEqualTo(8);
		assertThat((Boolean) ReflectionTestUtils.getField(itemReader, "ignoreWarnings")).isTrue();
		assertThat((Boolean) ReflectionTestUtils.getField(itemReader, "verifyCursorPosition")).isTrue();
		assertThat((Boolean) ReflectionTestUtils.getField(itemReader, "driverSupportsAbsolute")).isTrue();
		assertThat((Boolean) ReflectionTestUtils.getField(itemReader, "useSharedExtendedConnection")).isTrue();
	}

	@Test
	public void testNoName() {
		final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TaskLauncherConfiguration.class, BaseConfiguration.class)
			.withConfiguration(
					AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class, BatchAutoConfiguration.class,
							SingleStepJobAutoConfiguration.class, JdbcCursorItemReaderAutoConfiguration.class))
			.withPropertyValues("spring.batch.job.jobName=noNameJob", "spring.batch.job.stepName=step1",
					"spring.batch.job.chunkSize=5");

		assertThatThrownBy(() -> {
			runTest(applicationContextRunner);
		}).isInstanceOf(IllegalStateException.class).hasMessageContaining("UnsatisfiedDependencyException");
	}

	@Test
	public void testSqlName() {
		final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TaskLauncherConfiguration.class, BaseConfiguration.class)
			.withConfiguration(
					AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class, BatchAutoConfiguration.class,
							SingleStepJobAutoConfiguration.class, JdbcCursorItemReaderAutoConfiguration.class))
			.withPropertyValues("spring.batch.job.jobName=job", "spring.batch.job.stepName=step1",
					"spring.batch.job.chunkSize=5", "spring.batch.job.jdbccursoritemreader.name=fooReader");

		assertThatThrownBy(() -> {
			runTest(applicationContextRunner);
		}).isInstanceOf(IllegalStateException.class).hasMessageContaining("UnsatisfiedDependencyException");
	}

	private void runTest(ApplicationContextRunner applicationContextRunner) {
		applicationContextRunner.run((context) -> {
			JobOperator jobOperator = context.getBean(JobOperator.class);

			Job job = context.getBean(Job.class);

			JobExecution jobExecution = jobOperator.start(job, new JobParameters());

			JobRepository jobRepository = context.getBean(JobRepository.class);

			while (jobRepository.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}
		});
	}

	@AutoConfigureBefore({ JdbcCursorItemReaderAutoConfiguration.class,
			JDBCSingleStepDataSourceAutoConfiguration.class })
	@Configuration
	public static class TaskLauncherConfiguration {

		@Bean
		public PlatformTransactionManager platformTransactionManager() {
			return new ResourcelessTransactionManager();
		}

		private static Server defaultServer;

		@Bean
		public Server initH2TCPServer() {
			Server server = null;
			try {
				if (defaultServer == null) {
					server = Server
						.createTcpServer("-ifNotExists", "-tcp", "-tcpAllowOthers", "-tcpPort",
								String.valueOf(randomPort))
						.start();
					defaultServer = server;
					DriverManagerDataSource dataSource = new DriverManagerDataSource();
					dataSource.setDriverClassName(DATASOURCE_DRIVER_CLASS_NAME);
					dataSource.setUrl(DATASOURCE_URL);
					dataSource.setUsername(DATASOURCE_USER_NAME);
					dataSource.setPassword(DATASOURCE_USER_PASSWORD);
					ClassPathResource setupResource = new ClassPathResource("schema-h2.sql");
					ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator(setupResource);
					resourceDatabasePopulator.setContinueOnError(true);
					resourceDatabasePopulator.execute(dataSource);

					JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
					jdbcTemplate.execute("TRUNCATE TABLE item");
					jdbcTemplate.execute("INSERT INTO item VALUES ('foo')");
					jdbcTemplate.execute("INSERT INTO item VALUES ('bar')");
					jdbcTemplate.execute("INSERT INTO item VALUES ('baz')");
				}
			}
			catch (SQLException e) {
				throw new IllegalStateException(e);
			}
			return defaultServer;
		}

		@Bean
		public DataSource dataSource(Server server) {
			DriverManagerDataSource dataSource = new DriverManagerDataSource();
			dataSource.setDriverClassName(DATASOURCE_DRIVER_CLASS_NAME);
			dataSource.setUrl(DATASOURCE_URL);
			dataSource.setUsername(DATASOURCE_USER_NAME);
			dataSource.setPassword(DATASOURCE_USER_PASSWORD);
			return dataSource;
		}

	}

	@Configuration
	public static class BaseConfiguration {

		@Bean
		public ListItemWriter<Map<String, Object>> itemWriter() {
			return new ListItemWriter<>();
		}

	}

	@Configuration
	public static class RowMapperConfiguration {

		@Bean
		public RowMapper<Map<String, Object>> rowMapper() {
			return (rs, rowNum) -> {
				Map<String, Object> item = new HashMap<>();

				item.put("item", rs.getString("item_name"));

				return item;
			};
		}

		@Bean
		public ListItemWriter<Map<String, Object>> itemWriter() {
			return new ListItemWriter<>();
		}

	}

}
