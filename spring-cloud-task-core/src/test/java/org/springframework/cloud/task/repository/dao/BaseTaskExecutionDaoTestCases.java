/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.task.repository.dao;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.junit.Test;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Defines test cases that shall be shared between {@link JdbcTaskExecutionDaoTests} and {@link MapTaskExecutionDaoTests}.
 *
 * @author Gunnar Hillert
 */
public class BaseTaskExecutionDaoTestCases {

	protected TaskExecutionDao dao;

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionsByTaskNamesWithNullParameter()  {
		try {
			dao.getLatestTaskExecutionsByTaskNames(null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("At least 1 task name must be provided.", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionsByTaskNamesWithEmptyArrayParameter()  {
		try {
			dao.getLatestTaskExecutionsByTaskNames(new String[0]);
		}
		catch (IllegalArgumentException e) {
			assertEquals("At least 1 task name must be provided.", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionsByTaskNamesWithArrayParametersContainingNullAndEmptyValues()  {
		try {
			dao.getLatestTaskExecutionsByTaskNames("foo", null, "bar", " ");
		}
		catch (IllegalArgumentException e) {
			assertEquals("Task names must not contain any empty elements but 2 of 4 were empty or null.", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionsByTaskNamesWithSingleTaskName()  {
		initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		final List<TaskExecution> latestTaskExecutions = dao.getLatestTaskExecutionsByTaskNames("FOO1");
		assertTrue("Expected only 1 taskExecution but got " + latestTaskExecutions.size(), latestTaskExecutions.size() == 1);

		final TaskExecution lastTaskExecution = latestTaskExecutions.get(0);
		assertEquals("FOO1", lastTaskExecution.getTaskName());

		final Calendar dateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		dateTime.setTime(lastTaskExecution.getStartTime());

		assertEquals(2015, dateTime.get(Calendar.YEAR));
		assertEquals(2, dateTime.get(Calendar.MONTH) + 1);
		assertEquals(22, dateTime.get(Calendar.DAY_OF_MONTH));
		assertEquals(23, dateTime.get(Calendar.HOUR_OF_DAY));
		assertEquals(59, dateTime.get(Calendar.MINUTE));
		assertEquals(0, dateTime.get(Calendar.SECOND));
	}

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionsByTaskNamesWithMultipleTaskNames()  {
		initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		final List<TaskExecution> latestTaskExecutions = dao.getLatestTaskExecutionsByTaskNames("FOO1", "FOO3", "FOO4");
		assertTrue("Expected 3 taskExecutions but got " + latestTaskExecutions.size(), latestTaskExecutions.size() == 3);

		final Calendar dateTimeFoo3 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		dateTimeFoo3.setTime(latestTaskExecutions.get(0).getStartTime());

		assertEquals(2016, dateTimeFoo3.get(Calendar.YEAR));
		assertEquals(8, dateTimeFoo3.get(Calendar.MONTH) + 1);
		assertEquals(20, dateTimeFoo3.get(Calendar.DAY_OF_MONTH));
		assertEquals(14, dateTimeFoo3.get(Calendar.HOUR_OF_DAY));
		assertEquals(45, dateTimeFoo3.get(Calendar.MINUTE));
		assertEquals(0, dateTimeFoo3.get(Calendar.SECOND));

		final Calendar dateTimeFoo1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		dateTimeFoo1.setTime(latestTaskExecutions.get(1).getStartTime());

		assertEquals(2015, dateTimeFoo1.get(Calendar.YEAR));
		assertEquals(2, dateTimeFoo1.get(Calendar.MONTH) + 1);
		assertEquals(22, dateTimeFoo1.get(Calendar.DAY_OF_MONTH));
		assertEquals(23, dateTimeFoo1.get(Calendar.HOUR_OF_DAY));
		assertEquals(59, dateTimeFoo1.get(Calendar.MINUTE));
		assertEquals(0, dateTimeFoo1.get(Calendar.SECOND));

		final Calendar dateTimeFoo4 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		dateTimeFoo4.setTime(latestTaskExecutions.get(2).getStartTime());

		assertEquals(2015, dateTimeFoo4.get(Calendar.YEAR));
		assertEquals(2, dateTimeFoo4.get(Calendar.MONTH) + 1);
		assertEquals(20, dateTimeFoo4.get(Calendar.DAY_OF_MONTH));
		assertEquals(14, dateTimeFoo4.get(Calendar.HOUR_OF_DAY));
		assertEquals(45, dateTimeFoo4.get(Calendar.MINUTE));
		assertEquals(0, dateTimeFoo4.get(Calendar.SECOND));
	}

	/**
	 * This test is a special use-case. While not common, it is theoretically possible, that a task may have
	 * executed with the exact same start time multiple times. In that case we should still only get 1 returned
	 * {@link TaskExecution}.
	 */
	@Test
	@DirtiesContext
	public void getLatestTaskExecutionsByTaskNamesWithIdenticalTaskExecutions()  {
		long executionIdOffset = initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		final List<TaskExecution> latestTaskExecutions = dao.getLatestTaskExecutionsByTaskNames("FOO5");
		assertTrue("Expected only 1 taskExecution but got " + latestTaskExecutions.size(), latestTaskExecutions.size() == 1);

		final Calendar dateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		dateTime.setTime(latestTaskExecutions.get(0).getStartTime());

		assertEquals(2015, dateTime.get(Calendar.YEAR));
		assertEquals(2, dateTime.get(Calendar.MONTH) + 1);
		assertEquals(22, dateTime.get(Calendar.DAY_OF_MONTH));
		assertEquals(23, dateTime.get(Calendar.HOUR_OF_DAY));
		assertEquals(59, dateTime.get(Calendar.MINUTE));
		assertEquals(0, dateTime.get(Calendar.SECOND));
		assertEquals(9 + executionIdOffset, latestTaskExecutions.get(0).getExecutionId());
	}

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionForTaskNameWithNullParameter()  {
		try {
			dao.getLatestTaskExecutionForTaskName(null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("The task name must not be empty.", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionForTaskNameWithEmptyStringParameter()  {
		try {
			dao.getLatestTaskExecutionForTaskName("");
		}
		catch (IllegalArgumentException e) {
			assertEquals("The task name must not be empty.", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionForNonExistingTaskName()  {
		initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		final TaskExecution latestTaskExecution = dao.getLatestTaskExecutionForTaskName("Bar5");
		assertNull("Expected the latestTaskExecution to be null but got" + latestTaskExecution, latestTaskExecution);
	}

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionForExistingTaskName()  {
		initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		final TaskExecution latestTaskExecution = dao.getLatestTaskExecutionForTaskName("FOO1");
		assertNotNull("Expected the latestTaskExecution not to be null", latestTaskExecution);

		final Calendar dateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		dateTime.setTime(latestTaskExecution.getStartTime());

		assertEquals(2015, dateTime.get(Calendar.YEAR));
		assertEquals(2, dateTime.get(Calendar.MONTH) + 1);
		assertEquals(22, dateTime.get(Calendar.DAY_OF_MONTH));
		assertEquals(23, dateTime.get(Calendar.HOUR_OF_DAY));
		assertEquals(59, dateTime.get(Calendar.MINUTE));
		assertEquals(0, dateTime.get(Calendar.SECOND));
	}

	/**
	 * This test is a special use-case. While not common, it is theoretically possible, that a task may have
	 * executed with the exact same start time multiple times. In that case we should still only get 1 returned
	 * {@link TaskExecution}.
	 */
	@Test
	@DirtiesContext
	public void getLatestTaskExecutionForTaskNameWithIdenticalTaskExecutions()  {
		long executionIdOffset = initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		final TaskExecution latestTaskExecution = dao.getLatestTaskExecutionForTaskName("FOO5");
		assertNotNull("Expected the latestTaskExecution not to be null", latestTaskExecution);

		final Calendar dateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		dateTime.setTime(latestTaskExecution.getStartTime());

		assertEquals(2015, dateTime.get(Calendar.YEAR));
		assertEquals(2, dateTime.get(Calendar.MONTH) + 1);
		assertEquals(22, dateTime.get(Calendar.DAY_OF_MONTH));
		assertEquals(23, dateTime.get(Calendar.HOUR_OF_DAY));
		assertEquals(59, dateTime.get(Calendar.MINUTE));
		assertEquals(0, dateTime.get(Calendar.SECOND));
		assertEquals(9 + executionIdOffset, latestTaskExecution.getExecutionId());
	}

	protected long initializeRepositoryNotInOrderWithMultipleTaskExecutions() {

		final TaskExecution foo1_0 = getTaskExecution("FOO1", "externalC");
		foo1_0.setStartTime(getDate(2015, 2, 22, 23, 59));

		final TaskExecution foo1_1 = getTaskExecution("FOO1", "externalC");
		foo1_1.setStartTime(getDate(2015, 2, 20, 14, 45));

		final TaskExecution foo1_2 = getTaskExecution("FOO1", "externalC");
		foo1_2.setStartTime(getDate(2015, 1, 19, 14, 30));

		final TaskExecution foo1_3 = getTaskExecution("FOO1", "externalC");
		foo1_3.setStartTime(getDate(2015, 1, 20, 14, 45));

		TaskExecution foo2 = getTaskExecution("FOO2", "externalA");
		foo2.setStartTime(getDate(2015, 4, 20, 14, 45));

		TaskExecution foo3 = getTaskExecution("FOO3", "externalB");
		foo3.setStartTime(getDate(2016, 8, 20, 14, 45));

		TaskExecution foo4 = getTaskExecution("FOO4", "externalB");
		foo4.setStartTime(getDate(2015, 2, 20, 14, 45));

		final TaskExecution foo5_0 = getTaskExecution("FOO5", "externalC");
		foo5_0.setStartTime(getDate(2015, 2, 22, 23, 59));

		final TaskExecution foo5_1 = getTaskExecution("FOO5", "externalC");
		foo5_1.setStartTime(getDate(2015, 2, 22, 23, 59));

		final TaskExecution foo5_2 = getTaskExecution("FOO5", "externalC");
		foo5_2.setStartTime(getDate(2015, 2, 22, 23, 59));

		long executionIdOffset = this.createTaskExecution(foo1_0);
		this.createTaskExecution(foo1_1);
		this.createTaskExecution(foo1_2);
		this.createTaskExecution(foo1_3);

		this.createTaskExecution(foo2);
		this.createTaskExecution(foo3);
		this.createTaskExecution(foo4);

		this.createTaskExecution(foo5_0);
		this.createTaskExecution(foo5_1);
		this.createTaskExecution(foo5_2);

		return executionIdOffset;
	}

	private Date getDate(int year, int month, int day, int hour, int minute) {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.clear();
		calendar.set(year, month - 1, day, hour, minute);
		return calendar.getTime();
	}

	private long createTaskExecution(TaskExecution te) {
		return dao.createTaskExecution(te.getTaskName(), te.getStartTime(), te.getArguments(), te.getExternalExecutionId()).getExecutionId();
	}

	protected TaskExecution getTaskExecution(String taskName,
			String externalExecutionId) {
		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName(taskName);
		taskExecution.setExternalExecutionId(externalExecutionId);
		taskExecution.setStartTime(new Date());
		return taskExecution;
	}
}
