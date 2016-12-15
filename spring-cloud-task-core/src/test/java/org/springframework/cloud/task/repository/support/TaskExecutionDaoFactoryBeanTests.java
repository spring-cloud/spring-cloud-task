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
package org.springframework.cloud.task.repository.support;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;

import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.MapTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Michael Minella
 */
public class TaskExecutionDaoFactoryBeanTests {

	private ConfigurableApplicationContext context;

	@After
	public void tearDown() {
		if(this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testGetObjectType() {
		assertEquals(new TaskExecutionDaoFactoryBean().getObjectType(), TaskExecutionDao.class);
	}

	@Test
	public void testIsSingleton() {
		assertTrue(new TaskExecutionDaoFactoryBean().isSingleton());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorValidation() {
		new TaskExecutionDaoFactoryBean(null);
	}


	@Test
	public void testMapTaskExecutionDaoWithoutAppContext() throws Exception {
		TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean();
		TaskExecutionDao taskExecutionDao = factoryBean.getObject();

		assertTrue(taskExecutionDao instanceof MapTaskExecutionDao);

		TaskExecutionDao taskExecutionDao2 = factoryBean.getObject();

		assertTrue(taskExecutionDao == taskExecutionDao2);
	}

	@Test
	public void testDefaultDataSourceConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext(DefaultDataSourceConfiguration.class);

		DataSource dataSource = this.context.getBean(DataSource.class);

		TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean(dataSource);
		TaskExecutionDao taskExecutionDao = factoryBean.getObject();

		assertTrue(taskExecutionDao instanceof JdbcTaskExecutionDao);

		TaskExecutionDao taskExecutionDao2 = factoryBean.getObject();

		assertTrue(taskExecutionDao == taskExecutionDao2);
	}


	@Test
	public void testSettingTablePrefix() throws Exception {
		this.context = new AnnotationConfigApplicationContext(DefaultDataSourceConfiguration.class);

		DataSource dataSource = this.context.getBean(DataSource.class);

		TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean(dataSource, "foo_");
		TaskExecutionDao taskExecutionDao = factoryBean.getObject();

		assertEquals("foo_", ReflectionTestUtils.getField(taskExecutionDao, "tablePrefix"));
	}

	@Configuration
	public static class DefaultDataSourceConfiguration {

		@Bean
		public DataSource dataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2);
			return builder.build();
		}
	}
}
