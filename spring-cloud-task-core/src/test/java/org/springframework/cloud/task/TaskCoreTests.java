/*
 * Copyright 2015-2018 the original author or authors.
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

package org.springframework.cloud.task;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.junit.Assert.assertTrue;

/**
 * Verifies core behavior for Tasks.
 *
 * @author Glenn Renfro
 */
public class TaskCoreTests {

	private static final String TASK_NAME = "taskEventTest";
	private static final String EXCEPTION_MESSAGE = "FOO EXCEPTION";
	private static final String CREATE_TASK_MESSAGE = "Creating: TaskExecution{executionId=";
	private static final String UPDATE_TASK_MESSAGE = "Updating: TaskExecution with executionId=";
	private static final String SUCCESS_EXIT_CODE_MESSAGE = "with the following {exitCode=0";
	private static final String EXCEPTION_EXIT_CODE_MESSAGE = "with the following {exitCode=1";
	private static final String EXCEPTION_INVALID_TASK_EXECUTION_ID =
			"java.lang.IllegalArgumentException: Invalid TaskExecution, ID 55 not found";
	private static final String ERROR_MESSAGE =
			"errorMessage='java.lang.IllegalStateException: Failed to execute CommandLineRunner";

	private ConfigurableApplicationContext applicationContext;

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@After
	public void teardown() {
		if (applicationContext != null && applicationContext.isActive()) {
			applicationContext.close();
		}
	}

	@Test
	public void successfulTaskTest() {
		this.applicationContext = SpringApplication.run( TaskConfiguration.class,
				"--spring.cloud.task.closecontext.enable=false",
				"--spring.cloud.task.name=" + TASK_NAME,
				"--spring.main.web-environment=false");

		String output = this.outputCapture.toString();
		assertTrue("Test results do not show create task message: " + output,
				output.contains(CREATE_TASK_MESSAGE));
		assertTrue("Test results do not show success message: " + output,
				output.contains(UPDATE_TASK_MESSAGE));
		assertTrue("Test results have incorrect exit code: " + output,
				output.contains(SUCCESS_EXIT_CODE_MESSAGE));
	}

	/**
	 * Test to verify that deprecated annotation does not affect task execution.
	 */
	@Test
	public void successfulTaskTestWithAnnotation() {
		this.applicationContext = SpringApplication.run( TaskConfigurationWithAnotation.class,
				"--spring.cloud.task.closecontext.enable=false",
				"--spring.cloud.task.name=" + TASK_NAME,
				"--spring.main.web-environment=false");

		String output = this.outputCapture.toString();
		assertTrue("Test results do not show create task message: " + output,
				output.contains(CREATE_TASK_MESSAGE));
		assertTrue("Test results do not show success message: " + output,
				output.contains(UPDATE_TASK_MESSAGE));
		assertTrue("Test results have incorrect exit code: " + output,
				output.contains(SUCCESS_EXIT_CODE_MESSAGE));
	}

	@Test
	public void exceptionTaskTest() {
		boolean exceptionFired = false;
		try {
			this.applicationContext = SpringApplication.run( TaskExceptionConfiguration.class,
					"--spring.cloud.task.closecontext.enable=false",
					"--spring.cloud.task.name=" + TASK_NAME,
					"--spring.main.web-environment=false");
		}
		catch (IllegalStateException exception) {
			exceptionFired = true;
		}
		assertTrue("An IllegalStateException should have been thrown", exceptionFired);

		String output = this.outputCapture.toString();
		assertTrue("Test results do not show create task message: " + output,
				output.contains(CREATE_TASK_MESSAGE));
		assertTrue("Test results do not show success message: " + output,
				output.contains(UPDATE_TASK_MESSAGE));
		assertTrue("Test results have incorrect exit code: " + output,
				output.contains(EXCEPTION_EXIT_CODE_MESSAGE));
		assertTrue("Test results have incorrect exit message: " + output,
				output.contains(ERROR_MESSAGE));
		assertTrue("Test results have exception message: " + output,
				output.contains(EXCEPTION_MESSAGE));
	}

	@Test
	public void invalidExecutionId() {
		boolean exceptionFired = false;
		try {
			this.applicationContext = SpringApplication.run(
							TaskExceptionConfiguration.class, "--spring.cloud.task.closecontext.enable=false",
					"--spring.cloud.task.name=" + TASK_NAME,
					"--spring.main.web-environment=false",
					"--spring.cloud.task.executionid=55");
		}
		catch (ApplicationContextException exception) {
			exceptionFired = true;
		}
		assertTrue("An ApplicationContextException should have been thrown", exceptionFired);

		String output = this.outputCapture.toString();
		assertTrue("Test results do not show the correct exception message: " + output,
				output.contains(EXCEPTION_INVALID_TASK_EXECUTION_ID));
	}

	@EnableTask
	@ImportAutoConfiguration({SimpleTaskAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class})
	public static class TaskConfiguration {

		@Bean
		public CommandLineRunner commandLineRunner() {
			return new CommandLineRunner() {
				@Override
				public void run(String... strings) throws Exception {
				}
			};
		}
	}

	@EnableTask
	@ImportAutoConfiguration({SimpleTaskAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class})
	public static class TaskConfigurationWithAnotation {

		@Bean
		public CommandLineRunner commandLineRunner() {
			return new CommandLineRunner() {
				@Override
				public void run(String... strings) throws Exception {
				}
			};
		}
	}

	@EnableTask
	@ImportAutoConfiguration({SimpleTaskAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class})
	public static class TaskExceptionConfiguration {

		@Bean
		public CommandLineRunner commandLineRunner() {
			return new CommandLineRunner() {
				@Override
				public void run(String... strings) throws Exception {
					throw new IllegalStateException(EXCEPTION_MESSAGE);
				}
			};
		}
	}
}
