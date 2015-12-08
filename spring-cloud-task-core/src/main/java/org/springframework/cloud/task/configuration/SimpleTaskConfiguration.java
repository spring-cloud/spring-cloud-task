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

package org.springframework.cloud.task.configuration;

import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.TaskDatabaseInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Base {@code Configuration} class providing common structure for enabling and using
 * Spring Task. Customization is
 * available by implementing the {@link TaskConfigurer} interface.
 *
 * @author Glenn Renfro
 */
@EnableTransactionManagement
@Configuration
public class SimpleTaskConfiguration {

	protected static final Log logger = LogFactory.getLog(SimpleTaskConfiguration.class);


	@Autowired
	private ApplicationContext context;

	@Autowired(required = false)
	private Collection<DataSource> dataSources;

	@Autowired
	private ResourceLoader resourceLoader;

	@Value("${spring.class.initialize.enable:true}")
	private boolean taskInitializationEnable;

	private boolean initialized = false;

	private TaskRepository taskRepository;

	private TaskConfigurer configurer;

	@Bean
	@Scope("prototype")
	public TaskHandler taskHandler() {
		return new TaskHandler();
	}

	@Bean
	public TaskRepository taskRepository(){
		return taskRepository;
	}

	/**
	 * Sets up the basic components by extracting them from the {@link TaskConfigurer}, defaulting to some
	 * sensible values as long as a unique DataSource is available.
	 */
	@PostConstruct
	private void initialize()  {
		if (initialized) {
			return;
		}
		logger.debug("Getting Task Configurer");
		TaskConfigurer configurer = getConfigurer(context.getBeansOfType(TaskConfigurer.class).values());
		taskRepository = configurer.getTaskRepository();
		initialized = true;
	}

	private TaskConfigurer getConfigurer(Collection<TaskConfigurer> configurers) {
		if (this.configurer != null) {
			logger.debug(String.format("Using %s TaskConfigurer",
					configurer.getClass().getName()));
			return this.configurer;
		}
		if (configurers == null || configurers.isEmpty()) {
			if (dataSources == null || dataSources.isEmpty()) {
				this.configurer = new DefaultTaskConfigurer();
				logger.debug(String.format("Using %s TaskConfigurer, with no datasource",
						configurer.getClass().getName()));
				return this.configurer;
			}
			else if (dataSources != null && dataSources.size() == 1) {
				DataSource dataSource = dataSources.iterator().next();
				if(taskInitializationEnable) {
					logger.debug("Initializing Task Schema");
					TaskDatabaseInitializer.initializeDatabase(dataSource, resourceLoader);
				}
				this.configurer = new DefaultTaskConfigurer(dataSource);
				logger.debug(String.format("Using %s TaskConfigurer, with datasource",
						configurer.getClass().getName()));
				return this.configurer;
			}
			else {
				throw new IllegalStateException("To use the default TaskConfigurer the context must contain no more than" +
						"one DataSource, found " + dataSources.size());
			}
		}
		if (configurers.size() > 1) {
			throw new IllegalStateException(
					"To use a custom TaskConfigurer the context must contain precisely one, found "
							+ configurers.size());
		}
		this.configurer = configurers.iterator().next();
		logger.debug(String.format("More than one Task Configurer available.  Using"
				+ " first in list: %s TaskConfigurer",
				configurer.getClass().getName()));
		return this.configurer;
	}
}
