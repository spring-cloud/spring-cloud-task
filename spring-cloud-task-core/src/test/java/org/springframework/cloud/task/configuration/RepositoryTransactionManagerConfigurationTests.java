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

package org.springframework.cloud.task.configuration;

import java.util.ArrayList;
import java.util.Date;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.listener.TaskLifecycleListener;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael Minella
 */
public class RepositoryTransactionManagerConfigurationTests {

	@Test
	public void testZeroCustomTransactionManagerConfiguration() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
								SimpleTaskAutoConfiguration.class,
								ZeroTransactionManagerConfiguration.class))
				.withPropertyValues("application.name=transactionManagerTask");

		applicationContextRunner.run((context) -> {
			DataSource dataSource = context.getBean("dataSource", DataSource.class);

			int taskExecutionCount = JdbcTestUtils
					.countRowsInTable(new JdbcTemplate(dataSource), "TASK_EXECUTION");

			assertThat(taskExecutionCount).isEqualTo(1);
		});
	}

	@Test
	public void testSingleCustomTransactionManagerConfiguration() {
		testConfiguration(SingleTransactionManagerConfiguration.class);
	}

	@Test
	public void testMultipleCustomTransactionManagerConfiguration() {
		testConfiguration(MultipleTransactionManagerConfiguration.class);
	}

	private void testConfiguration(Class configurationClass) {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
								SimpleTaskAutoConfiguration.class, configurationClass))
				.withPropertyValues("application.name=transactionManagerTask");

		applicationContextRunner.run((context) -> {
			DataSource dataSource = context.getBean("dataSource", DataSource.class);

			int taskExecutionCount = JdbcTestUtils
					.countRowsInTable(new JdbcTemplate(dataSource), "TASK_EXECUTION");

			// Verify that the create call was rolled back
			assertThat(taskExecutionCount).isEqualTo(0);

			// Execute a new create call so that things close cleanly
			TaskRepository taskRepository = context.getBean("taskRepository",
					TaskRepository.class);

			TaskExecution taskExecution = taskRepository
					.createTaskExecution("transactionManagerTask");
			taskExecution = taskRepository.startTaskExecution(
					taskExecution.getExecutionId(), taskExecution.getTaskName(),
					new Date(), new ArrayList<>(0), null);

			TaskLifecycleListener listener = context.getBean(TaskLifecycleListener.class);

			ReflectionTestUtils.setField(listener, "taskExecution", taskExecution);
		});
	}

	@EnableTask
	@Configuration
	public static class ZeroTransactionManagerConfiguration {

		@Bean
		public TaskConfigurer taskConfigurer(DataSource dataSource) {
			return new DefaultTaskConfigurer(dataSource);
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
		}

	}

	@EnableTask
	@Configuration
	public static class SingleTransactionManagerConfiguration {

		@Bean
		public TaskConfigurer taskConfigurer(DataSource dataSource,
				PlatformTransactionManager transactionManager) {
			return new DefaultTaskConfigurer(dataSource) {
				@Override
				public PlatformTransactionManager getTransactionManager() {
					return transactionManager;
				}
			};
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
		}

		@Bean
		public DataSourceTransactionManager transactionManager(DataSource dataSource) {
			return new TestDataSourceTransactionManager(dataSource);
		}

	}

	@EnableTask
	@Configuration
	public static class MultipleTransactionManagerConfiguration {

		@Bean
		public TaskConfigurer taskConfigurer(DataSource dataSource,
				PlatformTransactionManager transactionManager) {
			return new DefaultTaskConfigurer(dataSource) {
				@Override
				public PlatformTransactionManager getTransactionManager() {
					return transactionManager;
				}
			};
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
		}

		@Bean
		public DataSourceTransactionManager transactionManager(DataSource dataSource) {
			return new TestDataSourceTransactionManager(dataSource);
		}

		@Bean
		public DataSource dataSource2() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
		}

		@Bean
		public DataSourceTransactionManager transactionManager2(DataSource dataSource2) {
			return new DataSourceTransactionManager(dataSource2);
		}

	}

	private static class TestDataSourceTransactionManager
			extends DataSourceTransactionManager {

		protected TestDataSourceTransactionManager(DataSource dataSource) {
			super(dataSource);
		}

		private int count = 0;

		@Override
		protected void doCommit(DefaultTransactionStatus status) {

			if (count == 0) {
				// Rollback the finish of the task
				super.doRollback(status);
			}
			else {
				// Commit the start of the task
				super.doCommit(status);
			}

			count++;
		}

	}

}
