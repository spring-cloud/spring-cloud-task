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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.util.ReflectionTestUtils;

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
	public void testMapTaskExecutionDaoWithAppContext() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.refresh();

		TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean(this.context);
		TaskExecutionDao taskExecutionDao = factoryBean.getObject();

		assertTrue(taskExecutionDao instanceof MapTaskExecutionDao);

		TaskExecutionDao taskExecutionDao2 = factoryBean.getObject();

		assertTrue(taskExecutionDao == taskExecutionDao2);
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

		TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean(this.context);
		TaskExecutionDao taskExecutionDao = factoryBean.getObject();

		assertTrue(taskExecutionDao instanceof JdbcTaskExecutionDao);

		TaskExecutionDao taskExecutionDao2 = factoryBean.getObject();

		assertTrue(taskExecutionDao == taskExecutionDao2);
	}

	@Test
	public void testNonDefaultNameDataSourceConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext(AlternativeDataSourceConfiguration.class);

		TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean(this.context);
		TaskExecutionDao taskExecutionDao = factoryBean.getObject();

		assertTrue(taskExecutionDao instanceof JdbcTaskExecutionDao);

		TaskExecutionDao taskExecutionDao2 = factoryBean.getObject();

		assertTrue(taskExecutionDao == taskExecutionDao2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMissingCustomDataSourceNameConfiguration() throws Exception {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(AlternativeDataSourceConfiguration.class);

		TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean(context);
		factoryBean.setDataSourceName("wrongName");
		factoryBean.getObject();
	}

	@Test
	public void testCustomDataSourceNameConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext(AlternativeDataSourceConfiguration.class);

		TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean(this.context);
		factoryBean.setDataSourceName("notDataSource");
		TaskExecutionDao taskExecutionDao = factoryBean.getObject();

		assertTrue(taskExecutionDao instanceof JdbcTaskExecutionDao);

		TaskExecutionDao taskExecutionDao2 = factoryBean.getObject();

		assertTrue(taskExecutionDao == taskExecutionDao2);
	}

	@Test
	public void testCustomDataSourceNameConfigurationWithMultipleDataSources() throws Exception {
		this.context = new AnnotationConfigApplicationContext(MultipleDataSourceConfiguration.class);

		TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean(this.context);
		factoryBean.setDataSourceName("useThisDataSource");
		JdbcTaskExecutionDao taskExecutionDao = (JdbcTaskExecutionDao) factoryBean.getObject();

		Object usedDataSource = ReflectionTestUtils.getField(taskExecutionDao, "dataSource");

		assertTrue(usedDataSource == this.context.getBean("useThisDataSource"));

		TaskExecutionDao taskExecutionDao2 = factoryBean.getObject();

		assertTrue(taskExecutionDao == taskExecutionDao2);
	}

	@Test
	public void testSettingTablePrefix() throws Exception {
		this.context = new AnnotationConfigApplicationContext(DefaultDataSourceConfiguration.class);

		TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean(this.context);
		factoryBean.setTablePrefix("foo_");
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

	@Configuration
	public static class AlternativeDataSourceConfiguration {

		@Bean
		public DataSource notDataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2);
			return builder.build();
		}
	}

	@Configuration
	public static class MultipleDataSourceConfiguration {

		@Bean
		public DataSource useThisDataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.H2)
					.setName("useThisDataSource");
			return builder.build();
		}

		@Bean
		public DataSource dontUseThisDataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.H2)
					.setName("dontUseThisDataSource");
			return builder.build();
		}

	}
}
