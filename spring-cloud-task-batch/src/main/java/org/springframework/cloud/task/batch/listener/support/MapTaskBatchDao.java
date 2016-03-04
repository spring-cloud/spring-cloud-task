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
package org.springframework.cloud.task.batch.listener.support;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.batch.core.JobExecution;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.util.Assert;

/**
 * Map implementation of the {@link TaskBatchDao}.  <note>This is intended for testing
 * purposes only!</note>
 *
 * @author Michael Minella
 */
public class MapTaskBatchDao implements TaskBatchDao {

	private Map<Long, Set<Long>> relationships;

	public MapTaskBatchDao(Map<Long, Set<Long>> relationships) {
		Assert.notNull(relationships, "Relationships must not be null");
		this.relationships = relationships;
	}

	@Override
	public void saveRelationship(TaskExecution taskExecution, JobExecution jobExecution) {
		Assert.notNull(taskExecution, "A taskExecution is required");
		Assert.notNull(jobExecution, "A jobExecution is required");

		if(this.relationships.containsKey(taskExecution.getExecutionId())) {
			this.relationships.get(taskExecution.getExecutionId()).add(jobExecution.getId());
		}
		else {
			TreeSet<Long> jobExecutionIds = new TreeSet<>();
			jobExecutionIds.add(jobExecution.getId());

			this.relationships.put(taskExecution.getExecutionId(), jobExecutionIds);
		}
	}
}
