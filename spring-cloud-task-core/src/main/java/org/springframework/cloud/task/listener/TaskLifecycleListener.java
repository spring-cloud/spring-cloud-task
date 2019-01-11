/*
 * Copyright 2016-2018 the original author or authors.
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
import java.lang.reflect.InvocationTargetException;
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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ExitCodeEvent;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Monitors the lifecycle of a task.  This listener will record both the start and end of
 * a task in the registered {@link TaskRepository}.
 *
 * The following events are used to identify the start and end of a task:
 *
 * <ul>
 *     <li>{@link SmartLifecycle#start()}  - Used to identify the start of a task.  A task
 *     is expected to contain a single application context.</li>
 *     <li>{@link ApplicationReadyEvent} - Used to identify the successful end of a task.</li>
 *     <li>{@link ApplicationFailedEvent} - Used to identify the failure of a task.</li>
 *     <li>{@link SmartLifecycle#stop()} - Used to identify the end of a task,
 *     if the {@link ApplicationReadyEvent} or {@link ApplicationFailedEvent}
 *     is not emitted. This can occur if an error occurs while executing a BeforeTask.
 *     </li>
 * </ul>
 *
 * <b>Note:</b> By default, the context will close at the completion of the task unless other non-daemon
 * threads keep it running.  Programatic closing of the context can be configured via the
 * property <code>spring.cloud.task.closecontext.enabled</code> (defaults to false).
 * If the <code>spring.cloud.task.closecontext.enabled</code> is set to true,
 * then the context will be closed upon task completion regardless if non-daemon threads are still running.
 * Also if the context did not start, the FailedTask and TaskEnd may not have all the dependencies met.
 *
 * @author Michael Minella
 * @author Glenn Renfro
 */
public class TaskLifecycleListener implements ApplicationListener<ApplicationEvent>, SmartLifecycle, DisposableBean {

	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired(required = false)
	private Collection<TaskExecutionListener> taskExecutionListenersFromContext;

	private List<TaskExecutionListener> taskExecutionListeners;

	private static final  Log logger = LogFactory.getLog(TaskLifecycleListener.class);

	private final TaskRepository taskRepository;

	private final TaskExplorer taskExplorer;

	private final TaskListenerExecutorObjectFactory taskListenerExecutorObjectFactory;

	private TaskExecution taskExecution;

	private TaskProperties taskProperties;

	private boolean started = false;

	private boolean finished = false;

	private boolean listenerFailed = false;

	private Throwable listenerException;

	private TaskNameResolver taskNameResolver;

	private ApplicationArguments applicationArguments;

	private Throwable applicationFailedException;

	private ExitCodeEvent exitCodeEvent;

	/**
	 * @param taskRepository {@link TaskRepository} to record executions.
	 * @param taskNameResolver {@link TaskNameResolver} used to determine task name for task execution.
	 * @param applicationArguments {@link ApplicationArguments} to be used for task execution.
	 * @param taskExplorer {@link TaskExplorer} to be used for task execution.
	 * @param taskProperties {@link TaskProperties} to be used for the task execution.
	 * @param taskListenerExecutorObjectFactory {@link TaskListenerExecutorObjectFactory} to initialize TaskListenerExecutor for a task
	 */
	public TaskLifecycleListener(TaskRepository taskRepository,
			TaskNameResolver taskNameResolver,
			ApplicationArguments applicationArguments, TaskExplorer taskExplorer,
			TaskProperties taskProperties,
			TaskListenerExecutorObjectFactory taskListenerExecutorObjectFactory) {
		Assert.notNull(taskRepository, "A taskRepository is required");
		Assert.notNull(taskNameResolver, "A taskNameResolver is required");
		Assert.notNull(taskExplorer, "A taskExplorer is required");
		Assert.notNull(taskProperties, "TaskProperties is required");
		Assert.notNull(taskListenerExecutorObjectFactory, "A TaskListenerExecutorObjectFactory is required");

		this.taskRepository = taskRepository;
		this.taskNameResolver = taskNameResolver;
		this.applicationArguments = applicationArguments;
		this.taskExplorer = taskExplorer;
		this.taskProperties = taskProperties;
		this.taskListenerExecutorObjectFactory = taskListenerExecutorObjectFactory;
	}

