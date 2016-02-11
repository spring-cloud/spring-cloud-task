/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.task.configuration;

import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.cloud.task.listener.TaskLifecycleListener;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskNameResolver;
import org.springframework.cloud.task.repository.support.TaskRepositoryInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Base {@code Configuration} class providing common structure for enabling and using
 * Spring Task. Customization is available by implementing the {@link TaskConfigurer}
 * interface.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 */
@Configuration
@EnableTransactionManagement
public class SimpleTaskConfiguration {

	protected static final Log logger = LogFactory.getLog(SimpleTaskConfiguration.class);

	@Autowired
	private ApplicationContext context;

	@Autowired(required = false)
	private Collection<DataSource> dataSources;

	@Autowired(required = false)
	private ApplicationArguments applicationArguments;

	private boolean initialized = false;

	private TaskRepository taskRepository;

	private TaskConfigurer configurer;

	private PlatformTransactionManager transactionManager;

	@Bean
	public TaskRepository taskRepository(){
		return taskRepository;
	}

	@Bean
	public TaskLifecycleListener taskLifecycleListener() {
		return new TaskLifecycleListener(taskRepository(), taskNameResolver(), this.applicationArguments);
	}

	@Bean
	public PlatformTransactionManager transactionManager() {
		return this.transactionManager;
	}

	@Bean
	public TaskNameResolver taskNameResolver() {
		return new SimpleTaskNameResolver();
	}

	@Bean
	public TaskRepositoryInitializer taskRepositoryInitializer() {
		TaskRepositoryInitializer taskRepositoryInitializer = new TaskRepositoryInitializer();

		if(this.dataSources != null && this.dataSources.size() == 1) {
			taskRepositoryInitializer.setDataSource(this.dataSources.iterator().next());
		}

		return taskRepositoryInitializer;
	}

	/**
	 * Sets up the basic components by extracting them from the {@link TaskConfigurer}, defaulting to some
	 * sensible values as long as a unique DataSource is available.
	 */
	@PostConstruct
	protected void initialize()  {
		if (initialized) {
			return;
		}
		logger.debug("Getting Task Configurer");
		if (configurer == null) {
			configurer = getDefaultConfigurer(context.getBeansOfType(TaskConfigurer.class).values());
		}
		logger.debug(String.format("Using %s TaskConfigurer",
				configurer.getClass().getName()));
		taskRepository = configurer.getTaskRepository();
		transactionManager = configurer.getTransactionManager();
		initialized = true;
	}

	private TaskConfigurer getDefaultConfigurer(Collection<TaskConfigurer> configurers) {
		boolean isDataSourceConfigured = (dataSources != null && dataSources.size() == 1);
		verifyEnvironment(configurers);
		if (configurers == null || configurers.isEmpty()) {
			if (!isDataSourceConfigured) {
				this.configurer = new DefaultTaskConfigurer();
				return this.configurer;
			}
			else {
				this.configurer = new DefaultTaskConfigurer(this.dataSources.iterator().next());
				return this.configurer;
			}
		}
		this.configurer = configurers.iterator().next();
		return this.configurer;
	}

	private void verifyEnvironment(Collection configurers){
		if (dataSources != null && dataSources.size() > 1) {
			throw new IllegalStateException("To use the default TaskConfigurer the context must contain no more than" +
					"one DataSource, found " + dataSources.size());
		}
		if (configurers.size() > 1) {
			throw new IllegalStateException(
					"To use a custom TaskConfigurer the context must contain precisely one, found "
							+ configurers.size());
		}
	}
}
