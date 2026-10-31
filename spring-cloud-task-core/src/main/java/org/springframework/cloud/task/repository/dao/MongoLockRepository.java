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

package org.springframework.cloud.task.repository.dao;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.util.Assert;

/**
 * MongoDB-based implementation of {@link LockRegistry} for single instance task execution.
 * This implementation provides distributed locking mechanism using MongoDB collections
 * to ensure only one instance of a task can run at a time.
 *
 * @author JongJun Kim
 */
public class MongoLockRepository implements LockRegistry<Lock> {

	private static final String TASK_LOCK_COLLECTION = "task_locks";

	private final MongoOperations mongoOperations;

	private final String tablePrefix;

	private final long timeToLive;

	private final String clientId;

	/**
	 * Constructor for MongoDB Lock Repository.
	 * @param mongoOperations MongoDB operations template
	 * @param taskProperties Task configuration properties
	 */
	public MongoLockRepository(MongoOperations mongoOperations, TaskProperties taskProperties) {
		Assert.notNull(mongoOperations, "mongoOperations must not be null");
		Assert.notNull(taskProperties, "taskProperties must not be null");

		this.mongoOperations = mongoOperations;
		this.tablePrefix = taskProperties.getTablePrefix();
		this.timeToLive = taskProperties.getSingleInstanceLockTtl();
		this.clientId = UUID.randomUUID().toString();

		initializeCollection();
	}

	/**
	 * Initialize MongoDB collection for task locks.
	 * This ensures the collection and indexes exist even if MongoTaskRepositoryInitializer
	 * has not run yet or is disabled.
	 */
	private void initializeCollection() {
		String collectionName = getCollectionName(TASK_LOCK_COLLECTION);
		if (!mongoOperations.collectionExists(collectionName)) {
			mongoOperations.createCollection(collectionName);
		}

		// Ensure the compound unique index exists
		// This is idempotent - MongoDB will not create duplicate indexes
		Index compoundIndex = new Index()
			.on("lockKey", Sort.Direction.ASC)
			.on("region", Sort.Direction.ASC)
			.unique()
			.named("idx_lock_key_region");

		var indexOps = mongoOperations.indexOps(collectionName);
		try {
			indexOps.createIndex(compoundIndex);
		}
		catch (Exception e) {
			// Index may already exist - this is acceptable
			// MongoDB will throw an error if index exists with same name but different spec
		}
	}

	/**
	 * Get collection name with table prefix.
	 * @param baseName base collection name
	 * @return prefixed collection name
	 */
	private String getCollectionName(String baseName) {
		return tablePrefix + baseName;
	}

	@Override
	public Lock obtain(Object lockKey) {
		Assert.notNull(lockKey, "lockKey must not be null");
		return new MongoLock(lockKey.toString());
	}

	/**
	 * MongoDB-based lock implementation.
	 */
	private class MongoLock implements Lock {


		private final String lockKey;

		private final String region = "default";

		private volatile boolean locked = false;

		MongoLock(String lockKey) {
			this.lockKey = lockKey;
		}

		@Override
		public void lock() {
			try {
				lockInterruptibly();
			}
			catch (InterruptedException e) {
				Thread.currentThread()
					.interrupt();
				throw new IllegalStateException("Interrupted while acquiring lock", e);
			}
		}

		@Override
		public void lockInterruptibly() throws InterruptedException {
			while (!tryLock()) {
				if (Thread.currentThread()
					.isInterrupted()) {
					throw new InterruptedException();
				}
				Thread.sleep(100); // Short sleep before retry
			}
		}

		@Override
		public boolean tryLock() {
			return tryLock(0, TimeUnit.MILLISECONDS);
		}

