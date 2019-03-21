/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.cloud.task.listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ExitCodeEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.util.TestDefaultConfiguration;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the TaskLifecycleListener Methods record the appropriate log header
 * entries and result codes.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 */
public class TaskLifecycleListenerTests {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	private AnnotationConfigApplicationContext context;

	private TaskExplorer taskExplorer;

	@Before
	public void setUp() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.setId("testTask");
		this.context.register(TestDefaultConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		TestListener.getStartupOrderList().clear();
		TestListener.getFailOrderList().clear();
		TestListener.getEndOrderList().clear();

	}

	@After
	public void tearDown() {
		if (this.context != null && this.context.isActive()) {
			this.context.close();
		}
	}

	@Test
	public void testTaskCreate() {
		this.context.refresh();
		this.taskExplorer = this.context.getBean(TaskExplorer.class);
		verifyTaskExecution(0, false);
	}

	@Test
	public void testTaskCreateWithArgs() {
		this.context.register(ArgsConfiguration.class);
		this.context.refresh();
		this.taskExplorer = this.context.getBean(TaskExplorer.class);
		verifyTaskExecution(2, false);
	}

	@Test
	public void testTaskUpdate() {
		this.context.refresh();
		this.taskExplorer = this.context.getBean(TaskExplorer.class);

		this.context.publishEvent(new ApplicationReadyEvent(new SpringApplication(),
				new String[0], this.context));

		verifyTaskExecution(0, true, 0);
	}

	@Test
	public void testTaskFailedUpdate() {
		this.context.refresh();
		RuntimeException exception = new RuntimeException("This was expected");
		SpringApplication application = new SpringApplication();
		this.taskExplorer = this.context.getBean(TaskExplorer.class);
		this.context.publishEvent(new ApplicationFailedEvent(application, new String[0],
				this.context, exception));
		this.context.publishEvent(
				new ApplicationReadyEvent(application, new String[0], this.context));

		verifyTaskExecution(0, true, 1, exception, null);
	}

	@Test
	public void testTaskFailedWithExitCodeEvent() {
		final int exitCode = 10;
		this.context.register(TestListener.class);
		this.context.register(TestListener2.class);

		this.context.refresh();
		RuntimeException exception = new RuntimeException("This was expected");
		SpringApplication application = new SpringApplication();
		this.taskExplorer = this.context.getBean(TaskExplorer.class);
		this.context.publishEvent(new ExitCodeEvent(this.context, exitCode));
		this.context.publishEvent(new ApplicationFailedEvent(application, new String[0],
				this.context, exception));
		this.context.publishEvent(
				new ApplicationReadyEvent(application, new String[0], this.context));

		verifyTaskExecution(0, true, exitCode, exception, null);
		assertThat(TestListener.getStartupOrderList().size()).isEqualTo(2);
		assertThat(TestListener.getStartupOrderList().get(0))
				.isEqualTo(Integer.valueOf(2));
		assertThat(TestListener.getStartupOrderList().get(1))
				.isEqualTo(Integer.valueOf(1));

		assertThat(TestListener.getEndOrderList().size()).isEqualTo(2);
		assertThat(TestListener.getEndOrderList().get(0)).isEqualTo(Integer.valueOf(1));
		assertThat(TestListener.getEndOrderList().get(1)).isEqualTo(Integer.valueOf(2));

		assertThat(TestListener.getFailOrderList().size()).isEqualTo(2);
		assertThat(TestListener.getFailOrderList().get(0)).isEqualTo(Integer.valueOf(1));
		assertThat(TestListener.getFailOrderList().get(1)).isEqualTo(Integer.valueOf(2));

	}

	@Test
	public void testNoClosingOfContext() {

		try (ConfigurableApplicationContext applicationContext = SpringApplication.run(
				new Class[] { TestDefaultConfiguration.class,
						PropertyPlaceholderAutoConfiguration.class },
				new String[] { "--spring.cloud.task.closecontext_enabled=false" })) {
			assertThat(applicationContext.isActive()).isTrue();
		}
	}

