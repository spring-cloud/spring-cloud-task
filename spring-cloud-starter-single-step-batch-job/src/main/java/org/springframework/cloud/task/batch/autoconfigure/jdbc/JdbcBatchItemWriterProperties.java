/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.task.batch.autoconfigure.jdbc;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties to configure a {@code JdbcBatchItemWriter}.
 *
 * @author Glenn Renfro
 * @since 2.3
 */
@ConfigurationProperties(prefix = "spring.batch.job.jdbcbatchitemwriter")
public class JdbcBatchItemWriterProperties {

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}.
	 */
	private String name;

	/**
	 * The SQL statement to be used to update the database.
	 */
	private String sql;

	/**
	 * If set to {@code true}, confirms that every insert results in the update of at
	 * least one row in the database. Defaults to {@code true}.
	 */
	private boolean assertUpdates = true;

	/**
	 * @return The current sql statement used to update the database.
	 */
	public String getSql() {
		return sql;
	}

	/**
	 * Sets the sql statement to be used to update the database.
	 * @param sql the sql statement to be used.
	 */
	public void setSql(String sql) {
		this.sql = sql;
	}

	/**
	 * @return if returns true then each insert will be confirmed to have at least one
	 * insert in the database.
	 */
	public boolean isAssertUpdates() {
		return assertUpdates;
	}

	/**
	 * If set to true, confirms that every insert results in the update of at least one
	 * row in the database. Defaults to True
	 */
	public void setAssertUpdates(boolean assertUpdates) {
		this.assertUpdates = assertUpdates;
	}

	/**
	 * Returns the configured value of the name used to calculate {@code ExecutionContext}
	 * keys.
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}.
	 * @param name name of the writer instance
	 * @see org.springframework.batch.item.ItemStreamSupport#setName(String)
	 */
	public void setName(String name) {
		this.name = name;
	}

}
