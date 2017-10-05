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

package org.springframework.cloud.task.listener.annotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.springframework.cloud.task.listener.TaskExecutionException;
import org.springframework.cloud.task.listener.TaskExecutionListener;
import org.springframework.cloud.task.repository.TaskExecution;

/**
 * Identifies all beans that contain a TaskExecutionListener annotation and stores the
 * associated method so that it can be called by the {@link TaskExecutionListener} at the
 * appropriate time.
 *
 * @author Glenn Renfro
 */
public class TaskListenerExecutor implements TaskExecutionListener{

	private Map<Method, Object> beforeTaskInstances;

	private Map<Method, Object> afterTaskInstances;

	private Map<Method, Object> failedTaskInstances;

	public TaskListenerExecutor(Map<Method, Object> beforeTaskInstances,
			Map<Method, Object> afterTaskInstances,
			Map<Method, Object> failedTaskInstances){

		this.beforeTaskInstances = beforeTaskInstances;
		this.afterTaskInstances = afterTaskInstances;
		this.failedTaskInstances = failedTaskInstances;
	}

	/**
	 * Executes all the methods that have been annotated with  &#064;BeforeTask.
	 * @param taskExecution associated with the event.
	 */
	@Override
	public void onTaskStartup(TaskExecution taskExecution) {
		executeTaskListener(taskExecution, beforeTaskInstances.keySet(), beforeTaskInstances);
	}

	/**
	 * Executes all the methods that have been annotated with  &#064;AfterTask.
	 * @param taskExecution associated with the event.
	 */
	@Override
	public void onTaskEnd(TaskExecution taskExecution) {
		executeTaskListener(taskExecution, afterTaskInstances.keySet(), afterTaskInstances);
	}

	/**
	 * Executes all the methods that have been annotated with  &#064;FailedTask.
	 * @param throwable that was not caught for the task execution.
	 * @param taskExecution associated with the event.
	 */
	@Override
	public void onTaskFailed(TaskExecution taskExecution, Throwable throwable) {
		executeTaskListenerWithThrowable(taskExecution, throwable,
				failedTaskInstances.keySet(),failedTaskInstances);
	}

	private void executeTaskListener(TaskExecution taskExecution, Set<Method> methods, Map<Method, Object> instances){
		for (Method method : methods) {
			try {
				method.invoke(instances.get(method),taskExecution);
			}
			catch (IllegalAccessException e) {
				throw new TaskExecutionException("@BeforeTask and @AfterTask annotated methods must be public.", e);
			}
			catch (InvocationTargetException e) {
				throw new TaskExecutionException(String.format("Failed to process @BeforeTask or @AfterTask" +
						" annotation because: %s", e.getTargetException().getMessage()), e);
			}
			catch (IllegalArgumentException e){
				throw new TaskExecutionException("taskExecution parameter is required for @BeforeTask and @AfterTask annotated methods", e);
			}
		}
	}

	private void executeTaskListenerWithThrowable(TaskExecution taskExecution,
			Throwable throwable, Set<Method> methods, Map<Method, Object> instances){
		for (Method method : methods) {
			try {
				method.invoke(instances.get(method),taskExecution, throwable);
			}
			catch (IllegalAccessException e) {
				throw new TaskExecutionException("@FailedTask annotated methods must be public.", e);
			}
			catch (InvocationTargetException e) {
				throw new TaskExecutionException(String.format("Failed to process @FailedTask " +
						"annotation because: %s", e.getTargetException().getMessage()), e);
			}
			catch (IllegalArgumentException e){
				throw new TaskExecutionException("taskExecution and throwable parameters "
						+ "are required for @FailedTask annotated methods", e);
			}
		}
	}

}