	@Test(expected = ApplicationContextException.class)
	public void testInvalidTaskExecutionId() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		Map<String, Object> myMap = new HashMap<>();
		myMap.put("spring.cloud.task.executionid", "55");
		propertySources
				.addFirst(new MapPropertySource("EnvrionmentTestPropsource", myMap));
		this.context.setEnvironment(environment);
		this.context.refresh();
	}

	@Test
	public void testRestartExistingTask() {
		this.context.refresh();
		TaskLifecycleListener taskLifecycleListener = this.context
				.getBean(TaskLifecycleListener.class);
		taskLifecycleListener.start();
		String output = this.outputCapture.toString();
		assertThat(output.contains("Multiple start events have been received"))
				.as("Test results do not show error message: " + output).isTrue();
	}

	@Test
	public void testExternalExecutionId() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		Map<String, Object> myMap = new HashMap<>();
		myMap.put("spring.cloud.task.external-execution-id", "myid");
		propertySources
				.addFirst(new MapPropertySource("EnvrionmentTestPropsource", myMap));
		this.context.setEnvironment(environment);
		this.context.refresh();
		this.taskExplorer = this.context.getBean(TaskExplorer.class);

		verifyTaskExecution(0, false, null, null, "myid");
	}

	@Test
	public void testParentExecutionId() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		Map<String, Object> myMap = new HashMap<>();
		myMap.put("spring.cloud.task.parentExecutionId", 789);
		propertySources
				.addFirst(new MapPropertySource("EnvrionmentTestPropsource", myMap));
		this.context.setEnvironment(environment);
		this.context.refresh();
		this.taskExplorer = this.context.getBean(TaskExplorer.class);

		verifyTaskExecution(0, false, null, null, null, 789L);
	}

	private void verifyTaskExecution(int numberOfParams, boolean update,
			Integer exitCode) {
		verifyTaskExecution(numberOfParams, update, exitCode, null, null);
	}

	private void verifyTaskExecution(int numberOfParams, boolean update) {
		verifyTaskExecution(numberOfParams, update, null, null, null);
	}

	private void verifyTaskExecution(int numberOfParams, boolean update, Integer exitCode,
			Throwable exception, String externalExecutionId) {
		verifyTaskExecution(numberOfParams, update, exitCode, exception,
				externalExecutionId, null);
	}

	private void verifyTaskExecution(int numberOfParams, boolean update, Integer exitCode,
			Throwable exception, String externalExecutionId, Long parentExecutionId) {

		Sort sort = Sort.by("id");

		PageRequest request = PageRequest.of(0, Integer.MAX_VALUE, sort);

		Page<TaskExecution> taskExecutionsByName = this.taskExplorer
				.findTaskExecutionsByName("testTask", request);
		assertThat(taskExecutionsByName.iterator().hasNext()).isTrue();
		TaskExecution taskExecution = taskExecutionsByName.iterator().next();

		assertThat(taskExecution.getArguments().size()).isEqualTo(numberOfParams);
		assertThat(taskExecution.getExitCode()).isEqualTo(exitCode);
		assertThat(taskExecution.getExternalExecutionId()).isEqualTo(externalExecutionId);
		assertThat(taskExecution.getParentExecutionId()).isEqualTo(parentExecutionId);

		if (exception != null) {
			assertThat(taskExecution.getErrorMessage()
					.length() > exception.getStackTrace().length).isTrue();
		}
		else {
			assertThat(taskExecution.getExitMessage()).isNull();
		}

		if (update) {
			assertThat(taskExecution.getEndTime().getTime() >= taskExecution
					.getStartTime().getTime()).isTrue();
			assertThat(taskExecution.getExitCode()).isNotNull();
		}
		else {
			assertThat(taskExecution.getEndTime()).isNull();
			assertThat(taskExecution.getExitCode() == null).isTrue();
		}

		assertThat(taskExecution.getTaskName()).isEqualTo("testTask");
	}

	@Configuration
	public static class ArgsConfiguration {

		@Bean
		public ApplicationArguments args() {
			Map<String, String> args = new HashMap<>(2);

			args.put("foo", "bar");
			args.put("baz", "qux");

			return new SimpleApplicationArgs(args);
		}

	}

	private static class SimpleApplicationArgs implements ApplicationArguments {

		private Map<String, String> args;

		SimpleApplicationArgs(Map<String, String> args) {
			this.args = args;
		}

		@Override
		public String[] getSourceArgs() {
			String[] sourceArgs = new String[this.args.size()];

			int i = 0;
			for (Map.Entry<String, String> stringStringEntry : this.args.entrySet()) {
				sourceArgs[i] = "--" + stringStringEntry.getKey() + "="
						+ stringStringEntry.getValue();
				i++;
			}

			return sourceArgs;
		}

		@Override
		public Set<String> getOptionNames() {
			return this.args.keySet();
		}

		@Override
		public boolean containsOption(String s) {
			return this.args.containsKey(s);
		}

		@Override
		public List<String> getOptionValues(String s) {
			return Collections.singletonList(this.args.get(s));
		}

		@Override
		public List<String> getNonOptionArgs() {
			throw new UnsupportedOperationException("Not supported at this time.");
		}

	}

	private static class TestListener2 extends TestListener {

	}

	private static class TestListener implements TaskExecutionListener {

		static List<Integer> startupOrderList = new ArrayList<>();
		static List<Integer> endOrderList = new ArrayList<>();
		static List<Integer> failOrderList = new ArrayList<>();

		private static int currentCount = 0;

		private int id = 0;

		TestListener() {
			currentCount++;
			this.id = currentCount;
		}

		public static List<Integer> getStartupOrderList() {
			return startupOrderList;
		}

		public static List<Integer> getEndOrderList() {
			return endOrderList;
		}

		public static List<Integer> getFailOrderList() {
			return failOrderList;
		}

		@Override
		public void onTaskStartup(TaskExecution taskExecution) {
			startupOrderList.add(this.id);
		}

		@Override
		public void onTaskEnd(TaskExecution taskExecution) {
			endOrderList.add(this.id);
		}

		@Override
		public void onTaskFailed(TaskExecution taskExecution, Throwable throwable) {
			failOrderList.add(this.id);
		}

	}

}
