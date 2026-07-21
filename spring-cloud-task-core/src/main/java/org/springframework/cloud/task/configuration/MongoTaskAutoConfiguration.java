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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.task.repository.dao.MongoLockRepository;
import org.springframework.cloud.task.repository.support.MongoTaskRepositoryInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Auto-configuration for MongoDB-based Task repository.
 * This configuration is activated when MongoDB is available and configured,
 * and when the appropriate properties are set to enable MongoDB task storage.
 *
 * @author JongJun Kim
 */
@AutoConfiguration
@ConditionalOnClass({ MongoOperations.class })
@ConditionalOnBean(MongoOperations.class)
@ConditionalOnProperty(prefix = "spring.cloud.task", name = "repository-type", havingValue = "mongodb", matchIfMissing = false)
@EnableConfigurationProperties(TaskProperties.class)
public class MongoTaskAutoConfiguration {

	/**
	 * MongoDB Repository Initializer configuration.
	 */
	@Bean
	@ConditionalOnMissingBean(MongoTaskRepositoryInitializer.class)
	public MongoTaskRepositoryInitializer mongoTaskRepositoryInitializer(MongoOperations mongoOperations,
			TaskProperties taskProperties) {
		return new MongoTaskRepositoryInitializer(mongoOperations, taskProperties);
	}

	/**
	 * MongoDB Lock Registry configuration for single instance task execution.
	 */
	@Bean
	@ConditionalOnMissingBean(LockRegistry.class)
	@ConditionalOnProperty(prefix = "spring.cloud.task", name = "single-instance-enabled", havingValue = "true")
	public LockRegistry mongoLockRegistry(MongoOperations mongoOperations, TaskProperties taskProperties) {
		return new MongoLockRepository(mongoOperations, taskProperties);
	}

	/**
	 * Configuration for MongoDB Task components when no custom {@link TaskConfigurer} is provided.
	 */
	@Configuration
	@ConditionalOnMissingBean(TaskConfigurer.class)
	public static class MongoTaskConfigurerConfiguration {

		/**
		 * Creates a {@link MongoTaskConfigurer} with transaction manager when MongoDB is
		 * the configured repository type, no custom {@link TaskConfigurer} bean exists,
		 * and a {@link PlatformTransactionManager} is available.
		 * @param mongoOperations the {@link MongoOperations} instance
		 * @param taskProperties the {@link TaskProperties} configuration
		 * @param transactionManager the {@link PlatformTransactionManager} for transaction support
		 * @return a configured {@link MongoTaskConfigurer} with transaction support
		 */
		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(PlatformTransactionManager.class)
		public TaskConfigurer taskConfigurer(MongoOperations mongoOperations,
				TaskProperties taskProperties, PlatformTransactionManager transactionManager) {
			return new MongoTaskConfigurer(mongoOperations, taskProperties, transactionManager);
		}

		/**
		 * Creates a {@link MongoTaskConfigurer} when MongoDB is the configured repository type
		 * and no custom {@link TaskConfigurer} bean exists.
		 * @param mongoOperations the {@link MongoOperations} instance
		 * @param taskProperties the {@link TaskProperties} configuration
		 * @return a configured {@link MongoTaskConfigurer}
		 */
		@Bean
		@ConditionalOnMissingBean
		public TaskConfigurer taskConfigurerWithoutTransactionManager(MongoOperations mongoOperations, TaskProperties taskProperties) {
			return new MongoTaskConfigurer(mongoOperations, taskProperties);
		}
	}
}
