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

import javax.sql.DataSource;

import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.cloud.task.repository.support.TaskRepositoryInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Michael Minella
 */

@Configuration
public class TestConfiguration implements InitializingBean {

	@Autowired(required = false)
	private DataSource dataSource;

	@Autowired(required = false)
	private ResourceLoader resourceLoader;

	private TaskExecutionDaoFactoryBean taskExecutionDaoFactoryBean;

	@Bean
	public TaskRepositoryInitializer taskRepositoryInitializer() throws Exception {
		TaskRepositoryInitializer taskRepositoryInitializer = new TaskRepositoryInitializer();
		taskRepositoryInitializer.setDataSource(this.dataSource);
		taskRepositoryInitializer.setResourceLoader(this.resourceLoader);
		taskRepositoryInitializer.afterPropertiesSet();

		return taskRepositoryInitializer;
	}

	@Bean
	public TaskExplorer taskExplorer() throws Exception {
		return new SimpleTaskExplorer(this.taskExecutionDaoFactoryBean);
	}

	@Bean
	public TaskRepository taskRepository(){
		return new SimpleTaskRepository(this.taskExecutionDaoFactoryBean);
	}

	@Bean
	public PlatformTransactionManager transactionManager() {
		if(dataSource == null) {
			return new ResourcelessTransactionManager();
		}
		else {
			return new DataSourceTransactionManager(dataSource);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(this.dataSource != null) {
			this.taskExecutionDaoFactoryBean = new TaskExecutionDaoFactoryBean(this.dataSource);
		}
		else {
			this.taskExecutionDaoFactoryBean = new TaskExecutionDaoFactoryBean();
		}
	}
}
