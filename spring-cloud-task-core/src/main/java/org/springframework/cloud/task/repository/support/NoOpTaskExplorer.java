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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Provides a no-op TaskExplorer for development purposes.
 *
 * @author Michael Minella
 */
public class NoOpTaskExplorer implements TaskExplorer {

	public TaskExecution getTaskExecution(String executionId) {
		return null;
	}

	public Set<TaskExecution> findRunningTaskExecutions(String taskName) {
		return new HashSet<TaskExecution>(0);
	}

	public List<String> getTaskNames() {
		return new ArrayList<String>(0);
	}

	public long getTaskExecutionCountByTaskName(String taskName) {
		return 0;
	}

	@Override
	public long getTaskExecutionCount() {
		return 0;
	}

	public List<TaskExecution> getTaskExecutionsByName(String taskName, int start, int count) {
		return new ArrayList<TaskExecution>(0);
	}

	@Override
	public Page<TaskExecution> findAll(Pageable pageable) {
		return new PageImpl<TaskExecution>(new ArrayList<TaskExecution>(0), pageable, 0);
	}

}
