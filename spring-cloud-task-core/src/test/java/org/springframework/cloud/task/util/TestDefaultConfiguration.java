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

package org.springframework.cloud.task.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.cloud.task.listener.TaskLifecycleListener;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.dao.MapTaskExecutionDao;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskNameResolver;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Initializes the beans needed to test default task behavior.
 *
 * @author Glenn Renfro
 */
@Configuration
public class TestDefaultConfiguration {

	private MapTaskExecutionDao dao;

	@Autowired(required = false)
	private ApplicationArguments applicationArguments;

	public TestDefaultConfiguration() {
		this.dao = new MapTaskExecutionDao();
	}

	@Bean
	public TaskRepository taskRepository(){
		return new SimpleTaskRepository(this.dao);
	}

	@Bean
	public TaskNameResolver taskNameResolver() {
		return new SimpleTaskNameResolver();
	}

	@Bean
	public TaskLifecycleListener taskHandler(){
		return new TaskLifecycleListener(taskRepository(), taskNameResolver(), applicationArguments);
	}

	@Bean
	public TaskExplorer taskExplorer() {
		return new SimpleTaskExplorer(this.dao);
	}
}
