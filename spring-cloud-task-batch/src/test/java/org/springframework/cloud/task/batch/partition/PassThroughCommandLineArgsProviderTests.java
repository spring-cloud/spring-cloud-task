/*
 * Copyright 2016 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Michael Minella
 */
public class PassThroughCommandLineArgsProviderTests {

	@Test(expected = IllegalArgumentException.class)
	public void testNull() {
		new PassThroughCommandLineArgsProvider(null);
	}

	@Test
	public void test() {
		List<String> args = Arrays.asList("foo", "bar", "baz");

		CommandLineArgsProvider provider = new PassThroughCommandLineArgsProvider(args);

		List<String> commandLineArgs = provider.getCommandLineArgs(null);

		assertEquals("foo", commandLineArgs.get(0));
		assertEquals("bar", commandLineArgs.get(1));
		assertEquals("baz", commandLineArgs.get(2));
	}
}
