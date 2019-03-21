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

package org.springframework.cloud.task.repository.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Stores Task Execution Information to a in-memory map.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author David Turanski
 */
public class MapTaskExecutionDao implements TaskExecutionDao {

	private final AtomicLong currentId = new AtomicLong(0L);

	private ConcurrentMap<Long, TaskExecution> taskExecutions;

	private ConcurrentMap<Long, Set<Long>> batchJobAssociations;

	public MapTaskExecutionDao() {
		this.taskExecutions = new ConcurrentHashMap<>();
		this.batchJobAssociations = new ConcurrentHashMap<>();
	}

	@Override
	public TaskExecution createTaskExecution(String taskName, Date startTime,
			List<String> arguments, String externalExecutionId) {
		return createTaskExecution(taskName, startTime, arguments, externalExecutionId,
				null);
	}

	@Override
	public TaskExecution createTaskExecution(String taskName, Date startTime,
			List<String> arguments, String externalExecutionId, Long parentExecutionId) {
		long taskExecutionId = getNextExecutionId();
		TaskExecution taskExecution = new TaskExecution(taskExecutionId, null, taskName,
				startTime, null, null, arguments, null, externalExecutionId,
				parentExecutionId);
		this.taskExecutions.put(taskExecutionId, taskExecution);
		return taskExecution;
	}

	@Override
	public TaskExecution startTaskExecution(long executionId, String taskName,
			Date startTime, List<String> arguments, String externalExecutionid) {
		return startTaskExecution(executionId, taskName, startTime, arguments,
				externalExecutionid, null);
	}

	@Override
	public TaskExecution startTaskExecution(long executionId, String taskName,
			Date startTime, List<String> arguments, String externalExecutionid,
			Long parentExecutionId) {
		TaskExecution taskExecution = this.taskExecutions.get(executionId);
		taskExecution.setTaskName(taskName);
		taskExecution.setStartTime(startTime);
		taskExecution.setArguments(arguments);
		taskExecution.setParentExecutionId(parentExecutionId);
		if (externalExecutionid != null) {
			taskExecution.setExternalExecutionId(externalExecutionid);
		}
		return taskExecution;
	}

	@Override
	public void completeTaskExecution(long executionId, Integer exitCode, Date endTime,
			String exitMessage, String errorMessage) {
		if (!this.taskExecutions.containsKey(executionId)) {
			throw new IllegalStateException(
					"Invalid TaskExecution, ID " + executionId + " not found.");
		}

		TaskExecution taskExecution = this.taskExecutions.get(executionId);
		taskExecution.setEndTime(endTime);
		taskExecution.setExitCode(exitCode);
		taskExecution.setExitMessage(exitMessage);
		taskExecution.setErrorMessage(errorMessage);
	}

	@Override
	public void completeTaskExecution(long executionId, Integer exitCode, Date endTime,
			String exitMessage) {
		completeTaskExecution(executionId, exitCode, endTime, exitMessage, null);
	}

	@Override
	public TaskExecution getTaskExecution(long executionId) {
		return this.taskExecutions.get(executionId);
	}

	@Override
	public long getTaskExecutionCountByTaskName(String taskName) {
		int count = 0;
		for (Map.Entry<Long, TaskExecution> entry : this.taskExecutions.entrySet()) {
			if (entry.getValue().getTaskName().equals(taskName)) {
				count++;
			}
		}
		return count;
	}

	@Override
	public long getRunningTaskExecutionCountByTaskName(String taskName) {
		int count = 0;
		for (Map.Entry<Long, TaskExecution> entry : this.taskExecutions.entrySet()) {
			if (entry.getValue().getTaskName().equals(taskName)
					&& entry.getValue().getEndTime() == null) {
				count++;
			}
		}
		return count;
	}

	@Override
	public long getRunningTaskExecutionCount() {
		long count = 0;
		for (Map.Entry<Long, TaskExecution> entry : this.taskExecutions.entrySet()) {
			if (entry.getValue().getEndTime() == null) {
				count++;
			}
		}
		return count;
	}

	@Override
	public long getTaskExecutionCount() {
		return this.taskExecutions.size();
	}

	@Override
	public Page<TaskExecution> findRunningTaskExecutions(String taskName,
			Pageable pageable) {
		Set<TaskExecution> result = getTaskExecutionTreeSet();
		for (Map.Entry<Long, TaskExecution> entry : this.taskExecutions.entrySet()) {
			if (entry.getValue().getTaskName().equals(taskName)
					&& entry.getValue().getEndTime() == null) {
				result.add(entry.getValue());
			}
		}
		return getPageFromList(new ArrayList<>(result), pageable,
				getRunningTaskExecutionCountByTaskName(taskName));
	}

	@Override
	public Page<TaskExecution> findTaskExecutionsByName(String taskName,
			Pageable pageable) {
		Set<TaskExecution> filteredSet = getTaskExecutionTreeSet();
		for (Map.Entry<Long, TaskExecution> entry : this.taskExecutions.entrySet()) {
			if (entry.getValue().getTaskName().equals(taskName)) {
				filteredSet.add(entry.getValue());
			}
		}
		return getPageFromList(new ArrayList<>(filteredSet), pageable,
				getTaskExecutionCountByTaskName(taskName));
	}

	@Override
	public List<String> getTaskNames() {
		Set<String> result = new TreeSet<>();
		for (Map.Entry<Long, TaskExecution> entry : this.taskExecutions.entrySet()) {
			result.add(entry.getValue().getTaskName());
		}
		return new ArrayList<>(result);
	}

