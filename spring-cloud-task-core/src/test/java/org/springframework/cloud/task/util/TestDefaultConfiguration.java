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

package org.springframework.cloud.task.util;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.listener.TaskLifecycleListener;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskNameResolver;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Initializes the beans needed to test default task behavior.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 */
@Configuration
@EnableConfigurationProperties(TaskProperties.class)
public class TestDefaultConfiguration implements InitializingBean {

	private TaskExecutionDaoFactoryBean factoryBean;

	@Autowired
	TaskProperties taskProperties;

	@Autowired(required = false)
	private ApplicationArguments applicationArguments;

	@Autowired
	private ConfigurableApplicationContext context;

	public TestDefaultConfiguration() {
	}

	@Bean
	public TaskRepository taskRepository(){
		return new SimpleTaskRepository(this.factoryBean);
	}

	@Bean
	public TaskExplorer taskExplorer() throws Exception {
		return new SimpleTaskExplorer(this.factoryBean);
	}

	@Bean
	public TaskNameResolver taskNameResolver() {
		return new SimpleTaskNameResolver();
	}

	@Bean
	public TaskLifecycleListener taskHandler(TaskExplorer taskExplorer){
		return new TaskLifecycleListener(taskRepository(), taskNameResolver(),
				applicationArguments, taskExplorer, taskProperties);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(this.context.getBeanNamesForType(DataSource.class).length == 1){
			DataSource dataSource = this.context.getBean(DataSource.class);
			this.factoryBean = new TaskExecutionDaoFactoryBean(dataSource);
		}
		else {
			this.factoryBean = new TaskExecutionDaoFactoryBean();
		}
	}
}
