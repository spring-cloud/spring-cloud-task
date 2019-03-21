/*
 * Copyright 2015-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael Minella
 */
public class TaskExecutionDaoFactoryBeanTests {

	private ConfigurableApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testGetObjectType() {
		assertThat(TaskExecutionDao.class)
				.isEqualTo(new TaskExecutionDaoFactoryBean().getObjectType());
	}

	@Test
	public void testIsSingleton() {
		assertThat(new TaskExecutionDaoFactoryBean().isSingleton()).isTrue();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorValidation() {
		new TaskExecutionDaoFactoryBean(null);
	}

	@Test
	public void testMapTaskExecutionDaoWithoutAppContext() throws Exception {
		TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean();
		TaskExecutionDao taskExecutionDao = factoryBean.getObject();

		assertThat(taskExecutionDao instanceof MapTaskExecutionDao).isTrue();

		TaskExecutionDao taskExecutionDao2 = factoryBean.getObject();

		assertThat(taskExecutionDao == taskExecutionDao2).isTrue();
	}

	@Test
	public void testDefaultDataSourceConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				DefaultDataSourceConfiguration.class);

		DataSource dataSource = this.context.getBean(DataSource.class);

		TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean(
				dataSource);
		TaskExecutionDao taskExecutionDao = factoryBean.getObject();

		assertThat(taskExecutionDao instanceof JdbcTaskExecutionDao).isTrue();

		TaskExecutionDao taskExecutionDao2 = factoryBean.getObject();

		assertThat(taskExecutionDao == taskExecutionDao2).isTrue();
	}

	@Test
	public void testSettingTablePrefix() throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				DefaultDataSourceConfiguration.class);

		DataSource dataSource = this.context.getBean(DataSource.class);

		TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean(
				dataSource, "foo_");
		TaskExecutionDao taskExecutionDao = factoryBean.getObject();

		assertThat(ReflectionTestUtils.getField(taskExecutionDao, "tablePrefix"))
				.isEqualTo("foo_");
	}

	@Configuration
	public static class DefaultDataSourceConfiguration {

		@Bean
		public DataSource dataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.H2);
			return builder.build();
		}

	}

}
