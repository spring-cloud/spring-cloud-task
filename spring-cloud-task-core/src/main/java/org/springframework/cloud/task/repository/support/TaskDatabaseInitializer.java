/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.repository.support;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.MetaDataAccessException;

/**
 * @author Glenn Renfro
 */

public class TaskDatabaseInitializer {

	public static final String DEFAULT_TABLE_PREFIX = "";

	private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/springframework/"
			+ "cloud/task/schema-@@platform@@.sql";

	@Autowired(required = false)
	private DataSource dataSource;

	@Autowired
	private ResourceLoader resourceLoader;

	/**
	 * Path to the SQL file to use to initialize the database schema.
	 */
	private String schema = DEFAULT_SCHEMA_LOCATION;

	private String tablePrefix = DEFAULT_TABLE_PREFIX;


	@PostConstruct
	public void initializeDatabase() {
		if (dataSource !=null) {
			String platform = getDatabaseType();
			if ("hsql".equals(platform)) {
				platform = "hsqldb";
			}
			if ("postgres".equals(platform)) {
				platform = "postgresql";
			}
			if ("oracle".equals(platform)) {
				platform = "oracle10g";
			}
			ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
			String schemaLocation = schema;
			schemaLocation = schemaLocation.replace("@@platform@@", platform);
			populator.addScript(resourceLoader.getResource(schemaLocation));
			populator.setContinueOnError(false);
			DatabasePopulatorUtils.execute(populator, this.dataSource);
		}
	}
	private String getDatabaseType() {
		try {
			return DatabaseType.fromMetaData(this.dataSource).toString().toLowerCase();
		}
		catch (MetaDataAccessException ex) {
			throw new IllegalStateException("Unable to detect database type", ex);
		}
	}

}
