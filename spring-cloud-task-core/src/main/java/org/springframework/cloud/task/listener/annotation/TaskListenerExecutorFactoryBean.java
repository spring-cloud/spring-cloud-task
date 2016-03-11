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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.aop.scope.ScopedObject;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.task.listener.TaskExecutionListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * @author Glenn Renfro
 */
public class TaskListenerExecutorFactoryBean implements FactoryBean<TaskExecutionListener> {

	private final static Log logger = LogFactory.getLog(TaskListenerExecutor.class);

	private final Set<Class<?>> nonAnnotatedClasses =
			Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>());

	private ConfigurableApplicationContext context;

	private Map<Method, Object> beforeTaskInstances;

	private Map<Method, Object> afterTaskInstances;

	private Map<Method, Object> failedTaskInstances;

	public TaskListenerExecutorFactoryBean(ConfigurableApplicationContext context){
		this.context = context;
	}

	@Override
	public TaskListenerExecutor getObject() throws Exception {
		beforeTaskInstances = new HashMap<>();
		afterTaskInstances = new HashMap<>();
		failedTaskInstances = new HashMap<>();
		initializeExecutor();
		return new TaskListenerExecutor(beforeTaskInstances, afterTaskInstances, failedTaskInstances);
	}

	@Override
	public Class<?> getObjectType() {
		return TaskListenerExecutor.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
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
