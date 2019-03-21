/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.task;

import java.util.Properties;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.configuration.DefaultTaskConfigurer;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.configuration.SimpleTaskConfiguration;
import org.springframework.cloud.task.configuration.TaskConfigurer;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertiesPropertySource;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the beans created by the SimpleTaskConfiguration.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 */
public class SimpleTaskConfigurationTests {

	private ConfigurableApplicationContext context;

	@After
	public void tearDown() {
		if(this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testRepository() throws Exception {
		this.context = new AnnotationConfigApplicationContext(SimpleTaskConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);

		TaskRepository taskRepository = this.context.getBean(TaskRepository.class);

		assertNotNull("testRepository should not be null", taskRepository);

		Class<?> targetClass = AopProxyUtils.ultimateTargetClass(taskRepository);

		assertEquals(targetClass, SimpleTaskRepository.class);
	}


	@Test
	public void testRepositoryInitialized() throws Exception {
		this.context = new AnnotationConfigApplicationContext(EmbeddedDataSourceConfiguration.class, SimpleTaskConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);

		TaskExplorer taskExplorer = this.context.getBean(TaskExplorer.class);

		assertThat(taskExplorer.getTaskExecutionCount(), is(equalTo(1l)));
	}

	@Test
	public void testRepositoryNotInitialized() throws Exception {
		Properties properties = new Properties();

		properties.put("spring.cloud.task.tablePrefix", "foobarless");
		PropertiesPropertySource propertiesSource = new PropertiesPropertySource("test", properties);

		this.context = new AnnotationConfigApplicationContext();
		this.context.getEnvironment().getPropertySources().addLast(propertiesSource);
		((AnnotationConfigApplicationContext)context).register(SimpleTaskConfiguration.class);
		((AnnotationConfigApplicationContext)context).register(PropertyPlaceholderAutoConfiguration.class);
		((AnnotationConfigApplicationContext)context).register(EmbeddedDataSourceConfiguration.class);
		boolean wasExceptionThrown = false;
		try {
			this.context.refresh();
		}
		catch (ApplicationContextException ex) {
			wasExceptionThrown = true;
		}
		assertTrue("Expected ApplicationContextException to be thrown", wasExceptionThrown);
	}


	@Test(expected = BeanCreationException.class)
	public void testMultipleConfigurers() {
		this.context = new AnnotationConfigApplicationContext(MultipleConfigurers.class,
				PropertyPlaceholderAutoConfiguration.class);
	}

	@Configuration
	@EnableTask
	public static class MultipleConfigurers {

		@Bean
		public TaskConfigurer taskConfigurer1() {
			return new DefaultTaskConfigurer((DataSource) null);
		}

		@Bean
		public TaskConfigurer taskConfigurer2() {
			return new DefaultTaskConfigurer((DataSource) null);
		}
	}

}
