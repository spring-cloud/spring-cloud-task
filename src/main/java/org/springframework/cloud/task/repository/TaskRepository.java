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

package org.springframework.cloud.task.repository;

/**
 * TaskRepository interface offers methods that create and update task execution
 * information.  The interface will support the following methods:
 *
 * @author Glenn Renfro
 */
public interface TaskRepository {

	/**
	 * Notifies the repository that a taskExecution needs to be updated.
	 *
	 * @param taskExecution taskExecution to be updated
	 */
	public void update(TaskExecution taskExecution);

	/**
	 * Notifies the repository that a taskExecution needs to be created.
	 *
	 * @param taskExecution taskExecution to be recorded
	 */
	public void createTaskExecution(TaskExecution taskExecution);
}
