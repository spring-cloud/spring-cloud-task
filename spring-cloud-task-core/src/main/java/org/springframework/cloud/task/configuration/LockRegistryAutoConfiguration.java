/*
 *  Copyright 2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.configuration;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.support.locks.PassThruLockRegistry;

/**
 * Autoconfiguration of
 * {@link org.springframework.integration.support.locks.LockRegistry}.
 *
 * @author Glenn Renfro
 * @since 2.0.0
 */
@Configuration
public class LockRegistryAutoConfiguration {

	@Autowired(required = false)
	DataSource dataSource;

	@Autowired
	private TaskProperties taskProperties;

	@Bean
	@ConditionalOnMissingBean
	public LockRegistry lockRegistry() {
		if (dataSource == null) {
			return new PassThruLockRegistry();
		}
		DefaultLockRepository lockRepository = new DefaultLockRepository(this.dataSource);
		lockRepository.setPrefix(taskProperties.getTablePrefix());
		lockRepository.setTimeToLive(taskProperties.getSingleInstanceLockTtl());
		lockRepository.afterPropertiesSet();
		return new JdbcLockRegistry(lockRepository);
	}

}
