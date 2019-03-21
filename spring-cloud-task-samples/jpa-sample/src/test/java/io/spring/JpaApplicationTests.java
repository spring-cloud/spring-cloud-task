/*
 * Copyright 2017-2019 the original author or authors.
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

package io.spring;

import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.h2.tools.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a JPA Application can write its data to a repository.
 *
 * @author Glenn Renfro
 */
public class JpaApplicationTests {

	private final static String DATASOURCE_URL;

	private final static String DATASOURCE_USER_NAME = "SA";

	private final static String DATASOURCE_USER_PASSWORD = "";

	private final static String DATASOURCE_DRIVER_CLASS_NAME = "org.h2.Driver";

	private static int randomPort;

	static {
		randomPort = SocketUtils.findAvailableTcpPort();
		DATASOURCE_URL = "jdbc:h2:tcp://localhost:" + randomPort + "/mem:dataflow;DB_CLOSE_DELAY=-1;"
			+ "DB_CLOSE_ON_EXIT=FALSE";
	}

	@Rule
	public OutputCapture outputCapture = new OutputCapture();
	private ConfigurableApplicationContext context;
	private DataSource dataSource;
	private Server server;

	@Before
	public void setup() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(DATASOURCE_DRIVER_CLASS_NAME);
		dataSource.setUrl(DATASOURCE_URL);
		dataSource.setUsername(DATASOURCE_USER_NAME);
		dataSource.setPassword(DATASOURCE_USER_PASSWORD);
		this.dataSource = dataSource;
		try {
			this.server = Server
				.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", String
					.valueOf(randomPort))
				.start();
		}
		catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	@After
	public void tearDown() {
		if (this.context != null && this.context.isActive()) {
			this.context.close();
		}
		this.server.stop();
	}

	@Test
	public void testBatchJobApp() {
		final String INSERT_MESSAGE = "Hibernate: insert into task_run_output (";
		this.context = SpringApplication
			.run(JpaApplication.class, "--spring.datasource.url=" + DATASOURCE_URL,
				"--spring.datasource.username=" + DATASOURCE_USER_NAME,
				"--spring.datasource.driverClassName=" + DATASOURCE_DRIVER_CLASS_NAME,
				"--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect");
		String output = this.outputCapture.toString();
		assertThat(output
			.contains(INSERT_MESSAGE)).as("Unable to find the insert message: " + output)
			.isTrue();
		JdbcTemplate template = new JdbcTemplate(this.dataSource);
		Map<String, Object> result = template
			.queryForMap("Select * from TASK_RUN_OUTPUT");
		assertThat(result.get("ID")).isEqualTo(1L);
		assertThat(((String) result.get("OUTPUT"))).contains("Executed at");
	}

}
