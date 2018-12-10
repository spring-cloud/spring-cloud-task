/*
 * Copyright 2015-2018 the original author or authors.
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

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.function.Executable;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.configuration.DefaultTaskConfigurer;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.cloud.task.configuration.SingleTaskConfiguration;
import org.springframework.cloud.task.configuration.TaskConfigurer;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Verifies that the beans created by the SimpleTaskAutoConfiguration.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 */
public class SimpleTaskAutoConfigurationTests {

	private ConfigurableApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testRepository() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						PropertyPlaceholderAutoConfiguration.class,
						SimpleTaskAutoConfiguration.class,
						SingleTaskConfiguration.class));
		applicationContextRunner.run((context) -> {

			TaskRepository taskRepository = context.getBean(TaskRepository.class);
			assertThat(taskRepository).isNotNull();
			Class<?> targetClass = AopProxyUtils.ultimateTargetClass(taskRepository);
			assertThat(targetClass).isEqualTo(SimpleTaskRepository.class);
		});
	}

	@Test
	public void testAutoConfigurationDisabled() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						PropertyPlaceholderAutoConfiguration.class,
						SimpleTaskAutoConfiguration.class,
						SingleTaskConfiguration.class))
				.withPropertyValues("spring.cloud.task.autoconfiguration.enabled=false");
		Executable executable = () -> {
			applicationContextRunner.run((context) -> {
				context.getBean(TaskRepository.class);
			});
		};
		verifyExceptionThrown(NoSuchBeanDefinitionException.class, "No qualifying " +
				"bean of type 'org.springframework.cloud.task.repository.TaskRepository' " +
				"available", executable);
	}

	@Test
	public void testRepositoryInitialized() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(EmbeddedDataSourceConfiguration.class,
						PropertyPlaceholderAutoConfiguration.class,
						SimpleTaskAutoConfiguration.class,
						SingleTaskConfiguration.class))
				.withUserConfiguration(TaskLifecycleListenerConfiguration.class);
		applicationContextRunner.run((context) -> {
			TaskExplorer taskExplorer = context.getBean(TaskExplorer.class);
			assertThat(taskExplorer.getTaskExecutionCount()).isEqualTo(1l);
		});
	}

	@Test
	public void testRepositoryNotInitialized() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(EmbeddedDataSourceConfiguration.class,
						PropertyPlaceholderAutoConfiguration.class,
						SimpleTaskAutoConfiguration.class,
						SingleTaskConfiguration.class))
				.withUserConfiguration(TaskLifecycleListenerConfiguration.class)
				.withPropertyValues("spring.cloud.task.tablePrefix=foobarless");

		verifyExceptionThrownDefaultExecutable(ApplicationContextException.class, "Failed to start " +
				"bean 'taskLifecycleListener'; nested exception is " +
				"org.springframework.dao.DataAccessResourceFailureException: " +
				"Could not obtain sequence value; nested exception is org.h2.jdbc.JdbcSQLException: " +
				"Syntax error in SQL statement \"SELECT FOOBARLESSSEQ.NEXTVAL FROM[*] DUAL \"; " +
				"expected \"identifier\"; SQL statement:\n" +
				"select foobarlessSEQ.nextval from dual [42001-197]", applicationContextRunner);
	}

	@Test
	public void testMultipleConfigurers() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						PropertyPlaceholderAutoConfiguration.class,
						SimpleTaskAutoConfiguration.class,
						SingleTaskConfiguration.class))
				.withUserConfiguration(MultipleConfigurers.class);

		verifyExceptionThrownDefaultExecutable(BeanCreationException.class, "Error creating bean " +
				"with name 'simpleTaskAutoConfiguration': Invocation of init " +
				"method failed; nested exception is java.lang.IllegalStateException:" +
				" Expected one TaskConfigurer but found 2", applicationContextRunner);
	}

	@Test
	public void testMultipleDataSources() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
						SimpleTaskAutoConfiguration.class,
						SingleTaskConfiguration.class))
				.withUserConfiguration(MultipleDataSources.class);

		verifyExceptionThrownDefaultExecutable(BeanCreationException.class, "Error creating bean " +
				"with name 'simpleTaskAutoConfiguration': Invocation of init method " +
				"failed; nested exception is java.lang.IllegalStateException: To use " +
				"the default TaskConfigurer the context must contain no more than " +
				"one DataSource, found 2", applicationContextRunner);

	}

	public void verifyExceptionThrownDefaultExecutable(Class classToCheck, String message,
			ApplicationContextRunner applicationContextRunner) {
		Executable executable = () -> {
			applicationContextRunner.run((context) -> {
				Throwable expectedException = context.getStartupFailure();
				assertThat(expectedException).isNotNull();
				throw expectedException;
			});
		};
		verifyExceptionThrown(classToCheck, message, executable);
	}

	public void verifyExceptionThrown(Class classToCheck, String message, Executable executable) {
		Throwable exception = assertThrows(classToCheck, executable);
		assertThat(exception.getMessage()).isEqualTo(message);
	}

	/**
	 * Verify that the verifyEnvironment method skips DataSource Proxy Beans when determining
	 * the number of available dataSources.
	 */
	@Test
	public void testWithDataSourceProxy() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						EmbeddedDataSourceConfiguration.class,
						PropertyPlaceholderAutoConfiguration.class,
						SimpleTaskAutoConfiguration.class,
						SingleTaskConfiguration.class))
				.withUserConfiguration(DataSourceProxyConfiguration.class);
		applicationContextRunner.run((context) -> {
			assertThat(context.getBeanNamesForType(DataSource.class).length).isEqualTo(2);
			SimpleTaskAutoConfiguration taskConfiguration = context.getBean(SimpleTaskAutoConfiguration.class);
			assertThat(taskConfiguration).isNotNull();
			assertThat(taskConfiguration.taskExplorer()).isNotNull();
		});
	}

	@Configuration
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
		}

		@Bean
		public DataSource dataSource2() {
			return mock(DataSource.class);
		}

	}

	@Configuration
	public static class DataSourceProxyConfiguration {

		@Autowired
		private ConfigurableApplicationContext context;

		@Bean
		public BeanDefinitionHolder proxyDataSource() {
			GenericBeanDefinition proxyBeanDefinition = new GenericBeanDefinition();
			proxyBeanDefinition.setBeanClassName("javax.sql.DataSource");
			BeanDefinitionHolder myDataSource = new BeanDefinitionHolder(proxyBeanDefinition, "dataSource2");
			ScopedProxyUtils.createScopedProxy(myDataSource, (BeanDefinitionRegistry) this.context.getBeanFactory(),
					true);
			return myDataSource;
		}

	}

	@EnableTask
	@Configuration
	public static class TaskLifecycleListenerConfiguration {

	}

}
