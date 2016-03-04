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

import java.lang.annotation.Annotation;
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

	private Map<Method, Object> beforeTaskInstances;

	private Map<Method, Object> afterTaskInstances;

	private Map<Method, Object> failedTaskInstances;

	private TaskListenerExecutor(){
	}

	public TaskListenerExecutor(ConfigurableApplicationContext context){
		this.context = context;
		this.beforeTaskInstances = new HashMap<>();
		this.afterTaskInstances = new HashMap<>();
		this.failedTaskInstances = new HashMap<>();
		initializeExecutor();
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
				throw new TaskAnnotationException("@BeforeTask and @AfterTask annotated methods must be public.", e);
			}
			catch (InvocationTargetException e) {
				throw new TaskAnnotationException("Failed to process @BeforeTask or @AfterTask" +
						"annotation because: ", e);
			}
			catch (IllegalArgumentException e){
				throw new TaskAnnotationException("taskExecution parameter is required for @BeforeTask and @AfterTask annotated methods", e);
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
				throw new TaskAnnotationException("@FailedTask annotated methods must be public.", e);
			}
			catch (InvocationTargetException e) {
				throw new TaskAnnotationException("Failed to process @FailedTask " +
						"annotation because: ", e);
			}
			catch (IllegalArgumentException e){
				throw new TaskAnnotationException("taskExecution and throwable parameters "
						+ "are required for @FailedTask annotated methods", e);
			}
		}
	}

	private void initializeExecutor( ) {
		ConfigurableListableBeanFactory factory = context.getBeanFactory();
		for( String beanName : context.getBeanDefinitionNames()) {

			if (!ScopedProxyUtils.isScopedTarget(beanName)) {
				Class<?> type = null;
				try {
					type = AutoProxyUtils.determineTargetClass(factory, beanName);
				}
				catch (RuntimeException ex) {
					// An unresolvable bean type, probably from a lazy bean - let's ignore it.
					if (logger.isDebugEnabled()) {
						logger.debug("Could not resolve target class for bean with name '" + beanName + "'", ex);
					}
				}
				if (type != null) {
					if (ScopedObject.class.isAssignableFrom(type)) {
						try {
							type = AutoProxyUtils.determineTargetClass(factory,
									ScopedProxyUtils.getTargetBeanName(beanName));
						}
						catch (RuntimeException ex) {
							// An invalid scoped proxy arrangement - let's ignore it.
							if (logger.isDebugEnabled()) {
								logger.debug("Could not resolve target bean for scoped proxy '" + beanName + "'", ex);
							}
						}
					}
					try {
						processBean(beanName, type);
					}
					catch (RuntimeException ex) {
						throw new BeanInitializationException("Failed to process @BeforeTask " +
								"annotation on bean with name '" + beanName + "'", ex);
					}
				}
			}
		}

	}

	private void processBean(String beanName, final Class<?> type){
		if (!this.nonAnnotatedClasses.contains(type)) {
			Map<Method, BeforeTask> beforeTaskMethods =
					(new MethodGetter<BeforeTask>()).getMethods(type, BeforeTask.class);
			Map<Method, AfterTask> afterTaskMethods =
					(new MethodGetter<AfterTask>()).getMethods(type, AfterTask.class);
			Map<Method, FailedTask> failedTaskMethods =
					(new MethodGetter<FailedTask>()).getMethods(type, FailedTask.class);

			if (beforeTaskMethods.isEmpty() && afterTaskMethods.isEmpty()) {
				this.nonAnnotatedClasses.add(type);
				return;
			}
			if(!beforeTaskMethods.isEmpty()) {
				for(Method beforeTaskMethod : beforeTaskMethods.keySet()) {
					this.beforeTaskInstances.put(beforeTaskMethod, context.getBean(beanName));
				}
			}
			if(!afterTaskMethods.isEmpty()){
				for(Method afterTaskMethod : afterTaskMethods.keySet()) {
					this.afterTaskInstances.put(afterTaskMethod, context.getBean(beanName));
				}
			}
			if(!failedTaskMethods.isEmpty()){
				for(Method failedTaskMethod : failedTaskMethods.keySet()) {
					this.failedTaskInstances.put(failedTaskMethod, context.getBean(beanName));
				}
			}
		}
	}

	private static class MethodGetter<T extends Annotation> {
		public Map<Method, T> getMethods(final Class<?> type, final Class<T> annotationClass){
			return MethodIntrospector.selectMethods(type,
					new MethodIntrospector.MetadataLookup<T>() {
						@Override
						public T inspect(Method method) {
							return AnnotationUtils.findAnnotation(method, annotationClass);
						}
					});
		}
	}
}
