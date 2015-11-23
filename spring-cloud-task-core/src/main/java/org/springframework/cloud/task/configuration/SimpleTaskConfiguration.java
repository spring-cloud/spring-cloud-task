/*
 *
 *  * Copyright 2015 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.cloud.task.configuration;

import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Base {@code Configuration} class providing common structure for enabling and using
 * Spring Task. Customization is
 * available by implementing the {@link TaskConfigurer} interface.
 *
 * @author Glenn Renfro
 */
@Configuration
public class SimpleTaskConfiguration {

	@Bean
	@Scope("prototype")
	public TaskHandler taskHandler() {
		return new TaskHandler();
	}

	@Autowired
	private ApplicationContext context;

	private boolean initialized = false;

	@Autowired(required = false)
	private Collection<DataSource> dataSources;

	private TaskRepository taskRepository;

	private TaskConfigurer configurer;

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
		TaskConfigurer configurer = getConfigurer(context.getBeansOfType(TaskConfigurer.class).values());
		taskRepository = configurer.getTaskRepository();
		initialized = true;
	}

	private TaskConfigurer getConfigurer(Collection<TaskConfigurer> configurers) {
		if (this.configurer != null) {
			return this.configurer;
		}
		if (configurers == null || configurers.isEmpty()) {
			if (dataSources == null || dataSources.isEmpty()) {
				DefaultTaskConfigurer configurer = new DefaultTaskConfigurer();
				this.configurer = configurer;
				return configurer;
			} else if(dataSources != null && dataSources.size() == 1) {
				DataSource dataSource = dataSources.iterator().next();
				DefaultTaskConfigurer configurer = new DefaultTaskConfigurer(dataSource);
				this.configurer = configurer;
				return configurer;
			} else {
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
		return this.configurer;
	}
}
