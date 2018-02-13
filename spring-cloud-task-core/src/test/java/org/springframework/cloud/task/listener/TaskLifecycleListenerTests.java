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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the TaskLifecycleListener Methods record the appropriate log header entries and
 * result codes.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 */
public class TaskLifecycleListenerTests {

	private AnnotationConfigApplicationContext context;

	private TaskExplorer taskExplorer;

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Before
	public void setUp() {
		context = new AnnotationConfigApplicationContext();
		context.setId("testTask");
		context.register(TestDefaultConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
		TestListener.getStartupOrderList().clear();
		TestListener.getFailOrderList().clear();
		TestListener.getEndOrderList().clear();

	}

	@After
	public void tearDown() {
		if(context != null && context.isActive()) {
			context.close();
		}
	}

	@Test
	public void testTaskCreate() {
		context.refresh();
		this.taskExplorer = context.getBean(TaskExplorer.class);
		verifyTaskExecution(0, false);
	}

	@Test
	public void testTaskCreateWithArgs() {
		context.register(ArgsConfiguration.class);
		context.refresh();
		this.taskExplorer = context.getBean(TaskExplorer.class);
		verifyTaskExecution(2, false);
	}

	@Test
	public void testTaskUpdate() {
		context.refresh();
		this.taskExplorer = context.getBean(TaskExplorer.class);

		context.publishEvent(new ApplicationReadyEvent(new SpringApplication(), new String[0], context));

		verifyTaskExecution(0, true);
	}

	@Test
	public void testTaskFailedUpdate() {
		context.refresh();
		RuntimeException exception = new RuntimeException("This was expected");
		SpringApplication application = new SpringApplication();
		this.taskExplorer = context.getBean(TaskExplorer.class);
		context.publishEvent(new ApplicationFailedEvent(application, new String[0], context, exception));
		context.publishEvent(new ApplicationReadyEvent(application, new String[0], context));

		verifyTaskExecution(0, true, 1, exception, null);
	}

	@Test
	public void testTaskFailedWithExitCodeEvent() {
		final int exitCode = 10;
		context.register(TestListener.class);
		context.register(TestListener2.class);

		context.refresh();
		RuntimeException exception = new RuntimeException("This was expected");
		SpringApplication application = new SpringApplication();
		this.taskExplorer = context.getBean(TaskExplorer.class);
		context.publishEvent(new ExitCodeEvent(context, exitCode));
		context.publishEvent(new ApplicationFailedEvent(application, new String[0], context, exception));
		context.publishEvent(new ApplicationReadyEvent(application, new String[0], context));

		verifyTaskExecution(0, true, exitCode, exception, null);
		assertEquals(2, TestListener.getStartupOrderList().size());
		assertEquals(Integer.valueOf(2), TestListener.getStartupOrderList().get(0));
		assertEquals(Integer.valueOf(1), TestListener.getStartupOrderList().get(1));

		assertEquals(2, TestListener.getEndOrderList().size());
		assertEquals(Integer.valueOf(1), TestListener.getEndOrderList().get(0));
		assertEquals(Integer.valueOf(2), TestListener.getEndOrderList().get(1));

		assertEquals(2, TestListener.getFailOrderList().size());
		assertEquals(Integer.valueOf(1), TestListener.getFailOrderList().get(0));
		assertEquals(Integer.valueOf(2), TestListener.getFailOrderList().get(1));

	}

	@Test
	public void testNoClosingOfContext() {

		try (ConfigurableApplicationContext applicationContext = SpringApplication.run(new Class[] {TestDefaultConfiguration.class, PropertyPlaceholderAutoConfiguration.class},
				new String[] {"--spring.cloud.task.closecontext_enabled=false"})) {
			assertTrue(applicationContext.isActive());
		}
	}

	@Test(expected = ApplicationContextException.class)
	public void testInvalidTaskExecutionId() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		Map<String, Object> myMap = new HashMap<>();
		myMap.put("spring.cloud.task.executionid", "55");
		propertySources.addFirst(new MapPropertySource("EnvrionmentTestPropsource", myMap));
		context.setEnvironment(environment);
		context.refresh();
	}

