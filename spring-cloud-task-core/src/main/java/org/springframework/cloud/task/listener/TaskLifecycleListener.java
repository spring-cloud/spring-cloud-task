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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ExitCodeEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.Assert;

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
public class TaskLifecycleListener implements ApplicationListener<ApplicationEvent>{

	private final static Logger logger = LoggerFactory.getLogger(TaskLifecycleListener.class);

	private final TaskRepository taskRepository;

	private TaskExecution taskExecution;

	private boolean started = false;

	private TaskNameResolver taskNameResolver;

	private ApplicationArguments applicationArguments;

	private ApplicationFailedEvent applicationFailedEvent;

	private ExitCodeEvent exitCodeEvent;

	/**
	 * @param taskRepository The repository to record executions in.
	 */
	public TaskLifecycleListener(TaskRepository taskRepository,
			TaskNameResolver taskNameResolver,
			ApplicationArguments applicationArguments) {
		Assert.notNull(taskRepository, "A taskRepository is required");
		Assert.notNull(taskNameResolver, "A taskNameResolver is required");

		this.taskRepository = taskRepository;
		this.taskNameResolver = taskNameResolver;
		this.applicationArguments = applicationArguments;
	}

	/**
	 * Utilizes {@link ApplicationEvent}s to determine the start, end, and failure of a
	 * task.  Specifically:
	 * <ul>
	 *     <li>{@link ContextRefreshedEvent} - Start of a task</li>
	 *     <li>{@link ContextClosedEvent} - Successful end of a task</li>
	 *     <li>{@link ApplicationFailedEvent} - Failure of a task</li>
	 * </ul>
	 *
	 * @param applicationEvent The application being listened for.
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent applicationEvent) {
		if(applicationEvent instanceof ContextRefreshedEvent) {
			doTaskStart();
			started = true;
		}
		else if(applicationEvent instanceof ApplicationFailedEvent) {
			this.applicationFailedEvent = (ApplicationFailedEvent) applicationEvent;
		}
		else if(applicationEvent instanceof ExitCodeEvent){
			this.exitCodeEvent = (ExitCodeEvent) applicationEvent;
		}
		else if(applicationEvent instanceof ContextClosedEvent) {
			doTaskEnd();
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

			if(this.exitCodeEvent != null) {
				this.taskExecution.setExitCode(exitCodeEvent.getExitCode());
			}
			else if(this.applicationFailedEvent != null){
				this.taskExecution.setExitCode(1);
			}
			else{
				this.taskExecution.setExitCode(0);
			}

			if(this.applicationFailedEvent != null) {
				this.taskExecution.setExitMessage(stackTraceToString(this.applicationFailedEvent.getException()));
			}

			taskRepository.update(taskExecution);
		}
		else {
			logger.error("An event to end a task has been received for a task that has " +
					"not yet started.");
		}
	}

	private void doTaskStart() {

		if(!started) {
			List<String> args = new ArrayList<>(0);

			if(this.applicationArguments != null) {
				args = Arrays.asList(this.applicationArguments.getSourceArgs());
			}

			this.taskExecution = new TaskExecution(this.taskRepository.getNextExecutionId(),
					null, this.taskNameResolver.getTaskName(), new Date(), null, null,
					args);

			this.taskRepository.createTaskExecution(this.taskExecution);
		}
		else {
			logger.error("Multiple start events have been received.  The first one was " +
					"recorded.");
		}
	}
}
