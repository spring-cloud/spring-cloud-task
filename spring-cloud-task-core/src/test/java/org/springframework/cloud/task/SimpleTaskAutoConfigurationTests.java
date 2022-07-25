/*
 * Copyright 2015-2022 the original author or authors.
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

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Verifies that the beans created by the SimpleTaskAutoConfiguration.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 */
public class SimpleTaskAutoConfigurationTests {

	@Test
	public void testRepository() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
						SimpleTaskAutoConfiguration.class, SingleTaskConfiguration.class));
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
				.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
						SimpleTaskAutoConfiguration.class, SingleTaskConfiguration.class))
				.withPropertyValues("spring.cloud.task.autoconfiguration.enabled=false");
		Executable executable = () -> {
			applicationContextRunner.run((context) -> {
				context.getBean(TaskRepository.class);
			});
		};
		verifyExceptionThrown(
				NoSuchBeanDefinitionException.class, "No qualifying "
						+ "bean of type 'org.springframework.cloud.task.repository.TaskRepository' " + "available",
				executable);
	}

	@Test
	public void testRepositoryInitialized() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner().withConfiguration(
				AutoConfigurations.of(EmbeddedDataSourceConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
						SimpleTaskAutoConfiguration.class, SingleTaskConfiguration.class))
				.withUserConfiguration(TaskLifecycleListenerConfiguration.class);
		applicationContextRunner.run((context) -> {
			TaskExplorer taskExplorer = context.getBean(TaskExplorer.class);
			assertThat(taskExplorer.getTaskExecutionCount()).isEqualTo(1L);
		});
	}

	@Test
	public void testRepositoryInitializedWithLazyInitialization() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner().withInitializer(
				(context) -> context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor()))
				.withConfiguration(AutoConfigurations.of(EmbeddedDataSourceConfiguration.class,
						PropertyPlaceholderAutoConfiguration.class, SimpleTaskAutoConfiguration.class,
						SingleTaskConfiguration.class))
				.withUserConfiguration(TaskLifecycleListenerConfiguration.class);
		applicationContextRunner.run((context) -> {
			TaskExplorer taskExplorer = context.getBean(TaskExplorer.class);
			assertThat(taskExplorer.getTaskExecutionCount()).isEqualTo(1L);
		});
	}

	@Test
	public void testRepositoryNotInitialized() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(EmbeddedDataSourceConfiguration.class,
						PropertyPlaceholderAutoConfiguration.class, SimpleTaskAutoConfiguration.class,
						SingleTaskConfiguration.class))
				.withUserConfiguration(TaskLifecycleListenerConfiguration.class)
				.withPropertyValues("spring.cloud.task.tablePrefix=foobarless");

		verifyExceptionThrownDefaultExecutable(ApplicationContextException.class, applicationContextRunner);
	}

	@Test
	public void testMultipleConfigurers() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
						SimpleTaskAutoConfiguration.class, SingleTaskConfiguration.class))
				.withUserConfiguration(MultipleConfigurers.class);

		verifyExceptionThrownDefaultExecutable(BeanCreationException.class,
				"Error creating bean " + "with name 'simpleTaskAutoConfiguration': Invocation of init method failed",
				applicationContextRunner);
	}

	@Test
	public void testMultipleDataSources() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
						SimpleTaskAutoConfiguration.class, SingleTaskConfiguration.class))
				.withUserConfiguration(MultipleDataSources.class);

		verifyExceptionThrownDefaultExecutable(BeanCreationException.class,
				"Error creating bean " + "with name 'simpleTaskAutoConfiguration': Invocation of init method failed",
				applicationContextRunner);

	}

	public void verifyExceptionThrownDefaultExecutable(Class classToCheck,
			ApplicationContextRunner applicationContextRunner) {
		Executable executable = () -> {
			applicationContextRunner.run((context) -> {
				Throwable expectedException = context.getStartupFailure();
				assertThat(expectedException).isNotNull();
				throw expectedException;
			});
		};
		assertThatExceptionOfType(classToCheck).isThrownBy(executable::execute);
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
		assertThatExceptionOfType(classToCheck).isThrownBy(executable::execute).withMessage(message);
	}

	/**
	 * Verify that the verifyEnvironment method skips DataSource Proxy Beans when
	 * determining the number of available dataSources.
	 */
	@Test
	public void testWithDataSourceProxy() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner().withConfiguration(
				AutoConfigurations.of(EmbeddedDataSourceConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
						SimpleTaskAutoConfiguration.class, SingleTaskConfiguration.class))
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
