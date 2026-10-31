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

import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskNameResolver;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * A {@link TaskConfigurer} implementation that uses MongoDB for storing Task metadata.
 * This configurer uses MongoDB collections to store task execution information instead
 * of relational database tables.
 *
 * @author JongJun Kim
 */
public class MongoTaskConfigurer implements TaskConfigurer {

	private final MongoOperations mongoOperations;

	private final TaskProperties taskProperties;

	private TaskRepository taskRepository;

	private TaskExplorer taskExplorer;

	private PlatformTransactionManager transactionManager;

	/**
	 * Create a new {@link MongoTaskConfigurer} with the provided {@link MongoOperations}.
	 * @param mongoOperations the {@link MongoOperations} to use for task metadata storage
	 * @param taskProperties the {@link TaskProperties} for configuration
	 */
	public MongoTaskConfigurer(MongoOperations mongoOperations, TaskProperties taskProperties) {
		Assert.notNull(mongoOperations, "mongoOperations must not be null");
		Assert.notNull(taskProperties, "taskProperties must not be null");
		this.mongoOperations = mongoOperations;
		this.taskProperties = taskProperties;
	}

	/**
	 * Create a new {@link MongoTaskConfigurer} with the provided {@link MongoOperations}
	 * and {@link PlatformTransactionManager}.
	 * @param mongoOperations the {@link MongoOperations} to use for task metadata storage
	 * @param taskProperties the {@link TaskProperties} for configuration
	 * @param transactionManager the {@link PlatformTransactionManager} for transaction management
	 */
	public MongoTaskConfigurer(MongoOperations mongoOperations, TaskProperties taskProperties,
			PlatformTransactionManager transactionManager) {
		this(mongoOperations, taskProperties);
		this.transactionManager = transactionManager;
	}

	@Override
	public TaskRepository getTaskRepository() {
		if (this.taskRepository == null) {
			this.taskRepository = new SimpleTaskRepository(new TaskExecutionDaoFactoryBean(this.mongoOperations, this.taskProperties));
		}
		return this.taskRepository;
	}

	@Override
	public TaskExplorer getTaskExplorer() {
		if (this.taskExplorer == null) {
			this.taskExplorer = new SimpleTaskExplorer(new TaskExecutionDaoFactoryBean(this.mongoOperations, this.taskProperties));
		}
		return this.taskExplorer;
	}

	@Override
	public PlatformTransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	@Override
	public TaskNameResolver getTaskNameResolver() {
		return new SimpleTaskNameResolver();
	}

	@Override
	public DataSource getTaskDataSource() {
		return null;
	}
}
