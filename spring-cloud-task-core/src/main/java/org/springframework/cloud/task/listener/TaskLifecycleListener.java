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
package org.springframework.cloud.task.listener;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Monitors the lifecycle of a task.  This listener will record both the start and end of
 * a task in the registered {@link TaskRepository}.
 *
 * The following events are used to identify the start and end of a task:
 *
 * <ul>
 *     <li>{@link ContextRefreshedEvent} - Used to identify the start of a task.  A task
 *     is expected to contain a single application context.</li>
 *     <li>{@link ContextClosedEvent} - Used to identify the successful end of a task.</li>
 *     <li>{@link ApplicationFailedEvent} - Used to identify the failure of a task.</li>
 * </ul>
 *
 * <b>NOTE:</b> Multiple contexts (including parent/child relationships) will result in
 * only the first context refresh that contains this instance being recorded.
 *
 * @author Michael Minella
 */
public class TaskLifecycleListener implements ApplicationListener<ApplicationEvent>,
		ApplicationContextAware {

	private final static Logger logger = LoggerFactory.getLogger(TaskLifecycleListener.class);

	private final TaskRepository taskRepository;

	private TaskExecution taskExecution;

	private boolean started = false;

	private ApplicationContext applicationContext;

	/**
	 * @param taskRepository The repository to record executions in.
	 */
	public TaskLifecycleListener(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;
	}

	/**
	 * @param applicationEvent The application being listened for.
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent applicationEvent) {
		if(applicationEvent instanceof ContextRefreshedEvent) {
			doTaskStart();
			started = true;
		}
		else if(applicationEvent instanceof ContextClosedEvent) {
			doTaskEnd();
		}
		else if(applicationEvent instanceof ApplicationFailedEvent) {
			doTaskFailed(((ApplicationFailedEvent) applicationEvent).getException());
		}
	}

	private void doTaskFailed(Throwable exception) {

		if(started) {
			this.taskExecution.setEndTime(new Date());
			//TODO: get exit code from new event thrown by Boot.
			this.taskExecution.setExitCode(1);
			this.taskExecution.setExitMessage(stackTraceToString(exception));

			taskRepository.update(taskExecution);
		}
		else  {
			logger.warn("An event to fail a task has been received for a task that has " +
					"not yet started.");
		}
	}

	private String stackTraceToString(Throwable exception) {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);

		exception.printStackTrace(printWriter);

		return writer.toString();
	}

	private void doTaskEnd() {
		if(started) {
			this.taskExecution.setEndTime(new Date());
			//TODO: get exit code from new event thrown by Boot.
			this.taskExecution.setExitCode(0);

			taskRepository.update(taskExecution);
		}
		else {
			logger.warn("An event to end a task has been received for a task that has " +
					"not yet started.");
		}
	}

	private void doTaskStart() {

		if(!started) {

			String executionId = UUID.randomUUID().toString();
			this.taskExecution = new TaskExecution();

			this.taskExecution.setTaskName(applicationContext.getId());
			this.taskExecution.setStartTime(new Date());
			this.taskExecution.setExecutionId(executionId);
			this.taskRepository.createTaskExecution(this.taskExecution);
		}
		else {
			logger.warn("Multiple start events have been received.  The first one was " +
					"recorded.");
		}
	}

	/**
	 * Used for testing purposes
	 * @return the current {@link TaskExecution}
	 */
	TaskExecution getTaskExecution() {
		return this.taskExecution;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}
}
