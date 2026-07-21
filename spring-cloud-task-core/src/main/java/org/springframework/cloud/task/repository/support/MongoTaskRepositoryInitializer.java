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

package org.springframework.cloud.task.repository.support;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.util.Assert;

/**
 * Utility for initializing the MongoDB Task Repository collections and indexes.
 * This class creates the necessary collections and indexes for Spring Cloud Task
 * to function properly with MongoDB.
 *
 * Initialization can be disabled by setting the property
 * {@code spring.cloud.task.initialize-enabled} to false.
 *
 * @author JongJun Kim
 */
public final class MongoTaskRepositoryInitializer implements InitializingBean {

	private static final Log logger = LogFactory.getLog(MongoTaskRepositoryInitializer.class);

	private final MongoOperations mongoOperations;

	private final TaskProperties taskProperties;

	@Value("${spring.cloud.task.initialize.enable:true}")
	private boolean taskInitializationEnabled;

	private static final List<String> COLLECTION_NAMES = Arrays.asList(
		"task_executions",
		"task_execution_parameters",
		"task_batch_associations",
		"task_sequence",
		"task_locks"
	);

	/**
	 * Constructor for MongoDB Task Repository Initializer.
	 * @param mongoOperations MongoDB operations template
	 * @param taskProperties Task configuration properties
	 */
	public MongoTaskRepositoryInitializer(MongoOperations mongoOperations, TaskProperties taskProperties) {
		Assert.notNull(mongoOperations, "mongoOperations must not be null");
		Assert.notNull(taskProperties, "taskProperties must not be null");

		this.mongoOperations = mongoOperations;
		this.taskProperties = taskProperties;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		boolean isInitializeEnabled = (this.taskProperties.isInitializeEnabled() != null)
				? this.taskProperties.isInitializeEnabled() : this.taskInitializationEnabled;

		if (isInitializeEnabled) {
			logger.debug("Initializing MongoDB Task collections and indexes");
			initializeCollections();
			createIndexes();
			initializeSequence();
		}
	}

	/**
	 * Create all necessary MongoDB collections for Spring Cloud Task.
	 */
	private void initializeCollections() {
		String tablePrefix = this.taskProperties.getTablePrefix();

		for (String collectionName : COLLECTION_NAMES) {
			String prefixedName = tablePrefix + collectionName;

			if (!mongoOperations.collectionExists(prefixedName)) {
				mongoOperations.createCollection(prefixedName);
				logger.debug(String.format("Created collection: %s", prefixedName));
			}
		}
	}

	/**
	 * Create indexes for optimal query performance.
	 */
	private void createIndexes() {
		String tablePrefix = this.taskProperties.getTablePrefix();

		// Task Executions indexes
		createTaskExecutionIndexes(tablePrefix + "task_executions");

		// Task Execution Parameters indexes
		createTaskParameterIndexes(tablePrefix + "task_execution_parameters");

		// Task Batch Association indexes
		createBatchAssociationIndexes(tablePrefix + "task_batch_associations");

		// Task Locks indexes
		createLockIndexes(tablePrefix + "task_locks");
	}

	/**
	 * Create indexes for task executions collection.
	 */
	private void createTaskExecutionIndexes(String collectionName) {
		IndexOperations indexOps = mongoOperations.indexOps(collectionName);

		// Index on task name for faster lookups
		indexOps.createIndex(new Index().on("taskName", Sort.Direction.ASC)
			.named("idx_task_name"));

		// Index on start time for ordering
		indexOps.createIndex(new Index().on("startTime", Sort.Direction.DESC)
			.named("idx_start_time"));

		// Index on end time for running task queries
		indexOps.createIndex(new Index().on("endTime", Sort.Direction.ASC)
			.named("idx_end_time"));

		// Index on external execution ID
		indexOps.createIndex(new Index().on("externalExecutionId", Sort.Direction.ASC)
			.named("idx_external_execution_id"));

		// Compound index for running tasks by name
		indexOps.createIndex(new Index()
			.on("taskName", Sort.Direction.ASC)
			.on("endTime", Sort.Direction.ASC)
			.named("idx_task_name_end_time"));

		// Index on parent execution ID
		indexOps.createIndex(new Index().on("parentExecutionId", Sort.Direction.ASC)
			.named("idx_parent_execution_id"));

		logger.debug("Created indexes for task executions collection: " + collectionName);
	}

	/**
	 * Create indexes for task parameters collection.
	 */
	private void createTaskParameterIndexes(String collectionName) {
		IndexOperations indexOps = mongoOperations.indexOps(collectionName);

		// Index on task execution ID for parameter lookups
		indexOps.createIndex(new Index().on("taskExecutionId", Sort.Direction.ASC)
			.named("idx_task_execution_id"));

		logger.debug("Created indexes for task parameters collection: " + collectionName);
	}

	/**
	 * Create indexes for batch association collection.
	 */
	private void createBatchAssociationIndexes(String collectionName) {
		IndexOperations indexOps = mongoOperations.indexOps(collectionName);

		// Index on task execution ID
		indexOps.createIndex(new Index().on("taskExecutionId", Sort.Direction.ASC)
			.named("idx_task_execution_id"));

		// Index on job execution ID
		indexOps.createIndex(new Index().on("jobExecutionId", Sort.Direction.ASC)
			.named("idx_job_execution_id"));

		logger.debug("Created indexes for batch association collection: " + collectionName);
	}

	/**
	 * Create indexes for locks collection.
	 */
	private void createLockIndexes(String collectionName) {
		IndexOperations indexOps = mongoOperations.indexOps(collectionName);

		// Unique compound index on lock key and region
		indexOps.createIndex(new Index()
			.on("lockKey", Sort.Direction.ASC)
			.on("region", Sort.Direction.ASC)
			.unique()
			.named("idx_lock_key_region"));

		// Index on expiration date for cleanup
		indexOps.createIndex(new Index().on("expirationDate", Sort.Direction.ASC)
			.named("idx_expiration_date"));

		// Index on client ID
		indexOps.createIndex(new Index().on("clientId", Sort.Direction.ASC)
			.named("idx_client_id"));

		logger.debug("Created indexes for locks collection: " + collectionName);
	}

	/**
	 * Initialize the sequence counter for task execution IDs.
	 * The sequence starts at 0, and the first call to getNextExecutionId() will return 1.
	 * This matches the behavior of database sequences in the JDBC implementation.
	 */
	private void initializeSequence() {
		String sequenceCollectionName = this.taskProperties.getTablePrefix() + "task_sequence";

		// Check if sequence document exists, if not create it
		org.springframework.data.mongodb.core.query.Query sequenceQuery =
			org.springframework.data.mongodb.core.query.Query.query(
				org.springframework.data.mongodb.core.query.Criteria.where("_id").is("task_seq")
			);

		if (!mongoOperations.exists(sequenceQuery, sequenceCollectionName)) {
			// Create sequence document with initial value of 0
			// The first getNextExecutionId() call will increment and return 1
			java.util.Map<String, Object> sequenceDoc = new java.util.HashMap<>();
			sequenceDoc.put("_id", "task_seq");
			sequenceDoc.put("sequence", 0L);

			mongoOperations.save(sequenceDoc, sequenceCollectionName);
			logger.debug("Initialized sequence counter for task execution IDs");
		}
	}
}
