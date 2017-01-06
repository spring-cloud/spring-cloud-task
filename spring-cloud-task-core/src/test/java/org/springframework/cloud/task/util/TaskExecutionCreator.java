/*
 * Copyright 2015-2017 the original author or authors.
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

package org.springframework.cloud.task.util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;

/**
 * Offers ability to create TaskExecutions for the test suite.
 *
 * @author Glenn Renfro
 */
public class TaskExecutionCreator {

	/**
	 * Creates a sample TaskExecution and stores it in the taskRepository.
	 *
	 * @param taskRepository the taskRepository where the taskExecution should be stored.
	 * @return the taskExecution created.
	 */
	public static TaskExecution createAndStoreEmptyTaskExecution(TaskRepository taskRepository) {
		return taskRepository.createTaskExecution();
	}

	/**
	 * Creates a sample TaskExecution and stores it in the taskRepository.
	 *
	 * @param taskRepository the taskRepository where the taskExecution should be stored.
	 * @return the taskExecution created.
	 */
	public static TaskExecution createAndStoreTaskExecutionNoParams(TaskRepository taskRepository) {
		TaskExecution expectedTaskExecution = taskRepository.createTaskExecution();
		return expectedTaskExecution;
	}

	/**
	 * Creates a sample TaskExecution and stores it in the taskRepository with params.
	 *
	 * @param taskRepository the taskRepository where the taskExecution should be stored.
	 * @return the taskExecution created.
	 */
	public static TaskExecution createAndStoreTaskExecutionWithParams(TaskRepository taskRepository) {
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoArg();
		List<String> params = new ArrayList<String>();
		params.add(UUID.randomUUID().toString());
		params.add(UUID.randomUUID().toString());
		expectedTaskExecution.setArguments(params);
		expectedTaskExecution = taskRepository.createTaskExecution(expectedTaskExecution);
		return expectedTaskExecution;
	}

	/**
	 * Updates a sample TaskExecution in the taskRepository.
	 *
	 * @param taskRepository the taskRepository where the taskExecution should be updated.
	 * @return the taskExecution created.
	 */
	public static TaskExecution completeExecution(TaskRepository taskRepository,
			TaskExecution expectedTaskExecution) {
		return taskRepository.completeTaskExecution(expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getExitCode(), expectedTaskExecution.getEndTime(),
				expectedTaskExecution.getExitMessage(), expectedTaskExecution.getErrorMessage());
	}
}
