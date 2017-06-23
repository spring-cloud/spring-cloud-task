/*
 *  Copyright 2017 the original author or authors.
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
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
		assertThat(defaultTaskConfigurer.getTransactionManager().getClass().getName(),
				is("org.springframework.batch.support.transaction.ResourcelessTransactionManager"));
		defaultTaskConfigurer = new DefaultTaskConfigurer("foo");
		assertThat(defaultTaskConfigurer.getTransactionManager().getClass().getName(),
				is("org.springframework.batch.support.transaction.ResourcelessTransactionManager"));
	}

	@Test
	public void testDefaultContext() throws Exception {
		AnnotationConfigApplicationContext localContext = new AnnotationConfigApplicationContext();
		localContext.register(EmbeddedDataSourceConfiguration.class,EntityManagerConfiguration.class);
		localContext.refresh();
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer(dataSource, TaskProperties.DEFAULT_TABLE_PREFIX, localContext);
		assertThat(defaultTaskConfigurer.getTransactionManager().getClass().getName(), is(equalTo("org.springframework.orm.jpa.JpaTransactionManager")));
	}

	@Test
	public void dataSourceTransactionManagerTest() {
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer(dataSource);
		assertThat(defaultTaskConfigurer.getTransactionManager().getClass().getName(),
				is("org.springframework.jdbc.datasource.DataSourceTransactionManager"));
		defaultTaskConfigurer = new DefaultTaskConfigurer(dataSource, "FOO", null);
		assertThat(defaultTaskConfigurer.getTransactionManager().getClass().getName(),
				is("org.springframework.jdbc.datasource.DataSourceTransactionManager"));
		defaultTaskConfigurer = new DefaultTaskConfigurer(dataSource, "FOO", context);
		assertThat(defaultTaskConfigurer.getTransactionManager().getClass().getName(),
				is("org.springframework.jdbc.datasource.DataSourceTransactionManager"));
	}

	@Test
	public void taskExplorerTest() {
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer(dataSource);
		assertThat(defaultTaskConfigurer.getTaskExplorer(), is(notNullValue()));
		defaultTaskConfigurer = new DefaultTaskConfigurer();
		assertThat(defaultTaskConfigurer.getTaskExplorer(), is(notNullValue()));
	}

	@Test
	public void taskRepositoryTest() {
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer(dataSource);
		assertThat(defaultTaskConfigurer.getTaskRepository(), is(notNullValue()));
		defaultTaskConfigurer = new DefaultTaskConfigurer();
		assertThat(defaultTaskConfigurer.getTaskRepository(), is(notNullValue()));
	}

	@Test
	public void taskDataSource() {
		DefaultTaskConfigurer defaultTaskConfigurer = new DefaultTaskConfigurer(dataSource);
		assertThat(defaultTaskConfigurer.getTaskDataSource(), is(notNullValue()));
		defaultTaskConfigurer = new DefaultTaskConfigurer();
		assertThat(defaultTaskConfigurer.getTaskDataSource(), is(nullValue()));
	}

	@Configuration
	public static class EntityManagerConfiguration {
		@Bean
		public EntityManager entityManager() {
			return mock(EntityManager.class);
		}
	}
}
