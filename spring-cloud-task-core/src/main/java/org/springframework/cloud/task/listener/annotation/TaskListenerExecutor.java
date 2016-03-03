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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.aop.scope.ScopedObject;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.task.listener.TaskExecutionListener;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * Identifies all beans that contain a TaskExecutionListener annotation and stores the
 * associated method so that it can be called by the {@link TaskExecutionListener} at the
 * appropriate time.
 *
 * @author Glenn Renfro
 */
public class TaskListenerExecutor implements TaskExecutionListener{

	private ConfigurableApplicationContext context;

	private final static Logger logger = LoggerFactory.getLogger(TaskListenerExecutor.class);


	private final Set<Class<?>> nonAnnotatedClasses =
			Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>());

	private Map<Method, BeforeTask> beforeTaskMethods;
	private Map<Method, Object> beforeTaskInstances;

	private Map<Method, AfterTask> afterTaskMethods;
	private Map<Method, Object> afterTaskInstances;

	private Map<Method, FailedTask> failedTaskMethods;
	private Map<Method, Object> failedTaskInstances;

	public TaskListenerExecutor(ConfigurableApplicationContext context){
		this.context = context;
		this.beforeTaskMethods = new HashMap<>();
		this.beforeTaskInstances = new HashMap<>();
		this.afterTaskMethods = new HashMap<>();
		this.afterTaskInstances = new HashMap<>();
		this.failedTaskMethods = new HashMap<>();
		this.failedTaskInstances = new HashMap<>();
		initializeExecutor();
	}

	/**
	 * Executes all the methods that have been annotated with  &#064;BeforeTask.
	 * @param taskExecution associated with the event.
	 */
	@Override
	public void onTaskStartup(TaskExecution taskExecution) {
		executeTaskListener(taskExecution, beforeTaskMethods.keySet(), beforeTaskInstances);
	}

	/**
	 * Executes all the methods that have been annotated with  &#064;AfterTask.
	 * @param taskExecution associated with the event.
	 */
	@Override
	public void onTaskEnd(TaskExecution taskExecution) {
		executeTaskListener(taskExecution, afterTaskMethods.keySet(), afterTaskInstances);
	}

	/**
	 * Executes all the methods that have been annotated with  &#064;FailedTask.
	 * @param throwable that was not caught for the task execution.
	 * @param taskExecution associated with the event.
	 */
	@Override
	public void onTaskFailed(TaskExecution taskExecution, Throwable throwable) {
		executeTaskListenerWithThrowable(taskExecution, throwable,
				failedTaskMethods.keySet(),failedTaskInstances);
	}

	private void executeTaskListener(TaskExecution taskExecution, Set<Method> methods, Map<Method, Object> instances){
		for (Method method : methods) {
			try {
				method.invoke(instances.get(method),taskExecution);
			}
			catch (IllegalAccessException e) {
				throw new IllegalStateException("@BeforeTask and @AfterTask annotated methods must be public.");
			}
			catch (InvocationTargetException e) {
				throw new IllegalStateException("Failed to process @BeforeTask or @AfterTask" +
						"annotation because: ", e);
			}
			catch (IllegalArgumentException e){
				throw new IllegalStateException("taskExecution parameter is required for @BeforeTask and @AfterTask annotated methods");
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
				throw new IllegalStateException("@FailedTask annotated methods must be public.");
			}
			catch (InvocationTargetException e) {
				throw new IllegalStateException("Failed to process @FailedTask " +
						"annotation because: ", e);
			}
			catch (IllegalArgumentException e){
				throw new IllegalStateException("taskExecution and throwable parameters "
						+ "are required for @FailedTask annotated methods");
			}
		}
	}

	private void initializeExecutor( ) {
		ConfigurableListableBeanFactory factory = context.getBeanFactory();
		for( String beanName : context.getBeanNamesForType(Object.class)) {

			if (!ScopedProxyUtils.isScopedTarget(beanName)) {
				Class<?> type = null;
				try {
					type = AutoProxyUtils.determineTargetClass(this.context.getBeanFactory(), beanName);
				}
				catch (Throwable ex) {
					// An unresolvable bean type, probably from a lazy bean - let's ignore it.
					if (logger.isDebugEnabled()) {
						logger.debug("Could not resolve target class for bean with name '" + beanName + "'", ex);
					}
				}
				if (type != null) {
					if (ScopedObject.class.isAssignableFrom(type)) {
						try {
							type = AutoProxyUtils.determineTargetClass(this.context.getBeanFactory(),
									ScopedProxyUtils.getTargetBeanName(beanName));
						}
						catch (Throwable ex) {
							// An invalid scoped proxy arrangement - let's ignore it.
							if (logger.isDebugEnabled()) {
								logger.debug("Could not resolve target bean for scoped proxy '" + beanName + "'", ex);
							}
						}
					}
					try {
						processBean(beanName, type);
					}
					catch (Throwable ex) {
						throw new BeanInitializationException("Failed to process @BeforeTask " +
								"annotation on bean with name '" + beanName + "'", ex);
					}
				}
			}
		}

	}

	private void processBean(String beanName, final Class<?> type){
		if (!this.nonAnnotatedClasses.contains(type)) {
			Map<Method, BeforeTask> beforeTaskMethods = getBeforeTaskMethods(type);
			Map<Method, AfterTask> afterTaskMethods = getAfterTaskMethods(type);
			Map<Method, FailedTask> failedTaskMethods = getFailedTaskMethods(type);

			if (beforeTaskMethods.isEmpty() && afterTaskMethods.isEmpty()) {
				this.nonAnnotatedClasses.add(type);
				return;
			}
			if(!beforeTaskMethods.isEmpty()) {
				this.beforeTaskMethods.putAll(beforeTaskMethods);
				for(Method beforeTaskMethod : beforeTaskMethods.keySet()) {
					this.beforeTaskInstances.put(beforeTaskMethod, context.getBean(beanName));
				}
			}
			if(!afterTaskMethods.isEmpty()){
				this.afterTaskMethods.putAll(afterTaskMethods);
				for(Method afterTaskMethod : afterTaskMethods.keySet()) {
					this.afterTaskInstances.put(afterTaskMethod, context.getBean(beanName));
				}
			}
			if(!failedTaskMethods.isEmpty()){
				this.failedTaskMethods.putAll(failedTaskMethods);
				for(Method failedTaskMethod : failedTaskMethods.keySet()) {
					this.failedTaskInstances.put(failedTaskMethod, context.getBean(beanName));
				}
			}
		}
	}


	private Map<Method, BeforeTask> getBeforeTaskMethods(final Class<?> type){
		return MethodIntrospector.selectMethods(type,
				new MethodIntrospector.MetadataLookup<BeforeTask>() {
					@Override
					public BeforeTask inspect(Method method) {
						return AnnotationUtils.findAnnotation(method, BeforeTask.class);
					}
				});
	}

	private Map<Method, AfterTask> getAfterTaskMethods(final Class<?> type){
		return MethodIntrospector.selectMethods(type,
				new MethodIntrospector.MetadataLookup<AfterTask>() {
					@Override
					public AfterTask inspect(Method method) {
						return AnnotationUtils.findAnnotation(method, AfterTask.class);
					}
				});
	}

	private Map<Method, FailedTask> getFailedTaskMethods(final Class<?> type){
		return MethodIntrospector.selectMethods(type,
				new MethodIntrospector.MetadataLookup<FailedTask>() {
					@Override
					public FailedTask inspect(Method method) {
						return AnnotationUtils.findAnnotation(method, FailedTask.class);
					}
				});
	}
}
