/*
 *  Copyright 2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.configuration;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.cloud.task.listener.TaskLifecycleListener;
import org.springframework.cloud.task.listener.TaskListenerExecutorObjectFactory;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for a {@link TaskLifecycleListener}.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 * @since 2.1
 */
@Configuration
public class TaskLifecycleConfiguration {

	protected static final Log logger = LogFactory.getLog(TaskLifecycleConfiguration.class);

	private TaskProperties taskProperties;

	private ConfigurableApplicationContext context;

	private ApplicationArguments applicationArguments;

	private TaskRepository taskRepository;

	private TaskExplorer taskExplorer;

	private TaskNameResolver taskNameResolver;

	private TaskLifecycleListener taskLifecycleListener;

	private boolean initialized = false;

	@Autowired
	public TaskLifecycleConfiguration(TaskProperties taskProperties,
			ConfigurableApplicationContext context,
			TaskRepository taskRepository,
			TaskExplorer taskExplorer,
			TaskNameResolver taskNameResolver,
			ObjectProvider<ApplicationArguments> applicationArguments) {

		this.taskProperties = taskProperties;
		this.context = context;
		this.taskRepository = taskRepository;
		this.taskExplorer = taskExplorer;
		this.taskNameResolver = taskNameResolver;
		this.applicationArguments = applicationArguments.getIfAvailable();
	}

	@Bean
	public TaskLifecycleListener taskLifecycleListener() {
		return this.taskLifecycleListener;
	}

	/**
	 * Initializes the {@link TaskLifecycleListener} for the task app.
	 */
	@PostConstruct
	protected void initialize() {
		if (!this.initialized) {
			this.taskLifecycleListener =
					new TaskLifecycleListener(this.taskRepository,
							this.taskNameResolver,
							this.applicationArguments,
							this.taskExplorer,
							this.taskProperties,
							new TaskListenerExecutorObjectFactory(context));

			this.initialized = true;
		}
	}
}
