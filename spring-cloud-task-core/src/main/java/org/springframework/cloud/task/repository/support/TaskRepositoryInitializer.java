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

import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

/**
 * Utility for initializing the Task Repository's datasource. If a single
 * {@link DataSource} is available in the current context, and functionality is enabled
 * (as it is by default), this will initialize the database. If more than one DataSource
 * is available in the current context, custom configuration of this is required (if
 * desired).
 *
 * Initialization of the database can be disabled by configuring the property
 * <code>spring.cloud.task.initialize-enabled</code> to false.
 * <code>spring.cloud.task.initialize.enable</code> has been deprecated.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 */

public final class TaskRepositoryInitializer implements InitializingBean {

	private static final Log logger = LogFactory.getLog(TaskRepositoryInitializer.class);

	private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/springframework/"
			+ "cloud/task/schema-@@platform@@.sql";

	/**
	 * Path to the SQL file to use to initialize the database schema.
	 */
	private static String schema = DEFAULT_SCHEMA_LOCATION;

	@SuppressWarnings("NullAway.Init")
	private DataSource dataSource;

	@SuppressWarnings("NullAway.Init")
	private ResourceLoader resourceLoader;

	@Value("${spring.cloud.task.initialize.enable:true}")
	private boolean taskInitializationEnabled;

	private TaskProperties taskProperties;

	public TaskRepositoryInitializer(TaskProperties taskProperties) {
		this.taskProperties = taskProperties;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Autowired(required = false)
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	private String getDatabaseType(DataSource dataSource) {
		try {
			DatabaseType databaseType = DatabaseType.fromMetaData(dataSource);
			String databaseTypeName = (databaseType != null) ? databaseType.toString() : null;
			return Objects
				.requireNonNull(JdbcUtils.commonDatabaseName(databaseTypeName), "Common database name is required")
				.toLowerCase(Locale.ROOT);
		}
		catch (MetaDataAccessException ex) {
			throw new IllegalStateException("Unable to detect database type", ex);
		}
		catch (SQLException ex) {
			throw new IllegalStateException("Unable to detect database type", ex);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		boolean isInitializeEnabled = (this.taskProperties.isInitializeEnabled() != null)
				? this.taskProperties.isInitializeEnabled() : this.taskInitializationEnabled;
		if (this.dataSource != null && isInitializeEnabled
				&& this.taskProperties.getTablePrefix().equals(TaskProperties.DEFAULT_TABLE_PREFIX)) {
			String platform = getDatabaseType(this.dataSource);
			if ("hsql".equals(platform)) {
				platform = "hsqldb";
			}
			if ("postgres".equals(platform)) {
				platform = "postgresql";
			}
			if ("oracle".equals(platform)) {
				platform = "oracle";
			}
			if ("mysql".equals(platform)) {
				platform = "mysql";
			}
			if ("sqlserver".equals(platform)) {
				platform = "sqlserver";
			}
			ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
			String schemaLocation = schema;
			schemaLocation = schemaLocation.replace("@@platform@@", platform);
			populator.addScript(this.resourceLoader.getResource(schemaLocation));
			populator.setContinueOnError(true);
			logger.debug(String.format("Initializing task schema for %s database", platform));
			DatabasePopulatorUtils.execute(populator, this.dataSource);
		}
	}

}
