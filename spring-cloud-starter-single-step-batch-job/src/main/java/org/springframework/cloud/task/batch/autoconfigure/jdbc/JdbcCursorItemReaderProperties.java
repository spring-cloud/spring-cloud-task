/*
 * Copyright 2020-present the original author or authors.
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
 * @author Michael Minella
 * @since 2.3
 */
@ConfigurationProperties(prefix = "spring.batch.job.jdbccursoritemreader")
public class JdbcCursorItemReaderProperties {

	/**
	 * Configure whether the state of the
	 * {@link org.springframework.batch.item.ItemStreamSupport} should be persisted within
	 * the {@link org.springframework.batch.item.ExecutionContext} for restart purposes.
	 * Defaults to {@code true}.
	 */
	private boolean saveState = true;

	/**
	 * Returns the configured value of the name used to calculate {@code ExecutionContext}
	 * keys.
	 */
	private String name;

	/**
	 * Configure the maximum number of items to be read.
	 */
	private int maxItemCount = Integer.MAX_VALUE;

	/**
	 * Index for the current item. Also used on restarts to indicate where to start from.
	 * Defaults to 0.
	 */
	private int currentItemCount = 0;

	/**
	 * The number of items to return each time the cursor fetches from the server.
	 */
	private int fetchSize;

	/**
	 * Sets the maximum number of rows to be read with this reader.
	 */
	private int maxRows;

	/**
	 * The time in milliseconds for the query to timeout.
	 */
	private int queryTimeout;

	/**
	 * Establishes whether SQL warnings should be ignored. Defaults to {@code false}.
	 */
	private boolean ignoreWarnings;

	/**
	 * Sets whether the cursor's position should be validated with each item read.
	 * Defaults to {@code false}.
	 */
	private boolean verifyCursorPosition;

	/**
	 * Establishes {@code false} the driver supports absolute positioning of a cursor.
	 * Defaults to {@code false}.
	 */
	private boolean driverSupportsAbsolute;

	/**
	 * Establishes whether the connection used for the cursor is being used by all other
	 * processing and is, therefore, part of the same transaction. Defaults to
	 * {@code false}
	 */
	private boolean useSharedExtendedConnection;

	/**
	 * The SQL query to be executed.
	 */
	private String sql;

	/**
	 * Returns the configured value of if the state of the reader will be persisted.
	 * @return true if the state will be persisted
	 */
	public boolean isSaveState() {
		return this.saveState;
	}

