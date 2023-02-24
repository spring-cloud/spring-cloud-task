/*
 * Copyright 2018-2019 the original author or authors.
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

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Defines test cases that shall be between {@link TaskExecutionDao} tests.
 *
 * @author Gunnar Hillert
 */
public abstract class BaseTaskExecutionDaoTestCases {

	protected TaskExecutionDao dao;

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionsByTaskNamesWithNullParameter() {
		try {
			this.dao.getLatestTaskExecutionsByTaskNames(null);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("At least 1 task name must be provided.");
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionsByTaskNamesWithEmptyArrayParameter() {
		try {
			this.dao.getLatestTaskExecutionsByTaskNames(new String[0]);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("At least 1 task name must be provided.");
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionsByTaskNamesWithArrayParametersContainingNullAndEmptyValues() {
		try {
			this.dao.getLatestTaskExecutionsByTaskNames("foo", null, "bar", " ");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage())
					.isEqualTo("Task names must not contain any empty elements but 2 of 4 were empty or null.");
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionsByTaskNamesWithSingleTaskName() {
		initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		final List<TaskExecution> latestTaskExecutions = this.dao.getLatestTaskExecutionsByTaskNames("FOO1");
		assertThat(latestTaskExecutions.size() == 1)
				.as("Expected only 1 taskExecution but got " + latestTaskExecutions.size()).isTrue();

		final TaskExecution lastTaskExecution = latestTaskExecutions.get(0);
		assertThat(lastTaskExecution.getTaskName()).isEqualTo("FOO1");

		assertThat(lastTaskExecution.getStartTime().getYear()).isEqualTo(2015);
		assertThat(lastTaskExecution.getStartTime().getMonthValue()).isEqualTo(2);
		assertThat(lastTaskExecution.getStartTime().getDayOfMonth()).isEqualTo(22);
		assertThat(lastTaskExecution.getStartTime().getHour()).isEqualTo(23);
		assertThat(lastTaskExecution.getStartTime().getMinute()).isEqualTo(59);
		assertThat(lastTaskExecution.getStartTime().getSecond()).isEqualTo(0);
	}

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionsByTaskNamesWithMultipleTaskNames() {
		initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		final List<TaskExecution> latestTaskExecutions = this.dao.getLatestTaskExecutionsByTaskNames("FOO1", "FOO3",
				"FOO4");
		assertThat(latestTaskExecutions.size() == 3)
				.as("Expected 3 taskExecutions but got " + latestTaskExecutions.size()).isTrue();

		LocalDateTime startDateTime = latestTaskExecutions.get(0).getStartTime();
		assertThat(startDateTime.getYear()).isEqualTo(2016);
		assertThat(startDateTime.getMonthValue()).isEqualTo(8);
		assertThat(startDateTime.getDayOfMonth()).isEqualTo(20);
		assertThat(startDateTime.getHour()).isEqualTo(14);
		assertThat(startDateTime.getMinute()).isEqualTo(45);
		assertThat(startDateTime.getSecond()).isEqualTo(0);

		LocalDateTime startDateTimeOne = latestTaskExecutions.get(1).getStartTime();

		assertThat(startDateTimeOne.getYear()).isEqualTo(2015);
		assertThat(startDateTimeOne.getMonthValue()).isEqualTo(2);
		assertThat(startDateTimeOne.getDayOfMonth()).isEqualTo(22);
		assertThat(startDateTimeOne.getHour()).isEqualTo(23);
		assertThat(startDateTimeOne.getMinute()).isEqualTo(59);
		assertThat(startDateTimeOne.getSecond()).isEqualTo(0);

		LocalDateTime startDateTimeTwo = latestTaskExecutions.get(2).getStartTime();

		assertThat(startDateTimeTwo.getYear()).isEqualTo(2015);
		assertThat(startDateTimeTwo.getMonthValue()).isEqualTo(2);
		assertThat(startDateTimeTwo.getDayOfMonth()).isEqualTo(20);
		assertThat(startDateTimeTwo.getHour()).isEqualTo(14);
		assertThat(startDateTimeTwo.getMinute()).isEqualTo(45);
		assertThat(startDateTimeTwo.getSecond()).isEqualTo(0);
	}

	/**
	 * This test is a special use-case. While not common, it is theoretically possible,
	 * that a task may have executed with the exact same start time multiple times. In
	 * that case we should still only get 1 returned {@link TaskExecution}.
	 */
	@Test
	@DirtiesContext
	public void getLatestTaskExecutionsByTaskNamesWithIdenticalTaskExecutions() {
		long executionIdOffset = initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		final List<TaskExecution> latestTaskExecutions = this.dao.getLatestTaskExecutionsByTaskNames("FOO5");
		assertThat(latestTaskExecutions.size() == 1)
				.as("Expected only 1 taskExecution but got " + latestTaskExecutions.size()).isTrue();

		LocalDateTime startDateTime = latestTaskExecutions.get(0).getStartTime();

		assertThat(startDateTime.getYear()).isEqualTo(2015);
		assertThat(startDateTime.getMonthValue()).isEqualTo(2);
		assertThat(startDateTime.getDayOfMonth()).isEqualTo(22);
		assertThat(startDateTime.getHour()).isEqualTo(23);
		assertThat(startDateTime.getMinute()).isEqualTo(59);
		assertThat(startDateTime.getSecond()).isEqualTo(0);
		assertThat(latestTaskExecutions.get(0).getExecutionId()).isEqualTo(9 + executionIdOffset);
	}

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionForTaskNameWithNullParameter() {
		try {
			this.dao.getLatestTaskExecutionForTaskName(null);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("The task name must not be empty.");
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionForTaskNameWithEmptyStringParameter() {
		try {
			this.dao.getLatestTaskExecutionForTaskName("");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("The task name must not be empty.");
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionForNonExistingTaskName() {
		initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		final TaskExecution latestTaskExecution = this.dao.getLatestTaskExecutionForTaskName("Bar5");
		assertThat(latestTaskExecution).as("Expected the latestTaskExecution to be null but got" + latestTaskExecution)
				.isNull();
	}

	@Test
	@DirtiesContext
	public void getLatestTaskExecutionForExistingTaskName() {
		initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		final TaskExecution latestTaskExecution = this.dao.getLatestTaskExecutionForTaskName("FOO1");
		assertThat(latestTaskExecution).as("Expected the latestTaskExecution not to be null").isNotNull();

		LocalDateTime startDateTime = latestTaskExecution.getStartTime();

		assertThat(startDateTime.getYear()).isEqualTo(2015);
		assertThat(startDateTime.getMonthValue()).isEqualTo(2);
		assertThat(startDateTime.getDayOfMonth()).isEqualTo(22);
		assertThat(startDateTime.getHour()).isEqualTo(23);
		assertThat(startDateTime.getMinute()).isEqualTo(59);
		assertThat(startDateTime.getSecond()).isEqualTo(0);
	}

	/**
	 * This test is a special use-case. While not common, it is theoretically possible,
	 * that a task may have executed with the exact same start time multiple times. In
	 * that case we should still only get 1 returned {@link TaskExecution}.
	 */
	@Test
	@DirtiesContext
	public void getLatestTaskExecutionForTaskNameWithIdenticalTaskExecutions() {
		long executionIdOffset = initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		final TaskExecution latestTaskExecution = this.dao.getLatestTaskExecutionForTaskName("FOO5");
		assertThat(latestTaskExecution).as("Expected the latestTaskExecution not to be null").isNotNull();

		LocalDateTime startDateTime = latestTaskExecution.getStartTime();

		assertThat(startDateTime.getYear()).isEqualTo(2015);
		assertThat(startDateTime.getMonthValue()).isEqualTo(2);
		assertThat(startDateTime.getDayOfMonth()).isEqualTo(22);
		assertThat(startDateTime.getHour()).isEqualTo(23);
		assertThat(startDateTime.getMinute()).isEqualTo(59);
		assertThat(startDateTime.getSecond()).isEqualTo(0);
		assertThat(latestTaskExecution.getExecutionId()).isEqualTo(9 + executionIdOffset);
	}

	@Test
	@DirtiesContext
	public void getRunningTaskExecutions() {
		initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		assertThat(this.dao.getRunningTaskExecutionCount()).isEqualTo(this.dao.getTaskExecutionCount());
		this.dao.completeTaskExecution(1, 0, LocalDateTime.now(), "c'est fini!");
		assertThat(this.dao.getRunningTaskExecutionCount()).isEqualTo(this.dao.getTaskExecutionCount() - 1);
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

	private LocalDateTime getDate(int year, int month, int day, int hour, int minute) {
		return LocalDateTime.now().withYear(year).withMonth(month).withDayOfMonth(day).withHour(hour).withMinute(minute)
				.withSecond(0);
	}

	private long createTaskExecution(TaskExecution te) {
		return this.dao.createTaskExecution(te.getTaskName(), te.getStartTime(), te.getArguments(),
				te.getExternalExecutionId()).getExecutionId();
	}

	protected TaskExecution getTaskExecution(String taskName, String externalExecutionId) {
		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName(taskName);
		taskExecution.setExternalExecutionId(externalExecutionId);
		taskExecution.setStartTime(LocalDateTime.now());
		return taskExecution;
	}

}
