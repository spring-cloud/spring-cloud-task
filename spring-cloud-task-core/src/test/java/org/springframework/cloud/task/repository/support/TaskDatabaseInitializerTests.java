/*
 * Copyright 2016-2019 the original author or authors.
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.cloud.task.configuration.TestConfiguration;
import org.springframework.cloud.task.repository.dao.MapTaskExecutionDao;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies that task initialization occurs properly.
 *
 * @author Glenn Renfro
 */
public class TaskDatabaseInitializerTests {

	@Rule
	public ExpectedException expected = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaultContext() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(new JdbcTemplate(this.context.getBean(DataSource.class))
				.queryForList("select * from TASK_EXECUTION").size()).isEqualTo(0);
	}

	@Test
	public void testNoDatabase() {
		this.context = new AnnotationConfigApplicationContext(EmptyConfiguration.class);
		SimpleTaskRepository repository = new SimpleTaskRepository(
				new TaskExecutionDaoFactoryBean());
		assertThat(repository.getTaskExecutionDao())
				.isInstanceOf(MapTaskExecutionDao.class);
		MapTaskExecutionDao dao = (MapTaskExecutionDao) repository.getTaskExecutionDao();
		assertThat(dao.getTaskExecutions().size()).isEqualTo(0);
	}

	@Test
	public void testNoTaskConfiguration() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EmptyConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(SimpleTaskRepository.class).length)
				.isEqualTo(0);
	}

	@Test(expected = BeanCreationException.class)
	public void testMultipleDataSourcesContext() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(SimpleTaskAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		DataSource dataSource = mock(DataSource.class);
		this.context.getBeanFactory().registerSingleton("mockDataSource", dataSource);
		this.context.refresh();
	}

	@Configuration
	public static class EmptyConfiguration {

	}

}