	/**
	 * Utilizes {@link ApplicationEvent}s to determine the end and failure of a
	 * task.  Specifically:
	 * <ul>
	 *     <li>{@link ApplicationReadyEvent} - Successful end of a task</li>
	 *     <li>{@link ApplicationFailedEvent} - Failure of a task</li>
	 * </ul>
	 *
	 * @param applicationEvent The application being listened for.
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent applicationEvent) {
		if(applicationEvent instanceof ApplicationFailedEvent) {
			this.applicationFailedException = ((ApplicationFailedEvent) applicationEvent).getException();
			doTaskEnd();
		}
		else if(applicationEvent instanceof ExitCodeEvent){
			this.exitCodeEvent = (ExitCodeEvent) applicationEvent;
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
		if((this.listenerFailed || this.started) && !this.finished) {
			this.taskExecution.setEndTime(new Date());

			if(this.applicationFailedException != null) {
				this.taskExecution.setErrorMessage(stackTraceToString(this.applicationFailedException));
			}

			this.taskExecution.setExitCode(calcExitStatus());
			if (this.applicationFailedException != null) {
				setExitMessage(invokeOnTaskError(this.taskExecution, this.applicationFailedException));
			}

			setExitMessage(invokeOnTaskEnd(this.taskExecution));
			this.taskRepository.completeTaskExecution(this.taskExecution.getExecutionId(), this.taskExecution.getExitCode(),
					this.taskExecution.getEndTime(), this.taskExecution.getExitMessage(), this.taskExecution.getErrorMessage());

			this.finished = true;

			if(this.taskProperties.getClosecontextEnabled() && this.context.isActive()) {
				this.context.close();
			}

		}
		else if(!this.started){
			logger.error("An event to end a task has been received for a task that has " +
					"not yet started.");
		}
	}

	private void setExitMessage(TaskExecution taskExecutionParam) {
		if(taskExecutionParam.getExitMessage() != null) {
			this.taskExecution.setExitMessage(taskExecutionParam.getExitMessage());
		}
	}

	private int calcExitStatus() {
		int exitCode = 0;
		if (this.exitCodeEvent != null) {
			exitCode = this.exitCodeEvent.getExitCode();
		}
		else if (this.listenerFailed || this.applicationFailedException != null) {
			Throwable exception = this.listenerException;
			if (exception instanceof TaskExecutionException) {
				TaskExecutionException taskExecutionException = (TaskExecutionException) exception;
				if (taskExecutionException.getCause() instanceof InvocationTargetException) {
					InvocationTargetException invocationTargetException = (InvocationTargetException) taskExecutionException
							.getCause();
					if(invocationTargetException != null && invocationTargetException.getTargetException() != null) {
						exception = invocationTargetException.getTargetException();
					}
				}
			}

			if (exception instanceof ExitCodeGenerator) {
				exitCode = ((ExitCodeGenerator) exception).getExitCode();
			}
			else {
				exitCode = 1;
			}
		}

		return exitCode;
	}

	private void doTaskStart() {
		try {
			if(!this.started) {
				this.taskExecutionListeners = new ArrayList<>();
				this.taskListenerExecutorObjectFactory.getObject();
				if(!CollectionUtils.isEmpty(this.taskExecutionListenersFromContext)) {
					this.taskExecutionListeners.addAll(this.taskExecutionListenersFromContext);
				}
				this.taskExecutionListeners.add(this.taskListenerExecutorObjectFactory.getObject());

				List<String> args = new ArrayList<>(0);

				if(this.applicationArguments != null) {
					args = Arrays.asList(this.applicationArguments.getSourceArgs());
				}
				if(this.taskProperties.getExecutionid() != null) {
					TaskExecution taskExecution = this.taskExplorer.getTaskExecution(this.taskProperties.getExecutionid());
					Assert.notNull(taskExecution, String.format("Invalid TaskExecution, ID %s not found", this.taskProperties.getExecutionid()));
					Assert.isNull(taskExecution.getEndTime(), String.format(
							"Invalid TaskExecution, ID %s task is already complete", this.taskProperties.getExecutionid()));
					this.taskExecution = this.taskRepository.startTaskExecution(this.taskProperties.getExecutionid(),
							this.taskNameResolver.getTaskName(), new Date(), args,
							this.taskProperties.getExternalExecutionId(),
							this.taskProperties.getParentExecutionId());
				}
				else {
					TaskExecution taskExecution = new TaskExecution();
					taskExecution.setTaskName(this.taskNameResolver.getTaskName());
					taskExecution.setStartTime(new Date());
					taskExecution.setArguments(args);
					taskExecution.setExternalExecutionId(this.taskProperties.getExternalExecutionId());
					taskExecution.setParentExecutionId(this.taskProperties.getParentExecutionId());
					this.taskExecution = this.taskRepository.createTaskExecution(
							taskExecution);
				}
			}
			else {
				logger.error("Multiple start events have been received.  The first one was " +
						"recorded.");
			}

			setExitMessage(invokeOnTaskStartup(this.taskExecution));
		}
		catch (Throwable t) {
			// This scenario will result in a context that was not startup.
			this.applicationFailedException = t;
			this.doTaskEnd();
			throw t;
		}
	}

	private TaskExecution invokeOnTaskStartup(TaskExecution taskExecution){
		TaskExecution listenerTaskExecution = getTaskExecutionCopy(taskExecution);
		List<TaskExecutionListener> startupListenerList = new ArrayList<>(this.taskExecutionListeners);
		if (!CollectionUtils.isEmpty(startupListenerList)) {
			try {
				Collections.reverse(startupListenerList);
				for (TaskExecutionListener taskExecutionListener : startupListenerList) {
					taskExecutionListener.onTaskStartup(listenerTaskExecution);
				}
			}
			catch (Throwable currentListenerException) {
				logger.error(currentListenerException);
				this.listenerFailed = true;
				this.taskExecution.setErrorMessage(currentListenerException.getMessage());
				this.listenerException = currentListenerException;
				throw currentListenerException;
			}
		}
		return listenerTaskExecution;
	}

	private TaskExecution invokeOnTaskEnd(TaskExecution taskExecution){
		TaskExecution listenerTaskExecution = getTaskExecutionCopy(taskExecution);
		if (this.taskExecutionListeners != null) {
			try {
				for (TaskExecutionListener taskExecutionListener : this.taskExecutionListeners) {
					taskExecutionListener.onTaskEnd(listenerTaskExecution);
				}
			}
			catch (Throwable listenerException) {
				String errorMessage = stackTraceToString(listenerException);
				if (StringUtils.hasText(listenerTaskExecution.getErrorMessage())) {
					errorMessage = String.format("%s :Task also threw this Exception: %s", errorMessage, listenerTaskExecution.getErrorMessage());
				}
				logger.error(errorMessage);
				listenerTaskExecution.setErrorMessage(errorMessage);
				this.listenerFailed = true;
			}
		}
		return listenerTaskExecution;
	}

	private TaskExecution invokeOnTaskError(TaskExecution taskExecution, Throwable throwable){
		TaskExecution listenerTaskExecution = getTaskExecutionCopy(taskExecution);
		if (this.taskExecutionListeners != null) {
			try {
				for (TaskExecutionListener taskExecutionListener : this.taskExecutionListeners) {
					taskExecutionListener.onTaskFailed(listenerTaskExecution, throwable);
				}
			}
			catch (Throwable listenerException) {
				this.listenerFailed = true;
				String errorMessage;
				if(StringUtils.hasText(listenerTaskExecution.getErrorMessage())) {
					errorMessage = String.format("%s :While handling " +
							"this error: %s", listenerException.getMessage(),
							listenerTaskExecution.getErrorMessage());
				}
				else {
					errorMessage = listenerTaskExecution.getErrorMessage();
				}
				logger.error(errorMessage);
				listenerTaskExecution.setErrorMessage(errorMessage);
				listenerTaskExecution.setExitCode(1);
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
				Collections.unmodifiableList(taskExecution.getArguments()),
				taskExecution.getErrorMessage(), taskExecution.getExternalExecutionId());
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		Assert.notNull(callback, "A callback is required");
		stop();
		callback.run();
	}

	@Override
	public void start() {
		doTaskStart();
		this.started = true;
	}

	@Override
	public void stop() {
		this.doTaskEnd();
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
	public void destroy() {
	}

}
