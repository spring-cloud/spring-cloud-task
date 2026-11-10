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

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.transaction.PlatformTransactionManager;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MongoTaskConfigurer}.
 *
 * @author JongJun Kim
 */
@Testcontainers
public class MongoTaskConfigurerTests {

	private static final String DATABASE_NAME = "test-task-configurer-db";

	@Container
	static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0")
		.withExposedPorts(27017);

	private MongoClient mongoClient;
	private MongoOperations mongoOperations;
	private TaskProperties taskProperties;

	@Mock
	private PlatformTransactionManager transactionManager;

	@BeforeEach
	public void setup() {
		mongoClient = MongoClients.create(mongoDBContainer.getConnectionString());
		mongoOperations = new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, DATABASE_NAME));
		taskProperties = new TaskProperties();
		taskProperties.setTablePrefix("TASK_");
	}

	@AfterEach
	public void tearDown() {
		if (mongoClient != null) {
			mongoClient.close();
		}
	}

	@Test
	public void testMongoTaskConfigurerConstruction() {
		MongoTaskConfigurer configurer = new MongoTaskConfigurer(mongoOperations, taskProperties);

		assertThat(configurer).isNotNull();
		assertThat(configurer.getTaskRepository()).isInstanceOf(SimpleTaskRepository.class);
		assertThat(configurer.getTaskExplorer()).isInstanceOf(SimpleTaskExplorer.class);
	}

	@Test
	public void testMongoTaskConfigurerWithTransactionManager() {
		MongoTaskConfigurer configurer = new MongoTaskConfigurer(mongoOperations, taskProperties, transactionManager);

		assertThat(configurer).isNotNull();
		assertThat(configurer.getTaskRepository()).isInstanceOf(SimpleTaskRepository.class);
		assertThat(configurer.getTaskExplorer()).isInstanceOf(SimpleTaskExplorer.class);
		assertThat(configurer.getTransactionManager()).isSameAs(transactionManager);
	}

	@Test
	public void testGetTaskDataSource() {
		MongoTaskConfigurer configurer = new MongoTaskConfigurer(mongoOperations, taskProperties);
		DataSource dataSource = configurer.getTaskDataSource();

		// MongoDB configurer doesn't use DataSource, should return null
		assertThat(dataSource).isNull();
	}

	@Test
	public void testGetTaskNameResolver() {
		MongoTaskConfigurer configurer = new MongoTaskConfigurer(mongoOperations, taskProperties);
		TaskNameResolver nameResolver = configurer.getTaskNameResolver();

		assertThat(nameResolver).isNotNull();
	}

	@Test
	public void testGetTaskRepository() {
		MongoTaskConfigurer configurer = new MongoTaskConfigurer(mongoOperations, taskProperties);
		TaskRepository repository = configurer.getTaskRepository();

		assertThat(repository).isNotNull();
		assertThat(repository).isInstanceOf(SimpleTaskRepository.class);
	}

	@Test
	public void testGetTaskExplorer() {
		MongoTaskConfigurer configurer = new MongoTaskConfigurer(mongoOperations, taskProperties);
		TaskExplorer explorer = configurer.getTaskExplorer();

		assertThat(explorer).isNotNull();
		assertThat(explorer).isInstanceOf(SimpleTaskExplorer.class);
	}

	@Test
	public void testGetTransactionManager() {
		MongoTaskConfigurer configurer = new MongoTaskConfigurer(mongoOperations, taskProperties, transactionManager);
		PlatformTransactionManager retrievedManager = configurer.getTransactionManager();

		assertThat(retrievedManager).isSameAs(transactionManager);
	}

	@Test
	public void testGetTransactionManagerWithoutTransactionManager() {
		MongoTaskConfigurer configurer = new MongoTaskConfigurer(mongoOperations, taskProperties);
		PlatformTransactionManager retrievedManager = configurer.getTransactionManager();

		assertThat(retrievedManager).isNull();
	}

	@Test
	public void testConstructorWithNullMongoOperations() {
		assertThatThrownBy(() -> new MongoTaskConfigurer(null, taskProperties))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("mongoOperations must not be null");
	}

	@Test
	public void testConstructorWithNullTaskProperties() {
		assertThatThrownBy(() -> new MongoTaskConfigurer(mongoOperations, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("taskProperties must not be null");
	}

	@Test
	public void testConstructorWithTransactionManagerAndNullMongoOperations() {
		assertThatThrownBy(() -> new MongoTaskConfigurer(null, taskProperties, transactionManager))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("mongoOperations must not be null");
	}

	@Test
	public void testConstructorWithTransactionManagerAndNullTaskProperties() {
		assertThatThrownBy(() -> new MongoTaskConfigurer(mongoOperations, null, transactionManager))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("taskProperties must not be null");
	}

	@Test
	public void testRepositoryAndExplorerConsistency() {
		MongoTaskConfigurer configurer = new MongoTaskConfigurer(mongoOperations, taskProperties);

		TaskRepository repository = configurer.getTaskRepository();
		TaskExplorer explorer = configurer.getTaskExplorer();

		// Both should be created and of correct type
		assertThat(repository).isInstanceOf(SimpleTaskRepository.class);
		assertThat(explorer).isInstanceOf(SimpleTaskExplorer.class);

		// Both should be non-null and functional
		assertThat(repository).isNotNull();
		assertThat(explorer).isNotNull();
	}

	@Test
	public void testCustomTablePrefix() {
		TaskProperties customProperties = new TaskProperties();
		customProperties.setTablePrefix("CUSTOM_PREFIX_");

		MongoTaskConfigurer configurer = new MongoTaskConfigurer(mongoOperations, customProperties);
		TaskRepository repository = configurer.getTaskRepository();
		TaskExplorer explorer = configurer.getTaskExplorer();

		// Verify components are created with custom properties
		assertThat(repository).isNotNull();
		assertThat(explorer).isNotNull();
	}

	@Test
	public void testMultipleCallsReturnSameInstance() {
		MongoTaskConfigurer configurer = new MongoTaskConfigurer(mongoOperations, taskProperties);

		// Multiple calls should return the same instances (singleton behavior)
		TaskRepository repository1 = configurer.getTaskRepository();
		TaskRepository repository2 = configurer.getTaskRepository();
		assertThat(repository1).isSameAs(repository2);

		TaskExplorer explorer1 = configurer.getTaskExplorer();
		TaskExplorer explorer2 = configurer.getTaskExplorer();
		assertThat(explorer1).isSameAs(explorer2);
	}
}
