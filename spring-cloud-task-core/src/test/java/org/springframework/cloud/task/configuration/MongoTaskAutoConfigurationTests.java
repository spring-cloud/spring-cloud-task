/*
 * Copyright 2015-present the original author or authors.
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

package org.springframework.cloud.task.configuration;

import javax.sql.DataSource;

import com.mongodb.client.MongoClients;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.MongoTaskRepositoryInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.transaction.PlatformTransactionManager;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoTaskAutoConfiguration}.
 *
 * @author JongJun Kim
 */
@Testcontainers
public class MongoTaskAutoConfigurationTests {

	@Container
	static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0")
		.withExposedPorts(27017);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(
			MongoTaskAutoConfiguration.class))
		.withUserConfiguration(MongoTestConfiguration.class);

	@Test
	public void testMongoTaskAutoConfigurationDisabledByDefault() {
		contextRunner
			.run(context -> {
				assertThat(context).doesNotHaveBean(MongoTaskRepositoryInitializer.class);
				assertThat(context).doesNotHaveBean(LockRegistry.class);
				assertThat(context).doesNotHaveBean(TaskConfigurer.class);
			});
	}

	@Test
	public void testMongoTaskAutoConfigurationEnabled() {
		contextRunner
			.withPropertyValues("spring.cloud.task.repository-type=mongodb")
			.run(context -> {
				assertThat(context).hasSingleBean(MongoTaskRepositoryInitializer.class);
				assertThat(context).hasSingleBean(TaskConfigurer.class);
				assertThat(context).getBean(TaskConfigurer.class).isInstanceOf(MongoTaskConfigurer.class);
			});
	}

	@Test
	public void testMongoLockRegistryEnabled() {
		contextRunner
			.withPropertyValues(
				"spring.cloud.task.repository-type=mongodb",
				"spring.cloud.task.single-instance-enabled=true")
			.run(context -> {
				assertThat(context).hasSingleBean(MongoTaskRepositoryInitializer.class);
				assertThat(context).hasSingleBean(LockRegistry.class);
				assertThat(context).hasSingleBean(TaskConfigurer.class);
			});
	}

	@Test
	public void testMongoLockRegistryDisabledByDefault() {
		contextRunner
			.withPropertyValues("spring.cloud.task.repository-type=mongodb")
			.run(context -> {
				assertThat(context).hasSingleBean(MongoTaskRepositoryInitializer.class);
				assertThat(context).doesNotHaveBean(LockRegistry.class);
				assertThat(context).hasSingleBean(TaskConfigurer.class);
			});
	}

	@Test
	public void testCustomTaskConfigurerPreventsAutoConfiguration() {
		contextRunner
			.withPropertyValues("spring.cloud.task.repository-type=mongodb")
			.withUserConfiguration(CustomTaskConfigurerConfiguration.class)
			.run(context -> {
				assertThat(context).hasSingleBean(MongoTaskRepositoryInitializer.class);
				assertThat(context).hasSingleBean(TaskConfigurer.class);
				assertThat(context).getBean(TaskConfigurer.class).isInstanceOf(CustomTaskConfigurer.class);
			});
	}

	@Test
	public void testMongoAutoConfigurationWithoutMongoOperations() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MongoTaskAutoConfiguration.class))
			.withPropertyValues("spring.cloud.task.repository-type=mongodb")
			.run(context -> {
				assertThat(context).doesNotHaveBean(MongoTaskRepositoryInitializer.class);
				assertThat(context).doesNotHaveBean(TaskConfigurer.class);
			});
	}

	@Test
	public void testCustomTablePrefix() {
		contextRunner
			.withPropertyValues(
				"spring.cloud.task.repository-type=mongodb",
				"spring.cloud.task.table-prefix=CUSTOM_")
			.run(context -> {
				assertThat(context).hasSingleBean(MongoTaskRepositoryInitializer.class);
				assertThat(context).hasSingleBean(TaskConfigurer.class);

				TaskProperties taskProperties = context.getBean(TaskProperties.class);
				assertThat(taskProperties.getTablePrefix()).isEqualTo("CUSTOM_");
			});
	}

	@Configuration
	static class MongoTestConfiguration {

		@Bean
		public MongoOperations mongoOperations() {
			return new MongoTemplate(
				new SimpleMongoClientDatabaseFactory(
					MongoClients.create(mongoDBContainer.getConnectionString()),
					"test-task-config-db"
				)
			);
		}
	}

	@Configuration
	static class CustomTaskConfigurerConfiguration {

		@Bean
		public TaskConfigurer customTaskConfigurer() {
			return new CustomTaskConfigurer();
		}
	}

	static class CustomTaskConfigurer implements TaskConfigurer {

		@Override
		public TaskRepository getTaskRepository() {
			return null;
		}

		@Override
		public TaskExplorer getTaskExplorer() {
			return null;
		}

		@Override
		public PlatformTransactionManager getTransactionManager() {
			return null;
		}

		@Override
		public DataSource getTaskDataSource() {
			return null;
		}

		@Override
		public TaskNameResolver getTaskNameResolver() {
			return null;
		}
	}
}
