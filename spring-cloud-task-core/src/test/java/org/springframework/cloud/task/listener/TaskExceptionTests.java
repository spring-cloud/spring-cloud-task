/*
 *  Copyright 2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.listener;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Glenn Renfro
 */
public class TaskExceptionTests {

	private static final String ERROR_MESSAGE = "ERROR MESSAGE";

	@Test
	public void testTaskException() {
		TaskException taskException = new TaskException(ERROR_MESSAGE);
		assertEquals(ERROR_MESSAGE, taskException.getMessage());

		taskException = new TaskException(ERROR_MESSAGE,
				new IllegalStateException(ERROR_MESSAGE));
		assertEquals(ERROR_MESSAGE, taskException.getMessage());
		assertNotNull(taskException.getCause());
		assertEquals(ERROR_MESSAGE, taskException.getCause().getMessage());
	}

	@Test
	public void testTaskExecutionException() {
		TaskExecutionException taskException = new TaskExecutionException(ERROR_MESSAGE);
		assertEquals(ERROR_MESSAGE, taskException.getMessage());

		taskException = new TaskExecutionException(ERROR_MESSAGE,
				new IllegalStateException(ERROR_MESSAGE));
		assertEquals(ERROR_MESSAGE, taskException.getMessage());
		assertNotNull(taskException.getCause());
		assertEquals(ERROR_MESSAGE, taskException.getCause().getMessage());
	}
}
