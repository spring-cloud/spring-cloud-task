/*
 * Copyright 2017-present the original author or authors.
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

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { EmbeddedDataSourceConfiguration.class })
public class DefaultTaskConfigurerTests {

	@Autowired
	DataSource dataSource;

	@Autowired
	ApplicationContext context;

	@Test
	public void resourcelessTransactionManagerTest() {
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer();
		assertThat(defaultTaskConfigurer.getTransactionManager().getClass().getName())
			.isEqualTo("org.springframework.batch.support.transaction.ResourcelessTransactionManager");
		defaultTaskConfigurer = new DefaultTaskConfigurer("foo");
		assertThat(defaultTaskConfigurer.getTransactionManager().getClass().getName())
			.isEqualTo("org.springframework.batch.support.transaction.ResourcelessTransactionManager");
	}

	@Test
	public void testDefaultContext() {
		AnnotationConfigApplicationContext localContext = new AnnotationConfigApplicationContext();
		localContext.register(EmbeddedDataSourceConfiguration.class, EntityManagerConfiguration.class);
		localContext.refresh();
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource,
				TaskProperties.DEFAULT_TABLE_PREFIX, localContext);
		assertThat(defaultTaskConfigurer.getTransactionManager().getClass().getName())
			.isEqualTo("org.springframework.orm.jpa.JpaTransactionManager");
	}

	@Test
	public void dataSourceTransactionManagerTest() {
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource);
		assertThat(defaultTaskConfigurer.getTransactionManager().getClass().getName())
			.isEqualTo("org.springframework.jdbc.support.JdbcTransactionManager");
		defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource, "FOO", null);
		assertThat(defaultTaskConfigurer.getTransactionManager().getClass().getName())
			.isEqualTo("org.springframework.jdbc.support.JdbcTransactionManager");
		defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource, "FOO", this.context);
		assertThat(defaultTaskConfigurer.getTransactionManager().getClass().getName())
			.isEqualTo("org.springframework.jdbc.support.JdbcTransactionManager");
	}

	@Test
	public void taskExplorerTest() {
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource);
		assertThat(defaultTaskConfigurer.getTaskExplorer()).isNotNull();
		defaultTaskConfigurer = new DefaultTaskConfigurer();
		assertThat(defaultTaskConfigurer.getTaskExplorer()).isNotNull();
	}

	@Test
	public void taskNameResolverTest() {
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource);
		assertThat(defaultTaskConfigurer.getTaskNameResolver()).isNotNull();
		defaultTaskConfigurer = new DefaultTaskConfigurer();
		assertThat(defaultTaskConfigurer.getTaskNameResolver()).isNotNull();
	}

	@Test
	public void taskRepositoryTest() {
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource);
		assertThat(defaultTaskConfigurer.getTaskRepository()).isNotNull();
		defaultTaskConfigurer = new DefaultTaskConfigurer();
		assertThat(defaultTaskConfigurer.getTaskRepository()).isNotNull();
	}

	@Test
	public void taskDataSource() {
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource);
		assertThat(defaultTaskConfigurer.getTaskDataSource()).isNotNull();
		defaultTaskConfigurer = new DefaultTaskConfigurer();
		assertThat(defaultTaskConfigurer.getTaskDataSource()).isNull();
	}

	@Test
	public void taskDataSourceWithProperties() {
		TaskProperties taskProperties = new TaskProperties();
		taskProperties.setTablePrefix("foo");
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource, taskProperties);
		assertThat(defaultTaskConfigurer.getTaskDataSource()).isNotNull();
		String prefix = getPrefix(defaultTaskConfigurer);
		assertThat(prefix).isEqualTo("foo");
		System.out.println(prefix);
		defaultTaskConfigurer = new DefaultTaskConfigurer();
		validatePrefix(defaultTaskConfigurer, "TASK_");
		defaultTaskConfigurer = new DefaultTaskConfigurer(taskProperties);
		validatePrefix(defaultTaskConfigurer, "TASK_");
		defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource);
		validatePrefix(defaultTaskConfigurer, "TASK_");
		defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource, taskProperties);
		validatePrefix(defaultTaskConfigurer, "foo");
		defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource, new TaskProperties());
		validatePrefix(defaultTaskConfigurer, "TASK_");
		defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource, null, null);
		validatePrefix(defaultTaskConfigurer, "TASK_");
		defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource, "bar", null);
		validatePrefix(defaultTaskConfigurer, "bar");
		defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource, "bar", null, null);
		validatePrefix(defaultTaskConfigurer, "bar");
		defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource, "bar", null, taskProperties);
		validatePrefix(defaultTaskConfigurer, "bar");
	}

	private void validatePrefix(DefaultTaskConfigurer defaultTaskConfigurer, String prefix) {
		String result = getPrefix(defaultTaskConfigurer);
		assertThat(result).isEqualTo(prefix);
	}

	private String getPrefix(DefaultTaskConfigurer defaultTaskConfigurer) {
		SimpleTaskRepository taskRepository = (SimpleTaskRepository) ReflectionTestUtils.getField(defaultTaskConfigurer,
				"taskRepository");
		TaskExecutionDaoFactoryBean factoryBean = (TaskExecutionDaoFactoryBean) ReflectionTestUtils
			.getField(taskRepository, "taskExecutionDaoFactoryBean");
		return (String) ReflectionTestUtils.getField(factoryBean, "tablePrefix");
	}

	@Configuration
	public static class EntityManagerConfiguration {

		@Bean
		public EntityManager entityManager() {
			return mock(EntityManager.class);
		}

	}

}