	/**
	 * Configure if the state of the
	 * {@link org.springframework.batch.item.ItemStreamSupport} should be persisted within
	 * the {@link org.springframework.batch.item.ExecutionContext} for restart purposes.
	 * @param saveState defaults to true
	 */
	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}

	/**
	 * Returns the configured value of the name used to calculate {@code ExecutionContext}
	 * keys.
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}. Required if
	 * {@link #setSaveState} is set to true.
	 * @param name name of the reader instance
	 * @see org.springframework.batch.item.ItemStreamSupport#setName(String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * The maximum number of items to be read.
	 * @return the configured number of items, defaults to Integer.MAX_VALUE
	 */
	public int getMaxItemCount() {
		return this.maxItemCount;
	}

	/**
	 * Configure the max number of items to be read.
	 * @param maxItemCount the max items to be read
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
	 */
	public void setMaxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;
	}

	/**
	 * Provides the index of the current item.
	 * @return item index
	 */
	public int getCurrentItemCount() {
		return this.currentItemCount;
	}

	/**
	 * Index for the current item. Also used on restarts to indicate where to start from.
	 * @param currentItemCount current index
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setCurrentItemCount(int)
	 */
	public void setCurrentItemCount(int currentItemCount) {
		this.currentItemCount = currentItemCount;
	}

	/**
	 * Provides the number of items to return each time the cursor fetches from the
	 * server.
	 * @return fetch size
	 */
	public int getFetchSize() {
		return fetchSize;
	}

	/**
	 * Sets the number of items to return each time the cursor fetches from the server.
	 * @param fetchSize the number of items
	 * @see org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder#fetchSize(int)
	 */
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	/**
	 * Provides the maximum number of rows to read with this reader.
	 * @return maxiumum number of items
	 */
	public int getMaxRows() {
		return maxRows;
	}

	/**
	 * Sets the maximum number of rows to be read with this reader.
	 * @param maxRows maximum number of items
	 * @see org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder#maxRows(int)
	 */
	public void setMaxRows(int maxRows) {
		this.maxRows = maxRows;
	}

	/**
	 * Provides the time in milliseconds for the query to timeout.
	 * @return milliseconds for the timeout
	 */
	public int getQueryTimeout() {
		return queryTimeout;
	}

	/**
	 * Sets the time in milliseconds for the query to timeout.
	 * @param queryTimeout milliseconds
	 * @see org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder#queryTimeout(int)
	 */
	public void setQueryTimeout(int queryTimeout) {
		this.queryTimeout = queryTimeout;
	}

	/**
	 * Provides if SQL warnings should be ignored.
	 * @return true if warnings should be ignored
	 */
	public boolean isIgnoreWarnings() {
		return ignoreWarnings;
	}

	/**
	 * Sets if SQL warnings should be ignored.
	 * @param ignoreWarnings indicator if the warnings should be ignored
	 * @see org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder#ignoreWarnings(boolean)
	 */
	public void setIgnoreWarnings(boolean ignoreWarnings) {
		this.ignoreWarnings = ignoreWarnings;
	}

	/**
	 * Indicates if the cursor's position should be validated with each item read (to
	 * confirm that the RowMapper has not moved the cursor's location).
	 * @return true if the position should be validated
	 */
	public boolean isVerifyCursorPosition() {
		return verifyCursorPosition;
	}

	/**
	 * Provides if the cursor's position should be validated with each item read.
	 * @param verifyCursorPosition true if the position should be validated
	 * @see org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder#verifyCursorPosition(boolean)
	 */
	public void setVerifyCursorPosition(boolean verifyCursorPosition) {
		this.verifyCursorPosition = verifyCursorPosition;
	}

	/**
	 * Provides if the driver supports absolute positioning of a cursor.
	 * @return true if the driver supports absolute positioning
	 */
	public boolean isDriverSupportsAbsolute() {
		return driverSupportsAbsolute;
	}

	/**
	 * Sets if the driver supports absolute positioning of a cursor.
	 * @param driverSupportsAbsolute true if the driver supports absolute positioning
	 * @see org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder#driverSupportsAbsolute(boolean)
	 */
	public void setDriverSupportsAbsolute(boolean driverSupportsAbsolute) {
		this.driverSupportsAbsolute = driverSupportsAbsolute;
	}

	/**
	 * Sets whether the connection used for the cursor is being used by all other
	 * processing and is, therefore, part of the same transaction.
	 * @return true if the connection is shared beyond this query
	 */
	public boolean isUseSharedExtendedConnection() {
		return useSharedExtendedConnection;
	}

	/**
	 * Sets whether the the connection used for the cursor is being used by all other
	 * processing and is, therefore, part of the same transaction.
	 * @param useSharedExtendedConnection true if the connection is shared beyond this
	 * query
	 * @see org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder#useSharedExtendedConnection(boolean)
	 */
	public void setUseSharedExtendedConnection(boolean useSharedExtendedConnection) {
		this.useSharedExtendedConnection = useSharedExtendedConnection;
	}

	/**
	 * Returns the SQL query to be executed.
	 * @return the SQL query
	 */
	public String getSql() {
		return sql;
	}

	/**
	 * Sets the SQL query to be executed.
	 * @param sql the query
	 * @see org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder#sql(String)
	 */
	public void setSql(String sql) {
		this.sql = sql;
	}

}
