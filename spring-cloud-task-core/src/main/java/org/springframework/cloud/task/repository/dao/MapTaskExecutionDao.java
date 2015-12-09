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

package org.springframework.cloud.task.repository.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.cloud.task.repository.TaskExecution;

/**
 * Stores Task Execution Information to a in-memory map.
 *
 * @author Glenn Renfro
 */
public class MapTaskExecutionDao implements TaskExecutionDao {

	private ConcurrentMap<String, TaskExecution> taskExecutions;

	public MapTaskExecutionDao() {
		taskExecutions = new ConcurrentHashMap<>();
	}

	@Override
	public void saveTaskExecution(TaskExecution taskExecution) {
		taskExecutions.put(taskExecution.getExecutionId(), taskExecution);
	}

	@Override
	public void updateTaskExecution(TaskExecution taskExecution) {
		taskExecutions.put(taskExecution.getExecutionId(), taskExecution);
	}

	@Override
	public TaskExecution getTaskExecution(String executionId) {
		return taskExecutions.get(executionId);
	}

	@Override
	public long getTaskExecutionCount(String taskName) {
		int count = 0;
		for (Map.Entry<String, TaskExecution> entry : taskExecutions.entrySet()) {
			if (entry.getValue().getTaskName().equals(taskName)) {
				count++;
			}
		}
		return count;
	}

	@Override
	public Set<TaskExecution> findRunningTaskExecutions(String taskName) {
		Set<TaskExecution> result = new HashSet<>();
		for (Map.Entry<String, TaskExecution> entry : taskExecutions.entrySet()) {
			if (entry.getValue().getTaskName().equals(taskName) &&
					entry.getValue().getEndTime() == null) {
				result.add(entry.getValue());
			}
		}
		return Collections.unmodifiableSet(result);
	}

	@Override
	public List<TaskExecution> getTaskExecutionsByName(String taskName, int start, int count) {
		List<TaskExecution> result = new ArrayList<>();
		Set<TaskExecution> filteredSet = new HashSet<>();
		for (Map.Entry<String, TaskExecution> entry : taskExecutions.entrySet()) {
			if (entry.getValue().getTaskName().equals(taskName)) {
				filteredSet.add(entry.getValue());
			}
		}
		int rowNum = 0;
		Iterator<TaskExecution> rs = filteredSet.iterator();
		while (rowNum < start && rs.hasNext()) {
			rs.next();
			rowNum++;
		}
		while (rowNum < start + count && rs.hasNext()) {
			result.add(rs.next());
			rowNum++;
		}

		return Collections.unmodifiableList(result);
	}

	@Override
	public List<String> getTaskNames() {
		Set<String> result = new HashSet<>();
		for (Map.Entry<String, TaskExecution> entry : taskExecutions.entrySet()) {
			result.add(entry.getValue().getTaskName());
		}
		return Collections.unmodifiableList(new ArrayList(result));
	}

	public Map<String, TaskExecution> getTaskExecutions() {
		return Collections.unmodifiableMap(taskExecutions);
	}
}
