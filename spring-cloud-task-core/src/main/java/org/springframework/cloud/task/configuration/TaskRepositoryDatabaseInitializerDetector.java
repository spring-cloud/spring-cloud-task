/*
 * Copyright 2022-2022 the original author or authors.
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

import java.util.Set;

import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDatabaseInitializerDetector;
import org.springframework.boot.sql.init.dependency.DatabaseInitializerDetector;
import org.springframework.cloud.task.repository.support.TaskRepositoryInitializer;
import org.springframework.core.Ordered;

/**
 * {@link DatabaseInitializerDetector} for {@link TaskRepositoryInitializer}.
 *
 * @author Henning Pöttker
 */
class TaskRepositoryDatabaseInitializerDetector extends AbstractBeansOfTypeDatabaseInitializerDetector {

	private static final int PRECEDENCE = Ordered.LOWEST_PRECEDENCE - 100;

	@Override
	protected Set<Class<?>> getDatabaseInitializerBeanTypes() {
		return Set.of(TaskRepositoryInitializer.class);
	}

	@Override
	public int getOrder() {
		return PRECEDENCE;
	}

}
