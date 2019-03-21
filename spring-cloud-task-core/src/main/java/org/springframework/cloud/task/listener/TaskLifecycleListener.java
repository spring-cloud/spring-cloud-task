/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.cloud.task.listener;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ExitCodeEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
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
 *     <li>{@link ApplicationReadyEvent} - Used to identify the successful end of a task.</li>
 *     <li>{@link ApplicationFailedEvent} - Used to identify the failure of a task.</li>
 * </ul>
 *
 * <b>Note:</b> By default, the context will be closed at the completion of a task (once
 * the task repository has been updated).  This behavior can be configured via the
 * property <code>spring.cloud.task.closecontext.enable</code> (defaults to true).
 *
 * @author Michael Minella
 */
public class TaskLifecycleListener implements ApplicationListener<ApplicationEvent>, SmartLifecycle, DisposableBean {

	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired(required = false)
	private Collection<TaskExecutionListener> taskExecutionListeners;

	private final static Log logger = LogFactory.getLog(TaskLifecycleListener.class);

	private final TaskRepository taskRepository;

	private TaskExecution taskExecution;

	private boolean started = false;

	private boolean finished = false;

	private TaskNameResolver taskNameResolver;

	private ApplicationArguments applicationArguments;

	private ApplicationFailedEvent applicationFailedEvent;

	private ExitCodeEvent exitCodeEvent;

	@Value("${spring.cloud.task.closecontext.enable:true}")
	private Boolean closeContext;

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
	 *     <li>{@link ApplicationReadyEvent} - Successful end of a task</li>
	 *     <li>{@link ApplicationFailedEvent} - Failure of a task</li>
	 * </ul>
	 *
	 * @param applicationEvent The application being listened for.
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent applicationEvent) {
		if(applicationEvent instanceof ApplicationFailedEvent) {
			this.applicationFailedEvent = (ApplicationFailedEvent) applicationEvent;
			doTaskEnd();
		}
		else if(applicationEvent instanceof ExitCodeEvent){
			this.exitCodeEvent = (ExitCodeEvent) applicationEvent;
			doTaskEnd();
		}
		else if(applicationEvent instanceof ApplicationReadyEvent) {
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
		if(this.started && !this.finished) {
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

			if(this.taskExecution.getExitCode() != 0){
				taskExecution.setExitMessage(invokeOnTaskError(taskExecution,
						this.applicationFailedEvent.getException()).getExitMessage());
			}
			taskExecution.setExitMessage(invokeOnTaskEnd(taskExecution).getExitMessage());
			taskRepository.completeTaskExecution(taskExecution.getExecutionId(), taskExecution.getExitCode(),
					taskExecution.getEndTime(), taskExecution.getExitMessage());

			this.finished = true;

			if(this.closeContext && this.context.isActive()) {
				this.context.close();
			}

		}
		else if(!this.started){
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

			this.taskExecution = this.taskRepository.createTaskExecution(
					this.taskNameResolver.getTaskName(), new Date(), args);
		}
		else {
			logger.error("Multiple start events have been received.  The first one was " +
					"recorded.");
		}
		taskExecution.setExitMessage(invokeOnTaskStartup(taskExecution).getExitMessage());
	}

	private TaskExecution invokeOnTaskStartup(TaskExecution taskExecution){
		TaskExecution listenerTaskExecution = getTaskExecutionCopy(taskExecution);
		if (taskExecutionListeners != null) {
			for (TaskExecutionListener taskExecutionListener : taskExecutionListeners) {
				taskExecutionListener.onTaskStartup(listenerTaskExecution);
			}
		}
		return listenerTaskExecution;
	}

	private TaskExecution invokeOnTaskEnd(TaskExecution taskExecution){
		TaskExecution listenerTaskExecution = getTaskExecutionCopy(taskExecution);
		if (taskExecutionListeners != null) {
			for (TaskExecutionListener taskExecutionListener : taskExecutionListeners) {
				taskExecutionListener.onTaskEnd(listenerTaskExecution);
			}
		}
		return listenerTaskExecution;
	}

	private TaskExecution invokeOnTaskError(TaskExecution taskExecution, Throwable throwable){
		TaskExecution listenerTaskExecution = getTaskExecutionCopy(taskExecution);
		if (taskExecutionListeners != null) {
			for (TaskExecutionListener taskExecutionListener : taskExecutionListeners) {
				taskExecutionListener.onTaskFailed(listenerTaskExecution, throwable);
			}
		}
		return listenerTaskExecution;
	}

	private TaskExecution getTaskExecutionCopy(TaskExecution taskExecution){
		Date startTime = new Date(taskExecution.getStartTime().getTime());
		Date endTime = (taskExecution.getEndTime() == null) ?
				null : new Date(taskExecution.getEndTime().getTime());

		return new TaskExecution(taskExecution.getExecutionId(),
				taskExecution.getExitCode(), taskExecution.getTaskName(), startTime,
				endTime,taskExecution.getExitMessage(),
				Collections.unmodifiableList(taskExecution.getArguments()));
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		Assert.notNull(callback, "A callback is required");

		callback.run();
	}

	@Override
	public void start() {
		doTaskStart();
		this.started = true;
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean isRunning() {
		return this.started;
	}

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public void destroy() throws Exception {
		this.doTaskEnd();
	}
}
