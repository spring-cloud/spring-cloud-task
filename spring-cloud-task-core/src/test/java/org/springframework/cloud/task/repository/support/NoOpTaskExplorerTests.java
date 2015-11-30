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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.task.repository.TaskExplorer;

public class NoOpTaskExplorerTests {

	private TaskExplorer taskExplorer;

	@Before
	public void setUp() throws Exception {
		taskExplorer = new NoOpTaskExplorer();
	}

	@Test
	public void testGetTaskExecution() throws Exception {
		assertNull(taskExplorer.getTaskExecution(3l));
	}

	@Test
	public void testFindRunningTaskExecutions() throws Exception {
		assertEquals(taskExplorer.findRunningTaskExecutions("foo").size(), 0);
	}

	@Test
	public void testGetTaskNames() throws Exception {
		assertEquals(taskExplorer.getTaskNames().size(), 0);
	}

	@Test
	public void testGetTaskExecutionCount() throws Exception {
		assertEquals(taskExplorer.getTaskExecutionCount("foo"), 0);
	}

	@Test
	public void testGetTaskExecutionsByName() throws Exception {
		assertEquals(taskExplorer.getTaskExecutionsByName("foo", 0, 100).size(), 0);
	}
}
