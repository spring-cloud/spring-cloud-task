/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.cloud.task.batch.partition;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.springframework.cloud.task.repository.TaskExecution;

import static org.junit.Assert.assertEquals;

/**
 * @author Michael Minella
 */
public class SimpleCommandLineArgsProviderTests {

	@Test(expected = IllegalArgumentException.class)
	public void testNullConstructorArg() {
		new SimpleCommandLineArgsProvider(null);
	}

	@Test
	public void test() {
		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setArguments(Arrays.asList("foo", "bar", "baz"));

		CommandLineArgsProvider provider = new SimpleCommandLineArgsProvider(taskExecution);

		List<String> commandLineArgs = provider.getCommandLineArgs(null);

		assertEquals("foo", commandLineArgs.get(0));
		assertEquals("bar", commandLineArgs.get(1));
		assertEquals("baz", commandLineArgs.get(2));
	}
}
