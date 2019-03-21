/*
 * Copyright 2015 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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

/**
 * Stores Task Execution Information to a in-memory map.
 *
 * @author Glenn Renfro
 */
public class MapTaskExecutionDao implements TaskExecutionDao {

	private ConcurrentMap<Long, TaskExecution> taskExecutions;

	private ConcurrentMap<Long, Set<Long>> batchJobAssociations;

	private final AtomicLong currentId = new AtomicLong(0L);

	public MapTaskExecutionDao() {
		taskExecutions = new ConcurrentHashMap<>();
		batchJobAssociations = new ConcurrentHashMap<>();
	}

	@Override
	public TaskExecution createTaskExecution(String taskName,
			Date startTime, List<String> arguments) {
		long taskExecutionId = getNextExecutionId();
		TaskExecution taskExecution = new TaskExecution(taskExecutionId, null, taskName,
				startTime, null, null, arguments);
		taskExecutions.put(taskExecutionId, taskExecution);
		return taskExecution;
	}

	@Override
	public void completeTaskExecution(long executionId, Integer exitCode, Date endTime, String exitMessage) {
		TaskExecution taskExecution= taskExecutions.get(executionId);
		taskExecution.setEndTime(endTime);
		taskExecution.setExitCode(exitCode);
		taskExecution.setExitMessage(exitMessage);
	}

	@Override
	public TaskExecution getTaskExecution(long executionId) {
			return taskExecutions.get(executionId);
	}

	@Override
	public long getTaskExecutionCountByTaskName(String taskName) {
		int count = 0;
		for (Map.Entry<Long, TaskExecution> entry : taskExecutions.entrySet()) {
			if (entry.getValue().getTaskName().equals(taskName)) {
				count++;
			}
		}
		return count;
	}

	@Override
	public long getRunningTaskExecutionCountByTaskName(String taskName) {
		int count = 0;
		for (Map.Entry<Long, TaskExecution> entry : taskExecutions.entrySet()) {
			if (entry.getValue().getTaskName().equals(taskName) &&
					entry.getValue().getEndTime() == null) {
				count++;
			}
		}
		return count;
	}

	@Override
	public long getTaskExecutionCount() {
		return taskExecutions.size();
	}

	@Override
	public Page<TaskExecution> findRunningTaskExecutions(String taskName, Pageable pageable) {
		Set<TaskExecution> result = getTaskExecutionTreeSet();
		for (Map.Entry<Long, TaskExecution> entry : taskExecutions.entrySet()) {
			if (entry.getValue().getTaskName().equals(taskName) &&
					entry.getValue().getEndTime() == null) {
				result.add(entry.getValue());
			}
		}
		return getPageFromList(new ArrayList<>(result), pageable,
				getRunningTaskExecutionCountByTaskName(taskName));
	}

	@Override
	public Page<TaskExecution> findTaskExecutionsByName(String taskName, Pageable pageable) {
		Set<TaskExecution> filteredSet = getTaskExecutionTreeSet();
		for (Map.Entry<Long, TaskExecution> entry : taskExecutions.entrySet()) {
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
		for (Map.Entry<Long, TaskExecution> entry : taskExecutions.entrySet()) {
			result.add(entry.getValue().getTaskName());
		}
		return new ArrayList<>(result);
	}

	@Override
	public Page<TaskExecution> findAll(Pageable pageable) {
		TreeSet<TaskExecution> sortedSet = getTaskExecutionTreeSet();
		sortedSet.addAll(taskExecutions.values());
		List<TaskExecution> result = new ArrayList<>(sortedSet.descendingSet());
		return getPageFromList(result, pageable, getTaskExecutionCount());
	}

	public Map<Long, TaskExecution> getTaskExecutions() {
		return Collections.unmodifiableMap(taskExecutions);
	}

	public long getNextExecutionId(){
		return currentId.getAndIncrement();
	}

	@Override
	public Long getTaskExecutionIdByJobExecutionId(long jobExecutionId) {
		Long taskId = null;

		found:

		for (Map.Entry<Long, Set<Long>> association : batchJobAssociations.entrySet()) {
			for (Long curJobExecutionId : association.getValue()) {
				if(curJobExecutionId.equals(jobExecutionId)) {
					taskId = association.getKey();
					break found;
				}
			}
		}

		return taskId;
	}

	@Override
	public Set<Long> getJobExecutionIdsByTaskExecutionId(long taskExecutionId) {
		if(batchJobAssociations.containsKey(taskExecutionId)) {
			return Collections.unmodifiableSet(batchJobAssociations.get(taskExecutionId));
		}
		else {
			return new TreeSet<>();
		}
	}

	public ConcurrentMap<Long, Set<Long>> getBatchJobAssociations() {
		return batchJobAssociations;
	}

	private TreeSet<TaskExecution> getTaskExecutionTreeSet() {
		return new TreeSet<>(new Comparator<TaskExecution>() {
			@Override
			public int compare(TaskExecution e1, TaskExecution e2) {
				int result = e1.getStartTime().compareTo(e2.getStartTime());
				if (result == 0){
					result = Long.valueOf(e1.getExecutionId()).compareTo(e2.getExecutionId());
				}
				return result;
			}
		});
	}

	private Page getPageFromList(List<TaskExecution> executionList, Pageable pageable, long maxSize){
		int toIndex = (pageable.getOffset() + pageable.getPageSize() > executionList.size()) ?
				executionList.size() : pageable.getOffset() + pageable.getPageSize();
		return new PageImpl<>(
				executionList.subList(pageable.getOffset(), toIndex),
				pageable, maxSize);
	}
}
