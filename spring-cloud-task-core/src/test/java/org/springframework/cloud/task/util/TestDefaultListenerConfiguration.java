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

package org.springframework.cloud.task.util;

import org.springframework.cloud.task.listener.TaskExecutionListener;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Initializes the beans needed to TestExecutionListenerTests.
 *
 * @author Glenn Renfro
 */
@Configuration
public class TestDefaultListenerConfiguration {

	@Bean
	public TestTaskExecutionListener taskExecutionListener() {
		return new TestTaskExecutionListener();
	}


	public class TestTaskExecutionListener implements TaskExecutionListener {

		private boolean isTaskStartup;
		private boolean isTaskEnd;
		private boolean isTaskFailed;
		private TaskExecution taskExecution;

		@Override
		public void onTaskStartup(TaskExecution taskExecution) {
			isTaskStartup = true;
			this.taskExecution = taskExecution;
		}

		@Override
		public void onTaskEnd(TaskExecution taskExecution) {
			isTaskEnd = true;
			this.taskExecution = taskExecution;
		}

		@Override
		public void onTaskFailed(TaskExecution taskExecution) {
			isTaskFailed = true;
			this.taskExecution = taskExecution;
		}

		public boolean isTaskStartup() {
			return isTaskStartup;
		}

		public boolean isTaskEnd() {
			return isTaskEnd;
		}

		public boolean isTaskFailed() {
			return isTaskFailed;
		}

		public TaskExecution getTaskExecution() {
			return this.taskExecution;
		}
	}
}
