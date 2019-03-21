/*
 * Copyright 2017-2019 the original author or authors.
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

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Glenn Renfro
 */
@RunWith(SpringRunner.class)
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
				.isEqualTo(
						"org.springframework.batch.support.transaction.ResourcelessTransactionManager");
		defaultTaskConfigurer = new DefaultTaskConfigurer("foo");
		assertThat(defaultTaskConfigurer.getTransactionManager().getClass().getName())
				.isEqualTo(
						"org.springframework.batch.support.transaction.ResourcelessTransactionManager");
	}

	@Test
	public void testDefaultContext() throws Exception {
		AnnotationConfigApplicationContext localContext = new AnnotationConfigApplicationContext();
		localContext.register(EmbeddedDataSourceConfiguration.class,
				EntityManagerConfiguration.class);
		localContext.refresh();
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer(
				this.dataSource, TaskProperties.DEFAULT_TABLE_PREFIX, localContext);
		assertThat(defaultTaskConfigurer.getTransactionManager().getClass().getName())
				.isEqualTo("org.springframework.orm.jpa.JpaTransactionManager");
	}

	@Test
	public void dataSourceTransactionManagerTest() {
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer(
				this.dataSource);
		assertThat(defaultTaskConfigurer.getTransactionManager().getClass().getName())
				.isEqualTo(
						"org.springframework.jdbc.datasource.DataSourceTransactionManager");
		defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource, "FOO", null);
		assertThat(defaultTaskConfigurer.getTransactionManager().getClass().getName())
				.isEqualTo(
						"org.springframework.jdbc.datasource.DataSourceTransactionManager");
		defaultTaskConfigurer = new DefaultTaskConfigurer(this.dataSource, "FOO",
				this.context);
		assertThat(defaultTaskConfigurer.getTransactionManager().getClass().getName())
				.isEqualTo(
						"org.springframework.jdbc.datasource.DataSourceTransactionManager");
	}

	@Test
	public void taskExplorerTest() {
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer(
				this.dataSource);
		assertThat(defaultTaskConfigurer.getTaskExplorer()).isNotNull();
		defaultTaskConfigurer = new DefaultTaskConfigurer();
		assertThat(defaultTaskConfigurer.getTaskExplorer()).isNotNull();
	}

	@Test
	public void taskRepositoryTest() {
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer(
				this.dataSource);
		assertThat(defaultTaskConfigurer.getTaskRepository()).isNotNull();
		defaultTaskConfigurer = new DefaultTaskConfigurer();
		assertThat(defaultTaskConfigurer.getTaskRepository()).isNotNull();
	}

	@Test
	public void taskDataSource() {
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer(
				this.dataSource);
		assertThat(defaultTaskConfigurer.getTaskDataSource()).isNotNull();
		defaultTaskConfigurer = new DefaultTaskConfigurer();
		assertThat(defaultTaskConfigurer.getTaskDataSource()).isNull();
	}

	@Configuration
	public static class EntityManagerConfiguration {

		@Bean
		public EntityManager entityManager() {
			return mock(EntityManager.class);
		}

	}

}