	@Override
	public Page<TaskExecution> findAll(Pageable pageable) {
		TreeSet<TaskExecution> sortedSet = getTaskExecutionTreeSet();
		sortedSet.addAll(this.taskExecutions.values());
		List<TaskExecution> result = new ArrayList<>(sortedSet.descendingSet());
		return getPageFromList(result, pageable, getTaskExecutionCount());
	}

	public Map<Long, TaskExecution> getTaskExecutions() {
		return Collections.unmodifiableMap(this.taskExecutions);
	}

	public long getNextExecutionId() {
		return this.currentId.getAndIncrement();
	}

	@Override
	public Long getTaskExecutionIdByJobExecutionId(long jobExecutionId) {
		Long taskId = null;

		found:

		for (Map.Entry<Long, Set<Long>> association : this.batchJobAssociations
				.entrySet()) {
			for (Long curJobExecutionId : association.getValue()) {
				if (curJobExecutionId.equals(jobExecutionId)) {
					taskId = association.getKey();
					break found;
				}
			}
		}

		return taskId;
	}

	@Override
	public Set<Long> getJobExecutionIdsByTaskExecutionId(long taskExecutionId) {
		if (this.batchJobAssociations.containsKey(taskExecutionId)) {
			return Collections
					.unmodifiableSet(this.batchJobAssociations.get(taskExecutionId));
		}
		else {
			return new TreeSet<>();
		}
	}

	@Override
	public void updateExternalExecutionId(long taskExecutionId,
			String externalExecutionId) {
		TaskExecution taskExecution = this.taskExecutions.get(taskExecutionId);
		Assert.notNull(taskExecution,
				"Invalid TaskExecution, ID " + taskExecutionId + " not found.");
		taskExecution.setExternalExecutionId(externalExecutionId);
	}

	public ConcurrentMap<Long, Set<Long>> getBatchJobAssociations() {
		return this.batchJobAssociations;
	}

	private TreeSet<TaskExecution> getTaskExecutionTreeSet() {
		return new TreeSet<>(new Comparator<TaskExecution>() {
			@Override
			public int compare(TaskExecution e1, TaskExecution e2) {
				int result = e1.getStartTime().compareTo(e2.getStartTime());
				if (result == 0) {
					result = Long.valueOf(e1.getExecutionId())
							.compareTo(e2.getExecutionId());
				}
				return result;
			}
		});
	}

	private Page getPageFromList(List<TaskExecution> executionList, Pageable pageable,
			long maxSize) {
		long toIndex = (pageable.getOffset() + pageable.getPageSize() > executionList
				.size()) ? executionList.size()
						: pageable.getOffset() + pageable.getPageSize();
		return new PageImpl<>(
				executionList.subList((int) pageable.getOffset(), (int) toIndex),
				pageable, maxSize);
	}

	@Override
	public List<TaskExecution> getLatestTaskExecutionsByTaskNames(String... taskNames) {

		Assert.notEmpty(taskNames, "At least 1 task name must be provided.");

		final List<String> taskNamesAsList = new ArrayList<>();

		for (String taskName : taskNames) {
			if (StringUtils.hasText(taskName)) {
				taskNamesAsList.add(taskName);
			}
		}

		Assert.isTrue(taskNamesAsList.size() == taskNames.length, String.format(
				"Task names must not contain any empty elements but %s of %s were empty or null.",
				taskNames.length - taskNamesAsList.size(), taskNames.length));

		final Map<String, TaskExecution> tempTaskExecutions = new HashMap<>();

		for (Map.Entry<Long, TaskExecution> taskExecutionMapEntry : this.taskExecutions
				.entrySet()) {
			if (!taskNamesAsList
					.contains(taskExecutionMapEntry.getValue().getTaskName())) {
				continue;
			}

			final TaskExecution tempTaskExecution = tempTaskExecutions
					.get(taskExecutionMapEntry.getValue().getTaskName());
			if (tempTaskExecution == null
					|| tempTaskExecution.getStartTime()
							.before(taskExecutionMapEntry.getValue().getStartTime())
					|| (tempTaskExecution.getStartTime()
							.equals(taskExecutionMapEntry.getValue().getStartTime())
							&& tempTaskExecution.getExecutionId() < taskExecutionMapEntry
									.getValue().getExecutionId())) {
				tempTaskExecutions.put(taskExecutionMapEntry.getValue().getTaskName(),
						taskExecutionMapEntry.getValue());
			}
		}
		final List<TaskExecution> latestTaskExecutions = new ArrayList<>(
				tempTaskExecutions.values());
		Collections.sort(latestTaskExecutions, new TaskExecutionComparator());
		return latestTaskExecutions;
	}

	@Override
	public TaskExecution getLatestTaskExecutionForTaskName(String taskName) {
		Assert.hasText(taskName, "The task name must not be empty.");
		final List<TaskExecution> taskExecutions = this
				.getLatestTaskExecutionsByTaskNames(taskName);
		if (taskExecutions.isEmpty()) {
			return null;
		}
		else if (taskExecutions.size() == 1) {
			return taskExecutions.get(0);
		}
		else {
			throw new IllegalStateException(
					"Only expected a single TaskExecution but received "
							+ taskExecutions.size());
		}
	}

	private static class TaskExecutionComparator
			implements Comparator<TaskExecution>, Serializable {

		@Override
		public int compare(TaskExecution firstTaskExecution,
				TaskExecution secondTaskExecution) {
			if (firstTaskExecution.getStartTime()
					.equals(secondTaskExecution.getStartTime())) {
				return Long.compare(firstTaskExecution.getExecutionId(),
						secondTaskExecution.getExecutionId());
			}
			else {
				return secondTaskExecution.getStartTime()
						.compareTo(firstTaskExecution.getStartTime());
			}
		}

	}

}
