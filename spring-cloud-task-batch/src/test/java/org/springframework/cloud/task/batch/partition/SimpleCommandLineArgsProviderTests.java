/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.cloud.task.batch.partition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.springframework.cloud.task.repository.TaskExecution;

import static org.junit.Assert.assertEquals;

/**
 * @author Michael Minella
 */
public class SimpleCommandLineArgsProviderTests {

	@Test
	public void test() {
		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setArguments(Arrays.asList("foo", "bar", "baz"));

		SimpleCommandLineArgsProvider provider = new SimpleCommandLineArgsProvider(taskExecution);

		List<String> commandLineArgs = provider.getCommandLineArgs(null);

		assertEquals("foo", commandLineArgs.get(0));
		assertEquals("bar", commandLineArgs.get(1));
		assertEquals("baz", commandLineArgs.get(2));
	}

	@Test
	public void testAppending() {
		List<String> appendedValues = new ArrayList<>(3);
		appendedValues.add("one");
		appendedValues.add("two");
		appendedValues.add("three");

		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setArguments(Arrays.asList("foo", "bar", "baz"));

		SimpleCommandLineArgsProvider provider = new SimpleCommandLineArgsProvider(taskExecution);
		provider.setAppendedArgs(appendedValues);

		List<String> commandLineArgs = provider.getCommandLineArgs(null);

		assertEquals("foo", commandLineArgs.get(0));
		assertEquals("bar", commandLineArgs.get(1));
		assertEquals("baz", commandLineArgs.get(2));
		assertEquals("one", commandLineArgs.get(3));
		assertEquals("two", commandLineArgs.get(4));
		assertEquals("three", commandLineArgs.get(5));
	}

	@Test
	public void testAppendingNull() {

		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setArguments(Arrays.asList("foo", "bar", "baz"));

		SimpleCommandLineArgsProvider provider = new SimpleCommandLineArgsProvider(taskExecution);
		provider.setAppendedArgs(null);

		List<String> commandLineArgs = provider.getCommandLineArgs(null);

		assertEquals(3, commandLineArgs.size());
		assertEquals("foo", commandLineArgs.get(0));
		assertEquals("bar", commandLineArgs.get(1));
		assertEquals("baz", commandLineArgs.get(2));
	}
}
