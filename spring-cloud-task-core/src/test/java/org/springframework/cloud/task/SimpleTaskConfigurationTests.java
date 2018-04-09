/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.task;

import java.util.Properties;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.configuration.DefaultTaskConfigurer;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.configuration.SimpleTaskConfiguration;
import org.springframework.cloud.task.configuration.TaskConfigurer;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertiesPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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

		assertThat(taskRepository).isNotNull();

		Class<?> targetClass = AopProxyUtils.ultimateTargetClass(taskRepository);

		assertThat(targetClass).isEqualTo(SimpleTaskRepository.class);
	}


	@Test
	public void testRepositoryInitialized() throws Exception {
		this.context = new AnnotationConfigApplicationContext(EmbeddedDataSourceConfiguration.class, SimpleTaskConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);

		TaskExplorer taskExplorer = this.context.getBean(TaskExplorer.class);

		assertThat(taskExplorer.getTaskExecutionCount()).isEqualTo(1l);
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
		assertThat( wasExceptionThrown).isTrue();
	}


	@Test(expected = BeanCreationException.class)
	public void testMultipleConfigurers() {
		this.context = new AnnotationConfigApplicationContext(MultipleConfigurers.class,
				PropertyPlaceholderAutoConfiguration.class);
	}

	@Test(expected = BeanCreationException.class)
	public void testMultipleDataSources() {
		this.context = new AnnotationConfigApplicationContext(
				MultipleDataSources.class, SimpleTaskConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
	}

	/**
	 * Verify that the verifyEnvironment method skips DataSource Proxy Beans
	 * when determining the number of available dataSources.
	 */
	@Test
	public void testWithDataSourceProxy() {
		this.context = new AnnotationConfigApplicationContext(EmbeddedDataSourceConfiguration.class, TaskProperties.class,
				PropertyPlaceholderAutoConfiguration.class,  DataSourceProxyConfiguration.class
				);

		assertThat(this.context.getBeanNamesForType(DataSource.class).length).isEqualTo(2);
		SimpleTaskConfiguration taskConfiguration = this.context.getBean(SimpleTaskConfiguration.class);
		assertThat(taskConfiguration).isNotNull();
		assertThat(taskConfiguration.taskExplorer()).isNotNull();
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

	@Configuration
	public static class MultipleDataSources {

		@Bean
		public DataSource dataSource() {
			return mock(DataSource.class);
		};

		@Bean
		public DataSource dataSource2() {
			return mock(DataSource.class);
		};

	}

	@Configuration
	public static class DataSourceProxyConfiguration {

		@Autowired
		private ConfigurableApplicationContext context;

		@Bean
		public SimpleTaskConfiguration simpleTaskConfiguration() {
			GenericBeanDefinition proxyBeanDefinition = new GenericBeanDefinition();
			proxyBeanDefinition.setBeanClassName("javax.sql.DataSource");
			BeanDefinitionHolder myDataSource = new BeanDefinitionHolder(proxyBeanDefinition,"dataSource2");
			ScopedProxyUtils.createScopedProxy(myDataSource, (BeanDefinitionRegistry) this.context.getBeanFactory(), true);
			return new SimpleTaskConfiguration();
		}

	}

}
