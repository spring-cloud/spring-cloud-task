/*
 * Copyright 2020-2022 the original author or authors.
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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.h2.tools.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

//import org.springframework.batch.test.AssertFile;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.core.io.ClassPathResource;
//import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.TestSocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the SingleStepBatch Job for various scenarios are created properly.
 *
 * @author Glenn Renfro
 */
public class BatchJobApplicationTests {

	private final static String DATASOURCE_URL;

	private final static String DATASOURCE_USER_NAME = "SA";

	private final static String DATASOURCE_USER_PASSWORD = "''";

	private final static String DATASOURCE_DRIVER_CLASS_NAME = "org.h2.Driver";

	private static int randomPort;

	private static Server defaultServer;

	static {
		randomPort = TestSocketUtils.findAvailableTcpPort();
		DATASOURCE_URL = "jdbc:h2:tcp://localhost:" + randomPort + "/mem:dataflow;DB_CLOSE_DELAY=-1;"
				+ "DB_CLOSE_ON_EXIT=FALSE";
	}

	private File outputFile;

	@BeforeEach
	public void setup() throws Exception {
		outputFile = new File("result.txt");
		initH2TCPServer();
	}

	@AfterEach
	public void tearDown() throws Exception {
		Files.deleteIfExists(Paths.get(outputFile.getAbsolutePath()));
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(DATASOURCE_DRIVER_CLASS_NAME);
		dataSource.setUrl(DATASOURCE_URL);
		dataSource.setUsername(DATASOURCE_USER_NAME);
		dataSource.setPassword(DATASOURCE_USER_PASSWORD);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("TRUNCATE TABLE item");
	}

	@Test
	public void testFileReaderJdbcWriter() throws Exception {
		getSpringApplication().run(SingleStepBatchJobApplication.class, "--spring.profiles.active=ffreader,jdbcwriter",
				"--spring.datasource.username=" + DATASOURCE_USER_NAME, "--spring.datasource.url=" + DATASOURCE_URL,
				"--spring.datasource.driver-class-name=" + DATASOURCE_DRIVER_CLASS_NAME,
				"--spring.datasource.password=" + DATASOURCE_USER_PASSWORD, "foo=testFileReaderJdbcWriter");
		validateDBResult();
	}

	@Test
	public void testJdbcReaderJdbcWriter() throws Exception {
		getSpringApplication().run(SingleStepBatchJobApplication.class,
				"--spring.profiles.active=jdbcreader,jdbcwriter",
				"--spring.datasource.username=" + DATASOURCE_USER_NAME, "--spring.datasource.url=" + DATASOURCE_URL,
				"--spring.datasource.driver-class-name=" + DATASOURCE_DRIVER_CLASS_NAME,
				"--spring.datasource.password=" + DATASOURCE_USER_PASSWORD, "foo=testJdbcReaderJdbcWriter");
		validateDBResult();
	}

	@Test
	public void testJdbcReaderFlatfileWriter() throws Exception {
		getSpringApplication().run(SingleStepBatchJobApplication.class, "--spring.profiles.active=jdbcreader,ffwriter",
				"--spring.datasource.username=" + DATASOURCE_USER_NAME, "--spring.datasource.url=" + DATASOURCE_URL,
				"--spring.datasource.driver-class-name=" + DATASOURCE_DRIVER_CLASS_NAME,
				"--spring.datasource.password=" + DATASOURCE_USER_PASSWORD, "foo=testJdbcReaderFlatfileWriter");
		validateFileResult();
	}

	@Test
	public void testFileReaderFileWriter() throws Exception {
		getSpringApplication().run(SingleStepBatchJobApplication.class, "--spring.profiles.active=ffreader,ffwriter",
				"foo=testFileReaderFileWriter");
		validateFileResult();
	}

	public Server initH2TCPServer() throws SQLException {
		Server server;

		if (defaultServer == null) {
			server = Server
				.createTcpServer("-ifNotExists", "-tcp", "-tcpAllowOthers", "-tcpPort", String.valueOf(randomPort))
				.start();
			defaultServer = server;
			DriverManagerDataSource dataSource = new DriverManagerDataSource();
			dataSource.setDriverClassName(DATASOURCE_DRIVER_CLASS_NAME);
			dataSource.setUrl(DATASOURCE_URL);
			dataSource.setUsername(DATASOURCE_USER_NAME);
			dataSource.setPassword(DATASOURCE_USER_PASSWORD);
			ClassPathResource setupResource = new ClassPathResource("schema-h2.sql");
			ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator(setupResource);
			resourceDatabasePopulator.execute(dataSource);
		}

		return defaultServer;
	}

	private void validateFileResult() throws Exception {
		// AssertFile.assertLineCount(6, new FileSystemResource("./result.txt"));
		// AssertFile.assertFileEquals(new ClassPathResource("testresult.txt"),
		// new FileSystemResource(this.outputFile));
	}

	private void validateDBResult() {
		DataSource dataSource = getDataSource();
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		List<Map<String, Object>> result = jdbcTemplate.queryForList("SELECT item_name FROM item ORDER BY item_name");
		assertThat(result.size()).isEqualTo(6);

		assertThat(result.get(0).get("item_name")).isEqualTo("Job");
		assertThat(result.get(1).get("item_name")).isEqualTo("bar");
		assertThat(result.get(2).get("item_name")).isEqualTo("baz");
		assertThat(result.get(3).get("item_name")).isEqualTo("boo");
		assertThat(result.get(4).get("item_name")).isEqualTo("foo");
		assertThat(result.get(5).get("item_name")).isEqualTo("qux");
	}

	private DataSource getDataSource() {
		DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
		dataSourceBuilder.driverClassName(DATASOURCE_DRIVER_CLASS_NAME);
		dataSourceBuilder.url(DATASOURCE_URL);
		dataSourceBuilder.username(DATASOURCE_USER_NAME);
		dataSourceBuilder.password(DATASOURCE_USER_PASSWORD);
		return dataSourceBuilder.build();
	}

	private SpringApplication getSpringApplication() {
		SpringApplication springApplication = new SpringApplication();
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.application.name", "Single Step Batch Job");
		properties.put("spring.batch.job.jobName", "job");
		properties.put("spring.batch.job.stepName", "step1");
		properties.put("spring.batch.job.chunkSize", "5");
		springApplication.setDefaultProperties(properties);
		return springApplication;
	}

}