		@Override
		public boolean tryLock(long time, TimeUnit unit) {
			long timeoutMillis = unit.toMillis(time);
			long startTime = System.currentTimeMillis();
			long retryInterval = 100; // 100ms between retries

			while (true) {
				if (tryLockOnce()) {
					return true;
				}

				// Check if we've exceeded the timeout
				long elapsed = System.currentTimeMillis() - startTime;
				if (elapsed >= timeoutMillis) {
					return false;
				}

				// Sleep before retry, but not longer than remaining timeout
				long remainingTime = timeoutMillis - elapsed;
				long sleepTime = Math.min(retryInterval, remainingTime);

				if (sleepTime > 0) {
					try {
						Thread.sleep(sleepTime);
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return false;
					}
				}
			}
		}

		/**
		 * Attempts to acquire the lock once without retrying.
		 * @return true if lock was acquired, false otherwise
		 */
		private boolean tryLockOnce() {
			cleanupExpiredLocks();
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime expirationTime = now.plusSeconds(timeToLive / 1000);
			try {
				TaskLockDocument lockDoc = new TaskLockDocument();
				lockDoc.id = this.lockKey + ":" + this.region; // Composite ID for uniqueness
				lockDoc.lockKey = this.lockKey;
				lockDoc.region = this.region;
				lockDoc.clientId = MongoLockRepository.this.clientId;
				lockDoc.createdDate = now;
				lockDoc.expirationDate = expirationTime;
				mongoOperations.insert(lockDoc, getCollectionName(TASK_LOCK_COLLECTION));
				this.locked = true;
				return true;
			}
			catch (DuplicateKeyException e) {
				Query query = Query.query(
					Criteria.where("lockKey")
						.is(this.lockKey)
						.and("region")
						.is(this.region)
						.and("expirationDate")
						.lt(now)
				);
				Update update = Update.update("expirationDate", expirationTime)
					.set("clientId", MongoLockRepository.this.clientId)
					.set("createdDate", now);
				var result = mongoOperations.updateFirst(query, update, TaskLockDocument.class, getCollectionName(TASK_LOCK_COLLECTION));
				if (result.getModifiedCount() > 0) {
					this.locked = true;
					return true;
				}
				Query ownerQuery = Query.query(
					Criteria.where("lockKey")
						.is(this.lockKey)
						.and("region")
						.is(this.region)
						.and("clientId")
						.is(MongoLockRepository.this.clientId)
				);
				Update ownerUpdate = Update.update("expirationDate", expirationTime);
				var ownerResult = mongoOperations.updateFirst(ownerQuery, ownerUpdate, TaskLockDocument.class, getCollectionName(TASK_LOCK_COLLECTION));
				if (ownerResult.getModifiedCount() > 0) {
					this.locked = true;
					return true;
				}
			}
			return false;
		}

		@Override
		public void unlock() {
			if (this.locked) {
				Query query = Query.query(
					Criteria.where("lockKey")
						.is(this.lockKey)
						.and("region")
						.is(this.region)
						.and("clientId")
						.is(MongoLockRepository.this.clientId)
				);

				mongoOperations.remove(query, TaskLockDocument.class,
					getCollectionName(TASK_LOCK_COLLECTION));
				this.locked = false;
			}
		}

		@Override
		public Condition newCondition() {
			throw new UnsupportedOperationException("Conditions are not supported");
		}

		private void cleanupExpiredLocks() {
			LocalDateTime now = LocalDateTime.now();
			Query expiredQuery = Query.query(
				Criteria.where("expirationDate")
					.lt(now)
			);

			mongoOperations.remove(expiredQuery, TaskLockDocument.class,
				getCollectionName(TASK_LOCK_COLLECTION));
		}

	}

	/**
	 * MongoDB document representing a task lock.
	 */
	@Document
	static class TaskLockDocument {

		@Id
		String id;

		String lockKey;

		String region;

		String clientId;

		LocalDateTime createdDate;

		LocalDateTime expirationDate;

		TaskLockDocument() {
			// ID will be set as composite of lockKey:region for uniqueness
		}

	}

}