	@Test
	public void testRestartExistingTask() {
		context.refresh();
		TaskLifecycleListener taskLifecycleListener =
				context.getBean(TaskLifecycleListener.class);
		taskLifecycleListener.start();
		String output = this.outputCapture.toString();
		assertTrue("Test results do not show error message: " + output,
				output.contains("Multiple start events have been received"));
	}

	@Test
	public void testExternalExecutionId() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		Map<String, Object> myMap = new HashMap<>();
		myMap.put("spring.cloud.task.external-execution-id", "myid");
		propertySources.addFirst(new MapPropertySource("EnvrionmentTestPropsource", myMap));
		context.setEnvironment(environment);
		context.refresh();
		this.taskExplorer = context.getBean(TaskExplorer.class);

		verifyTaskExecution(0, false, 0, null, "myid");
	}

	@Test
	public void testParentExecutionId() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		Map<String, Object> myMap = new HashMap<>();
		myMap.put("spring.cloud.task.parentExecutionId", 789);
		propertySources.addFirst(new MapPropertySource("EnvrionmentTestPropsource", myMap));
		context.setEnvironment(environment);
		context.refresh();
		this.taskExplorer = context.getBean(TaskExplorer.class);

		verifyTaskExecution(0, false, 0, null, null, 789L);
	}

	private void verifyTaskExecution(int numberOfParams, boolean update) {
		verifyTaskExecution(numberOfParams, update, 0, null, null);
	}
	private void verifyTaskExecution(int numberOfParams, boolean update,
			Integer exitCode, Throwable exception, String externalExecutionId) {
		verifyTaskExecution(numberOfParams, update, exitCode, exception,
				externalExecutionId, null);
	}

	private void verifyTaskExecution(int numberOfParams, boolean update,
			Integer exitCode, Throwable exception, String externalExecutionId,
			Long parentExecutionId) {

		Sort sort = Sort.by("id");

		PageRequest request = PageRequest.of(0, Integer.MAX_VALUE, sort);

		Page<TaskExecution> taskExecutionsByName = this.taskExplorer.findTaskExecutionsByName("testTask",
				request);
		assertTrue(taskExecutionsByName.iterator().hasNext());
		TaskExecution taskExecution = taskExecutionsByName.iterator().next();

		assertEquals(numberOfParams, taskExecution.getArguments().size());
		assertEquals(exitCode, taskExecution.getExitCode());
		assertEquals(externalExecutionId, taskExecution.getExternalExecutionId());
		assertEquals(parentExecutionId, taskExecution.getParentExecutionId());

		if(exception != null) {
			assertTrue(taskExecution.getErrorMessage().length() > exception.getStackTrace().length);
		}
		else {
			assertNull(taskExecution.getExitMessage());
		}

		if(update) {
			assertTrue(taskExecution.getEndTime().getTime() >= taskExecution.getStartTime().getTime());
			assertNotNull(taskExecution.getExitCode());
		}
		else {
			assertNull(taskExecution.getEndTime());
			assertTrue(taskExecution.getExitCode() == 0);
		}

		assertEquals("testTask", taskExecution.getTaskName());
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

		public SimpleApplicationArgs(Map<String, String> args) {
			this.args = args;
		}

		@Override
		public String[] getSourceArgs() {
			String [] sourceArgs = new String[this.args.size()];

			int i = 0;
			for (Map.Entry<String, String> stringStringEntry : args.entrySet()) {
				sourceArgs[i] = "--" + stringStringEntry.getKey() + "=" + stringStringEntry.getValue();
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

		private static int currentCount = 0;

		private int id = 0;

		static List<Integer> startupOrderList = new ArrayList<>();

		static List<Integer> endOrderList = new ArrayList<>();

		static List<Integer> failOrderList = new ArrayList<>();

		public TestListener() {
			currentCount++;
			id = currentCount;
		}

		@Override
		public void onTaskStartup(TaskExecution taskExecution) {
			startupOrderList.add(id);
		}

		@Override
		public void onTaskEnd(TaskExecution taskExecution) {
			endOrderList.add(id);
		}

		@Override
		public void onTaskFailed(TaskExecution taskExecution, Throwable throwable) {
			failOrderList.add(id);
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
	}
}
