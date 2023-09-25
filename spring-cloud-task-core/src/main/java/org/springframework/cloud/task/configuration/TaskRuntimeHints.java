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

import java.sql.Statement;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.util.ClassUtils;

/**
 * Native Hints for Spring Cloud Task.
 *
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * @since 3.0
 */
public class TaskRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		hints.reflection().registerType(TypeReference.of("java.sql.DatabaseMetaData"), hint -> {
		});
		hints.resources().registerPattern("org/springframework/cloud/task/schema-db2.sql");
		hints.resources().registerPattern("org/springframework/cloud/task/schema-h2.sql");
		hints.resources().registerPattern("org/springframework/cloud/task/schema-mysql.sql");
		hints.resources().registerPattern("org/springframework/cloud/task/schema-mariadb.sql");
		hints.resources().registerPattern("org/springframework/cloud/task/schema-oracle.sql");
		hints.resources().registerPattern("org/springframework/cloud/task/schema-postgresql.sql");
		hints.resources().registerPattern("org/springframework/cloud/task/schema-hsqldb.sql");
		hints.resources().registerPattern("org/springframework/cloud/task/schema-sqlserver.sql");

		hints.reflection()
			.registerType(TypeReference.of("org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer"),
					hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.INVOKE_DECLARED_METHODS));
		hints.reflection()
			.registerType(TypeReference.of("org.springframework.cloud.task.repository.TaskExecution"), hint -> hint
				.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS));

		if (!ClassUtils.isPresent("com.zaxxer.hikari.HikariDataSource", classLoader)) {
			return;
		}
		hints.reflection().registerType(Statement[].class, hint -> {
		});
		hints.reflection()
			.registerType(TypeReference.of("com.zaxxer.hikari.util.ConcurrentBag$IConcurrentBagEntry[]"), hint -> {
			});
	}

}
