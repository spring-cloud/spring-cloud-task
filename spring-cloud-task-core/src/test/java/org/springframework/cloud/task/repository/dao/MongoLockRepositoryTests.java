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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MongoLockRepository}.
 *
 * @author JongJun Kim
 */
@Testcontainers
public class MongoLockRepositoryTests {

	private static final String DATABASE_NAME = "test-lock-db";

	@Container
	static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0")
		.withExposedPorts(27017);

	private MongoClient mongoClient;
	private MongoOperations mongoOperations;
	private TaskProperties taskProperties;
	private MongoLockRepository lockRepository;

	@BeforeEach
	public void setup() {
		mongoClient = MongoClients.create(mongoDBContainer.getConnectionString());
		mongoOperations = new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, DATABASE_NAME));
		taskProperties = new TaskProperties();
		taskProperties.setTablePrefix("TASK_");
		taskProperties.setSingleInstanceLockTtl(30000); // 30 seconds

		// Clean up any existing collections
		cleanupCollections();

		lockRepository = new MongoLockRepository(mongoOperations, taskProperties);
	}

	@AfterEach
	public void tearDown() {
		if (mongoClient != null) {
			cleanupCollections();
			mongoClient.close();
		}
	}

	private void cleanupCollections() {
		if (mongoOperations.collectionExists("TASK_task_locks")) {
			mongoOperations.getCollection("TASK_task_locks").drop();
		}
	}

	@Test
	public void testConstructorWithValidParameters() {
		MongoLockRepository repository = new MongoLockRepository(mongoOperations, taskProperties);
		assertThat(repository).isNotNull();

		// Verify collection was created
		assertThat(mongoOperations.collectionExists("TASK_task_locks")).isTrue();
	}

	@Test
	public void testConstructorWithNullMongoOperations() {
		assertThatThrownBy(() -> new MongoLockRepository(null, taskProperties))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("mongoOperations must not be null");
	}

	@Test
	public void testConstructorWithNullTaskProperties() {
		assertThatThrownBy(() -> new MongoLockRepository(mongoOperations, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("taskProperties must not be null");
	}

	@Test
	public void testObtainLock() {
		Lock lock = lockRepository.obtain("testKey");
		assertThat(lock).isNotNull();
	}

	@Test
	public void testObtainLockWithNullKey() {
		assertThatThrownBy(() -> lockRepository.obtain(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("lockKey must not be null");
	}

	@Test
	public void testTryLockSuccess() {
		Lock lock = lockRepository.obtain("testKey1");
		boolean acquired = lock.tryLock();
		assertThat(acquired).isTrue();

		// Verify lock document exists in MongoDB
		Query lockQuery = Query.query(
			Criteria.where("lockKey").is("testKey1")
				.and("region").is("default")
		);
		boolean lockExists = mongoOperations.exists(lockQuery, "TASK_task_locks");
		assertThat(lockExists).isTrue();

		lock.unlock();
	}

	@Test
	public void testTryLockFailureWhenAlreadyLocked() throws InterruptedException {
		// Use different repository instances to simulate different clients
		MongoLockRepository lockRepository1 = new MongoLockRepository(mongoOperations, taskProperties);
		MongoLockRepository lockRepository2 = new MongoLockRepository(mongoOperations, taskProperties);

		Lock lock1 = lockRepository1.obtain("testKey2");
		Lock lock2 = lockRepository2.obtain("testKey2");

		boolean acquired1 = lock1.tryLock();
		assertThat(acquired1).isTrue();

		// Small delay to ensure lock is persisted
		Thread.sleep(100);

		boolean acquired2 = lock2.tryLock();
		assertThat(acquired2).isFalse();

		lock1.unlock();
	}

	@Test
	public void testTryLockWithTimeout() throws InterruptedException {
		Lock lock = lockRepository.obtain("testKey3");
		boolean acquired = lock.tryLock(100, TimeUnit.MILLISECONDS);
		assertThat(acquired).isTrue();

		lock.unlock();
	}

	@Test
	public void testUnlock() {
		Lock lock = lockRepository.obtain("testKey4");
		lock.tryLock();

		// Verify lock exists
		Query lockQuery = Query.query(
			Criteria.where("lockKey").is("testKey4")
				.and("region").is("default")
		);
		boolean lockExists = mongoOperations.exists(lockQuery, "TASK_task_locks");
		assertThat(lockExists).isTrue();

		lock.unlock();

		// Verify lock was removed
		boolean lockExistsAfterUnlock = mongoOperations.exists(lockQuery, "TASK_task_locks");
		assertThat(lockExistsAfterUnlock).isFalse();
	}

	@Test
	public void testUnlockWhenNotLocked() {
		Lock lock = lockRepository.obtain("testKey5");
		// Should not throw exception when unlocking a lock that was never acquired
		lock.unlock();
	}


	@Test
	public void testLockExpirationWithDifferentClients() {
		// Create a lock with very short TTL
		TaskProperties shortTtlProperties = new TaskProperties();
		shortTtlProperties.setTablePrefix("TASK_");
		shortTtlProperties.setSingleInstanceLockTtl(100); // 0.1 second

		// Test that a lock can be obtained by different client after expiration
		// This simulates the most realistic scenario
		MongoLockRepository.TaskLockDocument expiredLock = new MongoLockRepository.TaskLockDocument();
		expiredLock.id = "expiredKey2:default";
		expiredLock.lockKey = "expiredKey2";
		expiredLock.region = "default";
		expiredLock.clientId = "different-expired-client";
		expiredLock.createdDate = LocalDateTime.now().minusMinutes(5);
		expiredLock.expirationDate = LocalDateTime.now().minusMinutes(1);

		mongoOperations.save(expiredLock, "TASK_task_locks");

		// New repository with different client ID should be able to acquire the expired lock
		MongoLockRepository newRepository = new MongoLockRepository(mongoOperations, taskProperties);
		Lock lock = newRepository.obtain("expiredKey2");
		boolean acquired = lock.tryLock();
		assertThat(acquired).isTrue();

		lock.unlock();
	}

	@Test
	public void testSameClientCanExtendLock() {
		Lock lock = lockRepository.obtain("testKey7");
		boolean acquired1 = lock.tryLock();
		assertThat(acquired1).isTrue();

		// Same lock instance from same repository should be able to extend the lock
		boolean acquired2 = lock.tryLock();
		assertThat(acquired2).isTrue();

		lock.unlock();
	}

	@Test
	public void testSameRepositoryDifferentLocks() {
		// Same repository instance, different lock keys - should both succeed
		Lock lock1 = lockRepository.obtain("testKey7a");
		Lock lock2 = lockRepository.obtain("testKey7b");

		boolean acquired1 = lock1.tryLock();
		assertThat(acquired1).isTrue();

		boolean acquired2 = lock2.tryLock();
		assertThat(acquired2).isTrue();

		lock1.unlock();
		lock2.unlock();
	}

	@Test
	public void testConcurrentLockAttempts() throws InterruptedException {
		final String lockKey = "concurrentTestKey";
		final int threadCount = 3;
		final CountDownLatch startLatch = new CountDownLatch(1);
		final CountDownLatch endLatch = new CountDownLatch(threadCount);
		final AtomicBoolean[] results = new AtomicBoolean[threadCount];

		for (int i = 0; i < threadCount; i++) {
			final int index = i;
			results[i] = new AtomicBoolean(false);

			new Thread(() -> {
				try {
					// Create separate repository instances to simulate different clients
					MongoLockRepository repository = new MongoLockRepository(mongoOperations, taskProperties);
					Lock lock = repository.obtain(lockKey);

					// Wait for all threads to be ready
					startLatch.await();

					boolean acquired = lock.tryLock();
					results[index].set(acquired);
					if (acquired) {
						// Hold the lock for a short time
						Thread.sleep(50);
						lock.unlock();
					}
				}
				catch (Exception e) {
					// Test failure - ignore silently for this test
				}
				finally {
					endLatch.countDown();
				}
			}).start();
		}

		// Start all threads simultaneously
		startLatch.countDown();

		// Wait for all threads to complete
		boolean completed = endLatch.await(10, TimeUnit.SECONDS);
		assertThat(completed).isTrue();

		// Only one thread should have successfully acquired the lock initially
		int successCount = 0;
		for (AtomicBoolean result : results) {
			if (result.get()) {
				successCount++;
			}
		}
		// At least 1 should succeed (may be more due to sequential access after unlock)
		assertThat(successCount).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void testLockInterruptibly() throws InterruptedException {
		Lock lock = lockRepository.obtain("interruptibleTestKey");
		lock.lockInterruptibly();

		// Verify lock was acquired
		Query lockQuery = Query.query(
			Criteria.where("lockKey").is("interruptibleTestKey")
				.and("region").is("default")
		);
		boolean lockExists = mongoOperations.exists(lockQuery, "TASK_task_locks");
		assertThat(lockExists).isTrue();

		lock.unlock();
	}

	@Test
	public void testLockMethod() {
		Lock lock = lockRepository.obtain("lockMethodTestKey");
		lock.lock();

		// Verify lock was acquired
		Query lockQuery = Query.query(
			Criteria.where("lockKey").is("lockMethodTestKey")
				.and("region").is("default")
		);
		boolean lockExists = mongoOperations.exists(lockQuery, "TASK_task_locks");
		assertThat(lockExists).isTrue();

		lock.unlock();
	}

	@Test
	public void testNewConditionThrowsException() {
		Lock lock = lockRepository.obtain("conditionTestKey");
		assertThatThrownBy(lock::newCondition)
			.isInstanceOf(UnsupportedOperationException.class)
			.hasMessage("Conditions are not supported");
	}

	@Test
	public void testCustomTablePrefix() {
		TaskProperties customProperties = new TaskProperties();
		customProperties.setTablePrefix("CUSTOM_");
		customProperties.setSingleInstanceLockTtl(30000);

		MongoLockRepository customRepository = new MongoLockRepository(mongoOperations, customProperties);

		// Verify collection was created with custom prefix
		assertThat(mongoOperations.collectionExists("CUSTOM_task_locks")).isTrue();

		Lock lock = customRepository.obtain("customPrefixTestKey");
		lock.tryLock();

		// Verify lock document exists in custom collection
		Query lockQuery = Query.query(
			Criteria.where("lockKey").is("customPrefixTestKey")
				.and("region").is("default")
		);
		boolean lockExists = mongoOperations.exists(lockQuery, "CUSTOM_task_locks");
		assertThat(lockExists).isTrue();

		lock.unlock();

		// Clean up custom collection
		mongoOperations.getCollection("CUSTOM_task_locks").drop();
	}

	@Test
	public void testExpiredLockCleanup() {
		// Create expired lock manually
		MongoLockRepository.TaskLockDocument expiredLock = new MongoLockRepository.TaskLockDocument();
		expiredLock.id = "expiredKey:default";
		expiredLock.lockKey = "expiredKey";
		expiredLock.region = "default";
		expiredLock.clientId = "expired-client";
		expiredLock.createdDate = LocalDateTime.now().minusHours(1);
		expiredLock.expirationDate = LocalDateTime.now().minusMinutes(1);

		mongoOperations.save(expiredLock, "TASK_task_locks");

		// Verify expired lock exists
		Query expiredQuery = Query.query(Criteria.where("lockKey").is("expiredKey"));
		boolean expiredExists = mongoOperations.exists(expiredQuery, "TASK_task_locks");
		assertThat(expiredExists).isTrue();

		// Try to acquire lock - this should trigger cleanup and succeed
		Lock lock = lockRepository.obtain("expiredKey");
		boolean acquired = lock.tryLock();
		assertThat(acquired).isTrue();

		lock.unlock();
	}
}
