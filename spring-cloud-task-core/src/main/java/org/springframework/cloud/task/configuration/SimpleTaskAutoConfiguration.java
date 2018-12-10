/*
 * Copyright 2015-2018 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskNameResolver;
import org.springframework.cloud.task.repository.support.TaskRepositoryInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.CollectionUtils;

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
@EnableConfigurationProperties(TaskProperties.class)
@ConditionalOnProperty(prefix = "spring.cloud.task.autoconfiguration", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SimpleTaskAutoConfiguration {

	protected static final Log logger = LogFactory.getLog(SimpleTaskAutoConfiguration.class);

	@Autowired(required = false)
	private Collection<DataSource> dataSources;

	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired(required = false)
	private ApplicationArguments applicationArguments;

	@Autowired
	private TaskProperties taskProperties;

	private boolean initialized = false;

	private TaskRepository taskRepository;

	private PlatformTransactionManager platformTransactionManager;

	private TaskExplorer taskExplorer;

	@Bean
	public TaskRepository taskRepository(){
		return this.taskRepository;
	}

	@Bean
	@ConditionalOnMissingBean
	public PlatformTransactionManager transactionManager() {
		return this.platformTransactionManager;
	}

	@Bean
	public TaskExplorer taskExplorer() {
		return this.taskExplorer;
	}

	@Bean
	public TaskNameResolver taskNameResolver() {
		return new SimpleTaskNameResolver();
	}

	@Bean
	public TaskRepositoryInitializer taskRepositoryInitializer() {
		TaskRepositoryInitializer taskRepositoryInitializer = new TaskRepositoryInitializer();
		DataSource initializerDataSource = getDefaultConfigurer().getTaskDataSource();
		if(initializerDataSource != null) {
			taskRepositoryInitializer.setDataSource(initializerDataSource);
		}

		return taskRepositoryInitializer;
	}

	/**
	 * Determines the {@link TaskConfigurer} to use.
	 */
	@PostConstruct
	protected void initialize() {
		if (initialized) {
			return;
		}

		TaskConfigurer taskConfigurer = getDefaultConfigurer();

		logger.debug(String.format("Using %s TaskConfigurer",
				taskConfigurer.getClass().getName()));

		this.taskRepository = taskConfigurer.getTaskRepository();
		this.platformTransactionManager = taskConfigurer.getTransactionManager();
		this.taskExplorer = taskConfigurer.getTaskExplorer();
		initialized = true;
	}

	private TaskConfigurer getDefaultConfigurer() {
		verifyEnvironment();

		int configurers = this.context.getBeanNamesForType(TaskConfigurer.class).length;

		if (configurers < 1) {
			TaskConfigurer taskConfigurer;
			if(!CollectionUtils.isEmpty(this.dataSources) && this.dataSources.size() == 1) {
				taskConfigurer = new DefaultTaskConfigurer(
						this.dataSources.iterator().next(),
						taskProperties.getTablePrefix(), context);
			}
			else {
				taskConfigurer = new DefaultTaskConfigurer(taskProperties.getTablePrefix());
			}
			this.context.getBeanFactory().registerSingleton("taskConfigurer", taskConfigurer);
			return taskConfigurer;
		}
		else {
			if(configurers == 1) {
				return this.context.getBean(TaskConfigurer.class);
			}
			else {
				throw new IllegalStateException("Expected one TaskConfigurer but found " + configurers);
			}
		}
	}

	private void verifyEnvironment() {
		int configurers = this.context.getBeanNamesForType(TaskConfigurer.class).length;
		// retrieve the count of dataSources (without instantiating them) excluding DataSource proxy beans
		long dataSources = Arrays.stream(this.context.getBeanNamesForType(DataSource.class))
				.filter((name -> !ScopedProxyUtils.isScopedTarget(name))).count();

		if(configurers == 0 && dataSources > 1) {
			throw new IllegalStateException("To use the default TaskConfigurer the context must contain no more than" +
					" one DataSource, found " + dataSources);
		}
	}
}
