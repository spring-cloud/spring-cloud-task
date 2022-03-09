/*
 * Copyright 2015-2022 the original author or authors.
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

import org.h2.tools.Server;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.test.TestSocketUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
@ExtendWith({OutputCaptureExtension.class, SpringExtension.class})
@SpringBootTest(classes = { MultiDataSourcesExternalApplicationTests.TaskLauncherConfiguration.class })
public class MultiDataSourcesExternalApplicationTests {
	private final static String DATASOURCE_URL;

	private final static String SECOND_DATASOURCE_URL;

	private final static String DATASOURCE_USER_NAME = "SA";

	private final static String DATASOURCE_USER_PASSWORD = "''";

	private final static String DATASOURCE_DRIVER_CLASS_NAME = "org.h2.Driver";

	private static int randomPort;

	private static int secondRandomPort;

	static {
		randomPort = TestSocketUtils.findAvailableTcpPort();
		DATASOURCE_URL = "jdbc:h2:tcp://localhost:" + randomPort
			+ "/mem:dataflow;DB_CLOSE_DELAY=-1;" + "DB_CLOSE_ON_EXIT=FALSE";
		secondRandomPort = TestSocketUtils.findAvailableTcpPort();
		SECOND_DATASOURCE_URL = "jdbc:h2:tcp://localhost:" + randomPort
			+ "/mem:dataflow;DB_CLOSE_DELAY=-1;" + "DB_CLOSE_ON_EXIT=FALSE";
	}


	@Test
	public void testTimeStampApp(CapturedOutput capturedOutput) throws Exception {

		SpringApplication.run(MultipleDataSourcesApplication.class, "--spring.profiles.active=external",
			"--spring.datasource.url=" + DATASOURCE_URL,
			"--spring.datasource.username=" + DATASOURCE_USER_NAME,
			"--spring.datasource.password=" + DATASOURCE_USER_PASSWORD,
			"--spring.datasource.driverClassName=" + DATASOURCE_DRIVER_CLASS_NAME,
			"--second.datasource.url=" + SECOND_DATASOURCE_URL,
			"--second.datasource.username=" + DATASOURCE_USER_NAME,
			"--second.datasource.password=" + DATASOURCE_USER_PASSWORD,
			"--second.datasource.driverClassName=" + DATASOURCE_DRIVER_CLASS_NAME);

		String output = capturedOutput.toString();

		assertThat(output.contains("There are 2 DataSources within this application"))
			.as("Unable to find CommandLineRunner output: " + output).isTrue();
		assertThat(output.contains("Creating: TaskExecution{"))
			.as("Unable to find start task message: " + output).isTrue();
		assertThat(output.contains("Updating: TaskExecution"))
			.as("Unable to find update task message: " + output).isTrue();
	}

	@Configuration(proxyBeanMethods = false)
	public static class TaskLauncherConfiguration {

		private static Server defaultServer;

		private static Server secondServer;

		@Bean
		public Server initH2TCPServer() {
			Server server = null;
			try {
				if (defaultServer == null) {
					server = Server.createTcpServer("-ifNotExists", "-tcp",
						"-tcpAllowOthers", "-tcpPort", String.valueOf(randomPort))
						.start();
					defaultServer = server;
				}
			}
			catch (SQLException e) {
				throw new IllegalStateException(e);
			}
			return defaultServer;
		}

		@Bean
		public Server initSecondH2TCPServer() {
			Server server = null;
			try {
				if (secondServer == null) {
					server = Server.createTcpServer("-ifNotExists", "-tcp",
						"-tcpAllowOthers", "-tcpPort", String.valueOf(secondRandomPort))
						.start();
					secondServer = server;
				}
			}
			catch (SQLException e) {
				throw new IllegalStateException(e);
			}
			return secondServer;
		}
	}
}
