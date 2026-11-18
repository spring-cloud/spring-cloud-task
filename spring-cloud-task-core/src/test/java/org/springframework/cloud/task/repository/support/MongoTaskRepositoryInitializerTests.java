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
import java.util.Map;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MongoTaskRepositoryInitializer}.
 *
 * @author JongJun Kim
 */
@Testcontainers
public class MongoTaskRepositoryInitializerTests {

	private static final String DATABASE_NAME = "test-task-init-db";

	@Container
	static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0")
		.withExposedPorts(27017);

	private MongoClient mongoClient;
	private MongoOperations mongoOperations;
	private TaskProperties taskProperties;
	private MongoTaskRepositoryInitializer initializer;

	@BeforeEach
	public void setup() {
		mongoClient = MongoClients.create(mongoDBContainer.getConnectionString());
		mongoOperations = new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, DATABASE_NAME));
		taskProperties = new TaskProperties();
		taskProperties.setTablePrefix("TASK_");

		// Clean up any existing collections
		cleanupCollections();

		initializer = new MongoTaskRepositoryInitializer(mongoOperations, taskProperties);
	}

	@AfterEach
	public void tearDown() {
		if (mongoClient != null) {
			cleanupCollections();
			mongoClient.close();
		}
	}

	private void cleanupCollections() {
		List<String> collectionNames = Arrays.asList(
			"TASK_task_executions",
			"TASK_task_execution_parameters",
			"TASK_task_batch_associations",
			"TASK_task_sequence",
			"TASK_task_locks",
			"CUSTOM_task_executions",
			"CUSTOM_task_execution_parameters",
			"CUSTOM_task_batch_associations",
			"CUSTOM_task_sequence",
			"CUSTOM_task_locks"
		);

		for (String collectionName : collectionNames) {
			if (mongoOperations.collectionExists(collectionName)) {
				mongoOperations.getCollection(collectionName).drop();
			}
		}
	}

	@Test
	public void testConstructorWithValidParameters() {
		MongoTaskRepositoryInitializer init = new MongoTaskRepositoryInitializer(mongoOperations, taskProperties);
		assertThat(init).isNotNull();
	}

	@Test
	public void testConstructorWithNullMongoOperations() {
		assertThatThrownBy(() -> new MongoTaskRepositoryInitializer(null, taskProperties))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("mongoOperations must not be null");
	}

	@Test
	public void testConstructorWithNullTaskProperties() {
		assertThatThrownBy(() -> new MongoTaskRepositoryInitializer(mongoOperations, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("taskProperties must not be null");
	}

	@Test
	public void testAfterPropertiesSetCreatesCollections() throws Exception {
		// Set initialization enabled
		ReflectionTestUtils.setField(initializer, "taskInitializationEnabled", true);

		// Verify collections don't exist initially
		assertThat(mongoOperations.collectionExists("TASK_task_executions")).isFalse();
		assertThat(mongoOperations.collectionExists("TASK_task_execution_parameters")).isFalse();
		assertThat(mongoOperations.collectionExists("TASK_task_batch_associations")).isFalse();
		assertThat(mongoOperations.collectionExists("TASK_task_sequence")).isFalse();
		assertThat(mongoOperations.collectionExists("TASK_task_locks")).isFalse();

		initializer.afterPropertiesSet();

		// Verify collections were created
		assertThat(mongoOperations.collectionExists("TASK_task_executions")).isTrue();
		assertThat(mongoOperations.collectionExists("TASK_task_execution_parameters")).isTrue();
		assertThat(mongoOperations.collectionExists("TASK_task_batch_associations")).isTrue();
		assertThat(mongoOperations.collectionExists("TASK_task_sequence")).isTrue();
		assertThat(mongoOperations.collectionExists("TASK_task_locks")).isTrue();
	}

	@Test
	public void testAfterPropertiesSetCreatesIndexes() throws Exception {
		ReflectionTestUtils.setField(initializer, "taskInitializationEnabled", true);

		initializer.afterPropertiesSet();

		// Verify task executions indexes
		List<IndexInfo> taskExecutionIndexes = mongoOperations.indexOps("TASK_task_executions").getIndexInfo();
		assertThat(taskExecutionIndexes.size()).isGreaterThan(1); // Should have multiple indexes beyond the default _id index

		// Check for specific indexes
		boolean hasTaskNameIndex = taskExecutionIndexes.stream()
			.anyMatch(index -> "idx_task_name".equals(index.getName()));
		assertThat(hasTaskNameIndex).isTrue();

		boolean hasStartTimeIndex = taskExecutionIndexes.stream()
			.anyMatch(index -> "idx_start_time".equals(index.getName()));
		assertThat(hasStartTimeIndex).isTrue();

		boolean hasEndTimeIndex = taskExecutionIndexes.stream()
			.anyMatch(index -> "idx_end_time".equals(index.getName()));
		assertThat(hasEndTimeIndex).isTrue();

		boolean hasExternalExecutionIdIndex = taskExecutionIndexes.stream()
			.anyMatch(index -> "idx_external_execution_id".equals(index.getName()));
		assertThat(hasExternalExecutionIdIndex).isTrue();

		// Verify task parameters indexes
		List<IndexInfo> taskParameterIndexes = mongoOperations.indexOps("TASK_task_execution_parameters").getIndexInfo();
		boolean hasTaskExecutionIdIndex = taskParameterIndexes.stream()
			.anyMatch(index -> "idx_task_execution_id".equals(index.getName()));
		assertThat(hasTaskExecutionIdIndex).isTrue();

		// Verify batch association indexes
		List<IndexInfo> batchAssociationIndexes = mongoOperations.indexOps("TASK_task_batch_associations").getIndexInfo();
		boolean hasTaskExecIdIndex = batchAssociationIndexes.stream()
			.anyMatch(index -> "idx_task_execution_id".equals(index.getName()));
		assertThat(hasTaskExecIdIndex).isTrue();

		boolean hasJobExecIdIndex = batchAssociationIndexes.stream()
			.anyMatch(index -> "idx_job_execution_id".equals(index.getName()));
		assertThat(hasJobExecIdIndex).isTrue();

		// Verify lock indexes
		List<IndexInfo> lockIndexes = mongoOperations.indexOps("TASK_task_locks").getIndexInfo();
		boolean hasLockKeyRegionIndex = lockIndexes.stream()
			.anyMatch(index -> "idx_lock_key_region".equals(index.getName()));
		assertThat(hasLockKeyRegionIndex).isTrue();
	}

	@Test
	public void testAfterPropertiesSetInitializesSequence() throws Exception {
		ReflectionTestUtils.setField(initializer, "taskInitializationEnabled", true);

		initializer.afterPropertiesSet();

		// Verify sequence document was created
		Query sequenceQuery = Query.query(Criteria.where("_id").is("task_seq"));
		boolean sequenceExists = mongoOperations.exists(sequenceQuery, "TASK_task_sequence");
		assertThat(sequenceExists).isTrue();
	}

	@Test
	public void testAfterPropertiesSetWithInitializationDisabled() throws Exception {
		ReflectionTestUtils.setField(initializer, "taskInitializationEnabled", false);

		initializer.afterPropertiesSet();

		// Verify collections were not created
		assertThat(mongoOperations.collectionExists("TASK_task_executions")).isFalse();
		assertThat(mongoOperations.collectionExists("TASK_task_execution_parameters")).isFalse();
		assertThat(mongoOperations.collectionExists("TASK_task_batch_associations")).isFalse();
		assertThat(mongoOperations.collectionExists("TASK_task_sequence")).isFalse();
		assertThat(mongoOperations.collectionExists("TASK_task_locks")).isFalse();
	}

	@Test
	public void testAfterPropertiesSetWithTaskPropertiesDisabled() throws Exception {
		taskProperties.setInitializeEnabled(false);
		ReflectionTestUtils.setField(initializer, "taskInitializationEnabled", true);

		initializer.afterPropertiesSet();

		// Verify collections were not created because task properties disabled it
		assertThat(mongoOperations.collectionExists("TASK_task_executions")).isFalse();
		assertThat(mongoOperations.collectionExists("TASK_task_execution_parameters")).isFalse();
		assertThat(mongoOperations.collectionExists("TASK_task_batch_associations")).isFalse();
		assertThat(mongoOperations.collectionExists("TASK_task_sequence")).isFalse();
		assertThat(mongoOperations.collectionExists("TASK_task_locks")).isFalse();
	}

	@Test
	public void testAfterPropertiesSetWithTaskPropertiesEnabled() throws Exception {
		taskProperties.setInitializeEnabled(true);
		ReflectionTestUtils.setField(initializer, "taskInitializationEnabled", false);

		initializer.afterPropertiesSet();

		// Verify collections were created because task properties enabled it (overrides field)
		assertThat(mongoOperations.collectionExists("TASK_task_executions")).isTrue();
		assertThat(mongoOperations.collectionExists("TASK_task_execution_parameters")).isTrue();
		assertThat(mongoOperations.collectionExists("TASK_task_batch_associations")).isTrue();
		assertThat(mongoOperations.collectionExists("TASK_task_sequence")).isTrue();
		assertThat(mongoOperations.collectionExists("TASK_task_locks")).isTrue();
	}

	@Test
	public void testCustomTablePrefix() throws Exception {
		TaskProperties customTaskProperties = new TaskProperties();
		customTaskProperties.setTablePrefix("CUSTOM_");

		MongoTaskRepositoryInitializer customInitializer =
			new MongoTaskRepositoryInitializer(mongoOperations, customTaskProperties);
		ReflectionTestUtils.setField(customInitializer, "taskInitializationEnabled", true);

		customInitializer.afterPropertiesSet();

		// Verify collections were created with custom prefix
		assertThat(mongoOperations.collectionExists("CUSTOM_task_executions")).isTrue();
		assertThat(mongoOperations.collectionExists("CUSTOM_task_execution_parameters")).isTrue();
		assertThat(mongoOperations.collectionExists("CUSTOM_task_batch_associations")).isTrue();
		assertThat(mongoOperations.collectionExists("CUSTOM_task_sequence")).isTrue();
		assertThat(mongoOperations.collectionExists("CUSTOM_task_locks")).isTrue();

		// Verify sequence document was created with custom prefix
		Query sequenceQuery = Query.query(Criteria.where("_id").is("task_seq"));
		boolean sequenceExists = mongoOperations.exists(sequenceQuery, "CUSTOM_task_sequence");
		assertThat(sequenceExists).isTrue();
	}

	@Test
	public void testIdempotentInitialization() throws Exception {
		ReflectionTestUtils.setField(initializer, "taskInitializationEnabled", true);

		// Run initialization twice
		initializer.afterPropertiesSet();
		initializer.afterPropertiesSet();

		// Verify collections still exist and no errors occurred
		assertThat(mongoOperations.collectionExists("TASK_task_executions")).isTrue();
		assertThat(mongoOperations.collectionExists("TASK_task_execution_parameters")).isTrue();
		assertThat(mongoOperations.collectionExists("TASK_task_batch_associations")).isTrue();
		assertThat(mongoOperations.collectionExists("TASK_task_sequence")).isTrue();
		assertThat(mongoOperations.collectionExists("TASK_task_locks")).isTrue();
	}

	@Test
	public void testSequenceInitializationDoesNotOverwrite() throws Exception {
		ReflectionTestUtils.setField(initializer, "taskInitializationEnabled", true);

		// Initialize first time
		initializer.afterPropertiesSet();

		// Manually update the sequence
		Query sequenceQuery = Query.query(Criteria.where("_id").is("task_seq"));
		Object sequenceDoc = mongoOperations.findOne(sequenceQuery, Object.class, "TASK_task_sequence");
		assertThat(sequenceDoc).isNotNull();

		// Update sequence value
		java.util.Map<String, Object> updatedDoc = new java.util.HashMap<>();
		updatedDoc.put("_id", "task_seq");
		updatedDoc.put("sequence", 100L);
		mongoOperations.save(updatedDoc, "TASK_task_sequence");

		// Initialize again
		initializer.afterPropertiesSet();

		// Verify sequence wasn't reset
		Object finalSequenceDoc = mongoOperations.findOne(sequenceQuery, Map.class, "TASK_task_sequence");
		@SuppressWarnings("unchecked")
		java.util.Map<String, Object> sequenceMap = (Map<String, Object>) finalSequenceDoc;
		assertThat(sequenceMap.get("sequence")).isEqualTo(100L);
	}
}
