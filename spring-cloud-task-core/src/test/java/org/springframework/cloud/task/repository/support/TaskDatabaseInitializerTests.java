/*
 * Copyright 2015 the original author or authors.
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
import static org.junit.Assert.assertNotNull;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.annotation.EnableTask;
import org.springframework.cloud.task.configuration.TaskConfigurer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.dao.MapTaskExecutionDao;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Glenn Renfro
 */
public class TaskDatabaseInitializerTests {
	private AnnotationConfigApplicationContext context;

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaultContext() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register( TestConfiguration.class,
				EmbeddedDataSourceConfiguration.class, TaskDatabaseInitializer.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertEquals(0, new JdbcTemplate(this.context.getBean(DataSource.class))
				.queryForList("select * from TASK_EXECUTION").size());
	}

	@Test
	public void testNoDatabase() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestCustomConfiguration.class,
				TaskDatabaseInitializer.class, PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(SimpleTaskRepository.class));
		SimpleTaskRepository repository = this.context.getBean(SimpleTaskRepository.class);
		assertNotNull(repository);
		MapTaskExecutionDao dao = (MapTaskExecutionDao) repository.getTaskExecutionDao();
		assertEquals(0, dao.getTaskExecutions().size());
	}
	@EnableTask
	protected static class TestConfiguration {
	}

	@EnableTask
	protected static class TestCustomConfiguration implements TaskConfigurer {
		@Override
		public TaskRepository getTaskRepository() {
			return new SimpleTaskRepository(new MapTaskExecutionDao());
		}
	}
}
